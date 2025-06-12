package io.github.defective4.sdr.sdrdscv.io.writer;

import java.io.Writer;
import java.util.List;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import io.github.defective4.sdr.sdrdscv.annotation.ConstructorParam;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class CSVBookmarkWriter implements BookmarkWriter {

    private final char separator;

    public CSVBookmarkWriter(
            @ConstructorParam(argName = "separator", defaultValue = ",", description = "A character used to separate values in the resulting CSV file.") char separator) {
        this.separator = separator;
    }

    @Override
    public void write(Writer output, List<RadioStation> stations) {
        ICSVWriter writer = new CSVWriterBuilder(output).withSeparator(separator).build();
        writer.writeNext(new String[] { "Name", "Frequency", "Modulation", "Description" }, false);
        for (RadioStation station : stations) {
            writer.writeNext(new String[] { station.getName(), Integer.toString((int) station.getFrequency()),
                    station.getModulation().name(),
                    station.getMetadataValue(RadioStation.METADATA_DESCRIPTION, String.class, "") }, false);
        }
    }

}
