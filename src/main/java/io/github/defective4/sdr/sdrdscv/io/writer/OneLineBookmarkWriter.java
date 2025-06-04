package io.github.defective4.sdr.sdrdscv.io.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class OneLineBookmarkWriter implements BookmarkWriter {

    private final String format;

    public OneLineBookmarkWriter(
            @WriterParam(argName = "format", defaultValue = "%s1 %s2Hz %s3 (%s4)", description = "Format of each line.\n"
                    + "$s1 - Station name\n" + "$s2 - Frequuuency\n" + "%s3 - Modulation\n"
                    + "%s4 - Description\n") String format) {
        this.format = format;
    }

    @Override
    public void write(Writer output, List<RadioStation> stations) throws IOException {
        for (RadioStation station : stations) output
                .write(String
                        .format(format + "\n", station.getName(), (long) station.getFrequency(),
                                station.getModulation(), station.getDescription()));
    }
}
