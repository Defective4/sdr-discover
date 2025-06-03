package io.github.defective4.sdr.sdrdscv.io.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class OneLineBookmarkWriter implements BookmarkWriter {
    @Override
    public void write(Writer output, List<RadioStation> stations) throws IOException {
        for (RadioStation station : stations) output.write(station.getName() + " " + station.getFrequency() + "\n");
    }
}
