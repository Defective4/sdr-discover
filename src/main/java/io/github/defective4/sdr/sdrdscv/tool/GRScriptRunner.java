package io.github.defective4.sdr.sdrdscv.tool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import io.github.defective4.sdr.sdrdscv.io.FileUtils;

public class GRScriptRunner {
    private GRScriptRunner() {
    }

    public static Process run(String resource, Collection<String> dependencies, Object... args) throws IOException {
        File tmpDir = Files.createTempDirectory("sdr-discover-").toFile();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> FileUtils.recursiveDelete(tmpDir)));
        File scriptFile = new File(tmpDir, resource);
        try (InputStream in = GRScriptRunner.class.getResourceAsStream("/" + resource)) {
            Files.copy(in, scriptFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        for (String dep : dependencies) {
            try (InputStream in = GRScriptRunner.class.getResourceAsStream("/" + dep)) {
                File depFile = new File(tmpDir, dep);
                Files.copy(in, depFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        Object[] procArray = new Object[args.length + 2];
        System.arraycopy(args, 0, procArray, 2, args.length);
        procArray[0] = "python3";
        procArray[1] = scriptFile.getPath();

        String[] strArray = Arrays.stream(procArray).map(Object::toString).toList().toArray(new String[0]);

        Process proc = new ProcessBuilder(strArray).directory(tmpDir).start();
        Runtime.getRuntime().addShutdownHook(new Thread(proc::destroyForcibly));
        return proc;
    }

    public static Process run(String resource, Object... args) throws IOException {
        return run(resource, Collections.emptySet(), args);
    }
}
