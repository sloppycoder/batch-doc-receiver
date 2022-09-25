package org.vino9.eipp.promptbiz;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.vino9.eipp.data.Presentment;

// represent a single present (zip) file
// content:
//   1. Present.xml
//   2. Attached_Presentment.zip
//      which contains
//        file_1
//        ...
//        file_n
@Slf4j
public class SinglePresentmentFile {

    private String seq;
    private Path path;
    private String checksum;
    private Presentment parent;
    private FileSystem fs;
    private String workDir;

    private SinglePresentmentFile(int seq, Presentment parent, FileSystem fs) {
        this.seq = String.format("%017d", seq);
        this.parent = parent;
        this.fs = fs;
        this.workDir = this.seq + "_tmp";
    }

    static SinglePresentmentFile create(int seq, Presentment ps, FileSystem fs) {

        try {
            var newFile = new SinglePresentmentFile(seq, ps, fs);
            newFile.ensureWorkDir();

            var attachmentZip = newFile.createAttachmentZip();
            var presentmentZip = newFile.createPresentmentZip(attachmentZip);
            Files.deleteIfExists(attachmentZip);

            newFile.checksum = newFile.calculateChecksum(presentmentZip);
            return newFile;
        } catch (IOException e) {
            log.debug("Unable to create single presentment zip file for {}/{} due to {}",
                ps.getId(), seq, e);
            return null;
        }

    }

    private String calculateChecksum(Path presentmentZip) {
        return "ABCDEF";
    }

    private Path createAttachmentZip() {
        OutputStream outputStream = null;
        ZipOutputStream zipStream = null;
        try {
            // testing logic only
            // always add 1 attachment with 1M of random data
            var path = fs.getPath(this.workDir + "/Attached_Presentment.zip");
            outputStream = Files.newOutputStream(path, CREATE_NEW);
            zipStream = new ZipOutputStream(outputStream);
            zipStream.putNextEntry(new ZipEntry("random_stuff.bin"));
            zipStream.write(AttachmentReader.read());

            return path;
        } catch (IOException e) {
            log.info("unable to create Attached_Presentment.zip due to {}", e);
            return null;
        } finally {
            try {
                zipStream.close();
                outputStream.close();
            } catch (Exception e) {
            }
        }
    }

    private Path createPresentmentZip(Path attachmentZip) {
        XmlMapper mapper = new XmlMapper();
        OutputStream outputStream = null;
        ZipOutputStream zipStream = null;

        try {
            var zipPath = fs.getPath(this.workDir + "/" + this.seq + ".zip");
            // 1. create zip file
            outputStream = Files.newOutputStream(zipPath, CREATE_NEW);
            zipStream = new ZipOutputStream(outputStream);

            // 2. add Presentment.xml
            zipStream.putNextEntry(new ZipEntry("Presentment.xml"));
            var xml = mapper.writeValueAsString(this.parent);
            zipStream.write(xml.getBytes());

            // 3. add
            zipStream.putNextEntry(new ZipEntry("Attached_Presentment.zip"));
            Files.copy(attachmentZip, zipStream);

            this.path = zipPath;
            return zipPath;

        } catch (IOException e) {
            log.info("unable to create Presentment.zip due to {}", e);
            return null;
        } finally {
            // 4 close after done
            try {
                zipStream.close();
                outputStream.close();
            } catch (Exception e) {
            }
        }
    }

    // create the work dir. delete all files inside if the dir already exists
    private boolean ensureWorkDir() throws IOException {
        if (this.workDir == null || this.workDir.isEmpty()) {
            return false;
        }

        var path = fs.getPath(this.workDir);
        // recursively delete content if already exists
        if (Files.isDirectory(path)) {
            Files.list(path).forEach(f -> {
                try {
                    Files.deleteIfExists(f);
                } catch (IOException e) {
                }
            });
        } else if (Files.isRegularFile(path)) {
            Files.deleteIfExists(path);
            Files.createDirectory(path);
        } else {
            Files.createDirectory(path);
        }

        return true;
    }

    public String getSeq() {
        return seq;
    }

    public Path getPath() {
        return path;
    }

    public String getChecksum() {
        return checksum;
    }

    public Presentment getParent() {
        return parent;
    }
}