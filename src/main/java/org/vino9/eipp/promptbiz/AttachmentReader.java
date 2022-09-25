package org.vino9.eipp.promptbiz;

import java.util.Random;

// dummy implementation
public class AttachmentReader {

    public AttachmentReader(String path) {
    }

    public static byte[] read() {
        var bytes = new byte[1024*1024];
        (new Random()).nextBytes(bytes);
        return bytes;
    }
}
