package org.vino9.eipp.promptbiz;
/*
 package presentments into batch of N
 */

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
        var individuals = payload.stream()
            .map(ps -> SinglePresentmentFile.create(counter.getAndIncrement(), ps, imfs))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        var outputFile = (String) exchange.getIn().getHeader("PSE_OUTPUT_FILE_NAME");
        if (outputFile == null || outputFile.isEmpty()) {
            outputFile = "PSE_KTB_TEST_REQ";
        }

        pack(individuals, outputFile);
    }

    private void pack(List<SinglePresentmentFile> individuals, String outputName) {
        ArrayList<Presentment> processedEntries = new ArrayList<>();
        long maxOutputSize = maxOutputSizeM * 1024 * 1024;

        var controlFilePath = imfs.getPath(outputName + ".csv");
        var zipPath = imfs.getPath(outputName + ".zip");
        try {

            Files.deleteIfExists(controlFilePath);
            Files.deleteIfExists(zipPath);

            var controlFile = Files.newOutputStream(controlFilePath, CREATE_NEW);
            var zip = new ZipOutputStream(Files.newOutputStream(zipPath, CREATE_NEW));

            AtomicLong totalSize = new AtomicLong();

            individuals.forEach(entry -> {
                try {
                    // check the size of zip file size for each entry
                    // skip if exceed max-output-size
                    var entryZipPath = entry.getPath();
                    var entryZipSize = Files.size(entryZipPath);
                    if (totalSize.get() + entryZipSize > maxOutputSize - MAX_OUTPUT_SIZE_MARGIN) {
                        log.info("skipping {} of size {}. will exceed max output size of {}",
                            entry.getSeq(), entryZipSize, maxOutputSize);
                        return;
                    }

                    // copy zip file of each entry into the final output zip
                    zip.putNextEntry(new ZipEntry(entry.getSeq() + ".zip"));
                    Files.copy(entry.getPath(), zip);

                    // add control file entry too
                    var controlLine = String.format("operationalFlag,fileNumber,%s,sellerTraderID",
                        entry.getChecksum());
                    controlFile.write(controlLine.getBytes());
                    controlFile.write('\n');

                    // done with one entry, cleanup and keep track of the total output size
                    totalSize.addAndGet(entryZipSize);

                    // mark database record was PROCESSED
                    processedEntries.add(entry.getParent());
                } catch (IOException e) {
                    log.error("Unable to add {} to {} due to {}", entry.getSeq(), outputName, e);
                }
            });

            // add control file to final zip
            controlFile.close();
            zip.putNextEntry(new ZipEntry(outputName + ".csv"));
            Files.copy(controlFilePath, zip);

            zip.close();

            // copy final output from in-memory FS to external path
            if (processedEntries.size() > 0) {
                var finalOutputPath = Paths.get(outputPath + "/" + outputName + ".zip");
                Files.deleteIfExists(finalOutputPath);
                var finalOutput = Files.newOutputStream(finalOutputPath, CREATE_NEW,
                    TRUNCATE_EXISTING);
                Files.copy(zipPath, finalOutput);
                log.info("written output file {}", finalOutputPath);
            }

            // we update the status of Presentment records AFTER we completed writing the output file
            // because we think the changes of failure is higher during file IO due to:
            //  1. file IO takes longer than DB operations
            //  2. external file system tends to be more volatile than DB
            processedEntries.forEach(ps -> this.markProcessed(ps));

        } catch (IOException e) {
            log.error("Unable to write to {} due to {}", outputName, e);
        } finally {
            // TODO: delete all entries files
            try {
                Files.deleteIfExists(controlFilePath);
                Files.deleteIfExists(zipPath);
            } catch (IOException e) {
            }
        }
    }

    private Optional<Long> markProcessed(Presentment ps) {
        var id = ps.getId();

        ps.setStatus(Status.PROCESSED);
        ps.setAttempts(ps.getAttempts() + 1);
        repo.save(ps);
        log.debug("processed presentment {}", id);

        return Optional.of(id);
    }
}
