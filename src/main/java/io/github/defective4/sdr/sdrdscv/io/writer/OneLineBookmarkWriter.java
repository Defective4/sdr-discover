package io.github.defective4.sdr.sdrdscv.io.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import io.github.defective4.sdr.sdrdscv.annotation.ConstructorParam;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class OneLineBookmarkWriter implements BookmarkWriter {

    private final String format;

    public OneLineBookmarkWriter(
            @ConstructorParam(argName = "format", defaultValue = "%1s %2sHz %3s (%4s)", description = "Format of each line.\n"
                    + "%1s - Station name\n" + "%2s - Frequency\n" + "%3s - Modulation\n"
                    + "%4s - Description\n") String format) {
        this.format = format;
    }

    @Override
    public void write(Writer output, List<RadioStation> stations) throws IOException {
        for (RadioStation station : stations) output
                .write(String
                        .format(format + "\n", station.getName(), (long) station.getFrequency(),
                                station.getModulation(),
                                station.getMetadataValue(RadioStation.METADATA_DESCRIPTION, "")));
    }
}
