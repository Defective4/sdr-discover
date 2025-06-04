package io.github.defective4.sdr.sdrdscv.io.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class CSVBookmarkWriter implements BookmarkWriter {

    private final char separator;

    public CSVBookmarkWriter(char separator) {
        this.separator = separator;
    }

    @Override
    public void write(Writer output, List<RadioStation> stations) throws IOException {
        output.write("Name, Frequency, Modulation, Description\n".replace(',', separator));
        for (RadioStation station : stations) {
            output
                    .write(String
                            .format("%s, %s, %s, %s\n".replace(',', separator), station.getName(),
                                    (int) station.getFrequency(), station.getModulation(), station.getDescription()));
        }
    }

}
