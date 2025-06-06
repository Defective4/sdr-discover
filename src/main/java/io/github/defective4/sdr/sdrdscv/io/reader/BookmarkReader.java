package io.github.defective4.sdr.sdrdscv.io.reader;

import java.io.Reader;
import java.util.List;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public abstract class BookmarkReader {

    protected final boolean lenient, verbose;

    protected BookmarkReader(boolean lenient, boolean verbose) {
        this.lenient = lenient;
        this.verbose = verbose;
    }

    public boolean isLenient() {
        return lenient;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public abstract List<RadioStation> read(Reader reader) throws Exception;

}
