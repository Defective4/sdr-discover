package io.github.defective4.sdr.sdrdscv.io.reader;

import java.awt.Color;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import io.github.defective4.sdr.sdrdscv.ParamConverters;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation.Modulation;

public class GqrxBookmarkReader extends BookmarkReader {

    private final CSVParser parser;

    public GqrxBookmarkReader(boolean lenient, boolean verbose, char sep) {
        super(lenient, verbose);
        parser = new CSVParserBuilder().withSeparator(sep).withIgnoreLeadingWhiteSpace(true).build();
    }

    @Override
    public List<RadioStation> read(Reader reader) throws Exception {
        List<RadioStation> stations = new ArrayList<>();
        CSVReader csv = new CSVReaderBuilder(reader).withCSVParser(parser).build();

        Map<String, Color> tags = new LinkedHashMap<>();

        int tagsFailed = 0;
        int stationsFailed = 0;

        while (true) {
            String[] next = csv.readNext();
            if (next == null) break;
            if (next.length == 2) {
                String tagName = next[0].trim();
                if (tagName.startsWith("#")) continue;
                Color tagColor;
                try {
                    tagColor = Color.decode(next[1].trim());
                } catch (Exception e) {
                    if (lenient) {
                        if (verbose) System.err
                                .println("Tag " + tagName + " couldn't be parsed, because its color code is malformed");
                        tagsFailed++;
                        continue;
                    }
                    throw e;
                }
                tags.put(tagName, tagColor);
            } else if (next.length == 5) {
                String freqName = next[0].trim();
                if (freqName.startsWith("#")) continue;
                String name = next[1].trim();
                String modulationStr = next[2].trim();
                String bandwidthStr = next[3].trim();
                String[] tagNames = next[4].trim().split(",");
                for (int i = 0; i < tagNames.length; i++) tagNames[i] = tagNames[i].trim();

                float freq;
                Modulation mod;
                int bandwidth;

                try {
                    freq = Float.parseFloat(freqName);
                    mod = Modulation.fromGqrxMod(modulationStr);
                    bandwidth = Integer.parseInt(bandwidthStr);
                } catch (Exception e) {
                    if (lenient) {
                        if (verbose) System.err.println("Bookmark " + name + " couldn't be parsed: " + e.getMessage());
                        stationsFailed++;
                        continue;
                    }
                    throw e;
                }

                Map<String, Object> metadata = new HashMap<>();
                if (tagNames.length > 0) {
                    String[] tagColors = new String[tagNames.length];
                    for (int i = 0; i < tagNames.length; i++) {
                        tagColors[i] = ParamConverters.encodeColor(tags.getOrDefault(tagNames[i], Color.white));
                    }
                    metadata.put(RadioStation.METADATA_GQRX_TAGS, String.join(",", tagNames));
                    metadata.put(RadioStation.METADATA_GQRX_TAG_COLORS, String.join(",", tagColors));
                }
                metadata.put(RadioStation.METADATA_BANDWIDTH, bandwidth);

                stations.add(new RadioStation(name, freq, mod, metadata));
            }
        }

        if (verbose) {
            System.err.println("Successfully parsed " + stations.size() + " gqrx bookmarks");
            if (tagsFailed > 0) System.err.println("Failed to parse " + tagsFailed + " tags");
            if (stationsFailed > 0) System.err.println("Failed to parse " + stationsFailed + " stations");
        }

        return Collections.unmodifiableList(stations);
    }

}
