package io.github.defective4.sdr.sdrdscv.bandplan.reader;

import java.io.IOException;
import java.io.Reader;

import io.github.defective4.sdr.sdrdscv.bandplan.Bandplan;

public interface BandplanReader {
    Bandplan read(Reader reader, boolean verbose) throws IOException;
}
