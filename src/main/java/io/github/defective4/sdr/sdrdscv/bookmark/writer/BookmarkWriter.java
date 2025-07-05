package io.github.defective4.sdr.sdrdscv.bookmark.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public interface BookmarkWriter {
    void write(Writer output, List<RadioStation> stations) throws IOException;
}
