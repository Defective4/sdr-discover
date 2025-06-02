package io.github.defective4.sdr.sdrdscv.tool;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class GRScriptRunner {
    private GRScriptRunner() {}

    public static Process run(String resource, Object... args) throws IOException {
        File tmpFile;
        try (InputStream in = GRScriptRunner.class.getResourceAsStream(resource)) {
            tmpFile = File.createTempFile("sdr-discover-", ".py");
            Files.copy(in, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        Object[] procArray = new Object[args.length + 2];
        System.arraycopy(args, 0, procArray, 2, args.length);
        procArray[0] = "python3";
        procArray[1] = tmpFile.getPath();

        String[] strArray = Arrays.stream(procArray).map(Object::toString).toList().toArray(new String[0]);

        Process proc = new ProcessBuilder(strArray).start();
        Runtime.getRuntime().addShutdownHook(new Thread(proc::destroyForcibly));
        return proc;
    }
}
