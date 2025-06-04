package io.github.defective4.sdr.sdrdscv.io.writer;

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class GqrxBookmarkWriter implements BookmarkWriter {

    private final Color tagColor;
    private final String tagName;

    public GqrxBookmarkWriter(String tagName, Color tagColor) {
        this.tagName = tagName;
        this.tagColor = tagColor;
    }

    @Override
    public void write(Writer output, List<RadioStation> stations) throws IOException {
        output.write("# Tag name; color\n");
        String color = Integer.toHexString(tagColor.getRGB() & 0xffffff);
        while (color.length() < 6) color = "0" + color;
        output.write(tagName + "; #" + color + "\n\n");
        output.write("# Frequency; Name; Modulation; Bandwidth; Tags\n");
        for (RadioStation station : stations) {
            output
                    .write(String
                            .format("    %s; %s; %s; %s; %s\n", (long) station.getFrequency(), station.getName(),
                                    station.getModulation().getGqrxMod(), (int) station.getModulation().getBandwidth(),
                                    tagName));
        }
    }
}
