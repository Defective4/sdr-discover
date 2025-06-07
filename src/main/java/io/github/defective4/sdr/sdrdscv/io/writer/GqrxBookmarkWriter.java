package io.github.defective4.sdr.sdrdscv.io.writer;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.defective4.sdr.sdrdscv.ParamConverters;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class GqrxBookmarkWriter implements BookmarkWriter {

    private final Color tagColor;
    private final String tagName;

    public GqrxBookmarkWriter(
            @WriterParam(argName = "tag-name", defaultValue = "Discovered", description = "Assigns a tag name to all detected stations.") String tagName,
            @WriterParam(argName = "tag-color", defaultValue = "#ffffff", description = "Defines the color used for all detected stations.") Color tagColor) {
        this.tagName = tagName;
        this.tagColor = tagColor;
    }

    @Override
    public void write(Writer output, List<RadioStation> stations) throws IOException {
        Map<String, Color> tags = new HashMap<>();
        tags.put(tagName, tagColor);
        for (RadioStation station : stations) {
            String metaTags = station.getMetadataValue(RadioStation.METADATA_GQRX_TAGS, String.class);
            if (metaTags != null) {
                String metaColors = station.getMetadataValue(RadioStation.METADATA_GQRX_TAG_COLORS, String.class);
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
        output.write("# Tag name; color\n");
        for (Entry<String, Color> entry : tags.entrySet()) {
            String color = ParamConverters.encodeColor(entry.getValue());
            output.write(entry.getKey() + "; " + color + "\n");
        }
        output.write("\n");
        output.write("# Frequency; Name; Modulation; Bandwidth; Tags\n");
        for (RadioStation station : stations) {
            String metaTags = station.getMetadataValue(RadioStation.METADATA_GQRX_TAGS, String.class);
            String[] tagsArray;
            tagsArray = metaTags == null ? new String[] {
                    tagName
            } : metaTags.split(",");
            output
                    .write(String
                            .format("    %s; %s; %s; %s; %s\n", (long) station.getFrequency(), station.getName(),
                                    station.getModulation().getGqrxMod(),
                                    station
                                            .getMetadataValue(RadioStation.METADATA_BANDWIDTH, Integer.class,
                                                    (int) station.getModulation().getBandwidth()),
                                    String.join(",", tagsArray)));
        }
    }
}
