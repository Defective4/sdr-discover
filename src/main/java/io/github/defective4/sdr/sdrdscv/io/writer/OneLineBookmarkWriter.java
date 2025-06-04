package io.github.defective4.sdr.sdrdscv.io.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class OneLineBookmarkWriter implements BookmarkWriter {

    private final String format;

    public OneLineBookmarkWriter(String format) {
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
