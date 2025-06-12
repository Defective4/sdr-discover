package io.github.defective4.sdr.sdrdscv.io;

import java.io.File;

public class FileUtils {
    public static void recursiveDelete(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                recursiveDelete(child);
            }
        }
        file.delete();
    }
}
