package io.github.defective4.sdr.sdrdscv.io.reader;

import java.io.Reader;
import java.util.Collections;
import java.util.List;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class DummyBookmarkReader extends BookmarkReader {

    public DummyBookmarkReader() {
        super(false, false);
    }

    @Override
    public List<RadioStation> read(Reader reader) throws Exception {
        return Collections.emptyList();
    }

}
