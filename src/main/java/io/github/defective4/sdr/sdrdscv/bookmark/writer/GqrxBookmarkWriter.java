package io.github.defective4.sdr.sdrdscv.bookmark.writer;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import io.github.defective4.sdr.sdrdscv.ParamConverters;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class GqrxBookmarkWriter implements BookmarkWriter {

    public GqrxBookmarkWriter() {
    }

    @Override
    public void write(Writer output, List<RadioStation> stations) throws IOException {
        ICSVWriter csv = new CSVWriterBuilder(output).withSeparator(';').build();
        Map<String, Color> tags = new HashMap<>();
        for (RadioStation station : stations) {
            String metaTags = station.getMetadataValue(RadioStation.METADATA_TAGS, String.class);
            if (metaTags != null) {
                String metaColors = station.getMetadataValue(RadioStation.METADATA_TAG_COLORS, String.class);
                List<Color> colors = new ArrayList<>();
                for (String colorCode : metaColors == null ? new String[0] : metaColors.split(",")) {
                    Color color;
                    try {
                        color = Color.decode(colorCode);
                    } catch (Exception e) {
                        color = Color.white;
                    }
                    colors.add(color);
                }

                String[] split = metaTags.split(",");
                for (int i = 0; i < split.length; i++) {
                    String tagName = split[i];
                    Color color = i >= colors.size() ? Color.white : colors.get(i);
                    tags.put(tagName, color);
                }
            }
        }
        csv.writeNext(new String[] { "# Tag name", "color" }, false);
        for (Entry<String, Color> entry : tags.entrySet()) {
            String color = ParamConverters.encodeColor(entry.getValue());
            csv.writeNext(new String[] { entry.getKey(), color }, false);
        }

        csv.flush();
        output.write("\n");
        csv.writeNext(new String[] { "# Frequency", "Name", "Modulation", "Bandwidth", "Tags" }, false);
        for (RadioStation station : stations) {
            String stTagsStr = station.getMetadataValue(RadioStation.METADATA_TAGS, String.class);
            String[] stTags = stTagsStr == null ? new String[0] : stTagsStr.split(",");
            csv.writeNext(new String[] { "    " + Long.toString((long) station.getFrequency()), station.getName(),
                    station.getModulation().getGqrxMod(),
                    Integer.toString(station.getMetadataValue(RadioStation.METADATA_BANDWIDTH, Integer.class,
                            (int) station.getModulation().getBandwidth())),
                    String.join(",", stTags) }, false);
        }
        csv.flush();
    }
}
