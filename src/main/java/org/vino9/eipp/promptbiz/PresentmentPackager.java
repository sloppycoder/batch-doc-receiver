package org.vino9.eipp.promptbiz;

/*
 package presentments into batch of output-batch-size
 */

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.vino9.eipp.data.Presentment;
import org.vino9.eipp.data.Presentment.Status;
import org.vino9.eipp.data.PresentmentRepository;

@Component
@Slf4j
public class PresentmentPackager {

    private static final long MAX_OUTPUT_SIZE_MARGIN = 1024 * 2048L; // 2M bytes

    @Autowired
    private PresentmentRepository repo;

    @Autowired
    @Qualifier("inMemoryFileSystem")
    private FileSystem imfs;


    @Value("${presentment-sender.output-batch-size:3}")
    int batchSize;

    @Value("${presentment-sender.max-output-size:100000000}")
    long maxOutputSizeM;

    @Value("${presentment-sender.output-path}")
    String outputPath;


    HashMap<Long, Integer> registry = new HashMap<>();

    // package the input payload of list of Presenetments into
    // a zip file according to specification 1.1
    // in memory file system is used for better performance and reduce IO related
    // failures
    // TODO: add reference of the file format here
    public void process(Exchange exchange) {
        var payload = (List<Presentment>) exchange.getIn().getBody();
        log.debug("packer received {} of presentments", payload.size());

        // loop through each presentment and create individual zip files
        // SinglePresentmentFile.create returns null if some error occurs
        // during file creation. the entry will be ignored here, the database
        // record will remain in IN_PROGRESS status. They should be handled
        // by re-try logic later.
        AtomicInteger counter = new AtomicInteger(1);
        var entries = payload.stream()
            .map(ps -> SinglePresentmentFile.create(counter.getAndIncrement(), ps, imfs))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // we update the status of Presentment records AFTER we completed writing the output file
        // because we think the changes of failure is higher during file IO due to:
        //  1. file IO takes longer than DB operations
        //  2. external file system tends to be more volatile than DB
        var outputFileName = (String) exchange.getIn().getHeader("PSE_OUTPUT_FILE_NAME");
        pack(entries, outputFileName)
            .forEach(this::markProcessed);

    }

    // pack individual presentment zip files into the final output zip format
    // and return a list of SinglePresentFile that were successfully written to
    // output file
    private List<Presentment> pack(List<SinglePresentmentFile> entries, String outputFileName) {
        ArrayList<Presentment> processedEntries = new ArrayList<>();

        var controlFilePath = imfs.getPath("control.csv");
        var zipPath = imfs.getPath("final.zip");
        try {

            Files.deleteIfExists(controlFilePath);
            Files.deleteIfExists(zipPath);

            var controlFile = Files.newOutputStream(controlFilePath, CREATE_NEW);
            var zip = new ZipOutputStream(Files.newOutputStream(zipPath, CREATE_NEW));

            AtomicLong totalSize = new AtomicLong();

            for (var entry : entries) {
                var bytesAdded = addEntryToOutput(entry, totalSize.get(), zip);
                if (bytesAdded > 0) {
                    totalSize.addAndGet(bytesAdded);
                    writeControlFile(entry, controlFile);
                    // keep track of added entries
                    // do not update the database just yet
                    // we will update after the output file
                    // is successfully written
                    processedEntries.add(entry.getParent());
                }
            }

            // add control file to final zip
            controlFile.close();
            zip.putNextEntry(new ZipEntry(outputFileName + ".csv"));
            Files.copy(controlFilePath, zip);

            zip.close();

            // copy final output from in-memory FS to external path
            if (processedEntries.size() > 0) {
                writeFinalOutput(zipPath, outputFileName);
            }

            return processedEntries;
        } catch (IOException e) {
            log.error("Unable to write to {} due to {}", outputFileName, e);
        } finally {
            // TODO: delete all entries files
            try {
                Files.deleteIfExists(controlFilePath);
                Files.deleteIfExists(zipPath);
            } catch (IOException e) {
            }
        }

        return null;
    }

    // add one presentment zip file to output file
    // returns the size of the file added.
    // skip adding the entry will exceed max-output-size, returns 0
    private long addEntryToOutput(SinglePresentmentFile entry, long currentSize, ZipOutputStream output) {
        long maxOutputSize = maxOutputSizeM * 1024 * 1024;

        // check the size of zip file size for each entry
        // skip if exceed max-output-size
        try {
            var entryZipPath = entry.getPath();
            var entryZipSize = Files.size(entryZipPath);
            if (currentSize + entryZipSize > maxOutputSize - MAX_OUTPUT_SIZE_MARGIN) {
                log.info("skipping {} of size {}. will exceed max output size of {}",
                    entry.getSeq(), entryZipSize, maxOutputSize);
                return 0L;
            }

            // copy zip file of each entry into the final output zip
            output.putNextEntry(new ZipEntry(entry.getSeq() + ".zip"));
            Files.copy(entry.getPath(), output);
            return entryZipSize;
        } catch (IOException e) {
            log.info("Unable to add entry {} due to {}", entry.getSeq(), e);
            return 0L;
        }
    }

    // write an entry to control file after adding the individual zip to output
    private void writeControlFile(SinglePresentmentFile entry, OutputStream controlFile)
        throws IOException {
        var controlLine = String.format(
            "operationalFlag,fileNumber,%s,sellerTraderID",
            entry.getChecksum());
        controlFile.write(controlLine.getBytes());
        controlFile.write('\n');
    }

    // copy the final output zip file from in memory FS to actual file system
    private void writeFinalOutput(Path zipPath, String outputFileName) throws IOException {
        var finalOutputPath = Paths.get(outputPath + "/" + outputFileName + ".zip");
        Files.deleteIfExists(finalOutputPath);
        var finalOutput = Files.newOutputStream(finalOutputPath, CREATE_NEW,
            TRUNCATE_EXISTING);
        Files.copy(zipPath, finalOutput);
        log.info("written output file {}", finalOutputPath);
    }


    // mark the presentment DB entry as processed
    private void markProcessed(Presentment ps) {
        var id = ps.getId();

        ps.setStatus(Status.PROCESSED);
        ps.setAttempts(ps.getAttempts() + 1);
        repo.save(ps);
        log.debug("processed presentment {}", id);
    }
}
