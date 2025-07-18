package io.github.defective4.sdr.sdrdscv.bandplan.reader;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import io.github.defective4.sdr.sdrdscv.bandplan.Band;
import io.github.defective4.sdr.sdrdscv.bandplan.Bandplan;
import io.github.defective4.sdr.sdrdscv.radio.Modulation;

public class GqrxBandplanReader implements BandplanReader {

    @Override
    public Bandplan read(Reader reader, boolean verbose) throws IOException {
        List<Band> bands = new ArrayList<>();
        CSVParser parser = new CSVParserBuilder().withSeparator(',').build();
        try (CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(parser).build()) {
            while (true) {
                String[] row = csvReader.readNextSilently();
                if (row == null) break;
                if (row.length == 6 && !row[0].startsWith("#")) {
                    try {
                        float startFreq = Float.parseFloat(row[0].trim());
                        float endFreq = Float.parseFloat(row[1].trim());
                        Modulation modulation;
                        String name = row[5].trim();
                        try {
                            modulation = Modulation.valueOf(row[2].trim().toUpperCase());
                        } catch (Exception e) {
                            if (verbose) System.err.println(
                                    "Unknown modulation type " + row[2].trim() + " on \"" + name + "\". Skipped.");
                            continue;
                        }

                        bands.add(new Band(name, startFreq, endFreq, modulation));
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                }
            }
        }
        return new Bandplan(bands);
    }
}
