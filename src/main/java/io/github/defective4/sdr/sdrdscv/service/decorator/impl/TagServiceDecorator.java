package io.github.defective4.sdr.sdrdscv.service.decorator.impl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.defective4.sdr.sdrdscv.ParamConverters;
import io.github.defective4.sdr.sdrdscv.annotation.BuilderParam;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecorator;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecoratorBuilder;

public class TagServiceDecorator implements ServiceDecorator {

    public static class Builder implements ServiceDecoratorBuilder<TagServiceDecorator> {

        private String tagName = "Discovered";
        private Color tagColor = Color.white;
        private boolean append = true;

        @BuilderParam(argName = "no-append", description = "Disables appending of tags when a station is already tagged. The tags are replaced instead.")
        public Builder noAppend() {
            this.append = false;
            return this;
        }

        @BuilderParam(argName = "name", defaultField = "tagName", description = "This tag name will be added to all decorated stations")
        public Builder withTagName(String tagName) {
            this.tagName = tagName;
            return this;
        }

        @BuilderParam(argName = "color", description = "Color of this tag. (Default: #ffffff)")
        public Builder withTagColor(Color tagColor) {
            this.tagColor = tagColor;
            return this;
        }

        @Override
        public TagServiceDecorator build() {
            return new TagServiceDecorator(tagName, tagColor, append);
        }

    }

    private final String tagName;
    private final Color tagColor;
    private final boolean append;

    private TagServiceDecorator(String tagName, Color tagColor, boolean append) {
        this.tagName = tagName;
        this.tagColor = tagColor;
        this.append = append;
    }

    @Override
    public List<RadioStation> decorate(List<RadioStation> stations) {
        List<RadioStation> decorated = new ArrayList<>();
        for (RadioStation station : stations) {
            Map<String, Object> metadata = new HashMap<>(station.getMetadata());
            List<String> tags = new ArrayList<>();
            List<String> colors = new ArrayList<>();
            if (append) {
                String oldTags = station.getMetadataValue(RadioStation.METADATA_TAGS, String.class);
                String oldColors = station.getMetadataValue(RadioStation.METADATA_TAG_COLORS, String.class);
                if (oldTags != null) Collections.addAll(tags, oldTags.split(","));
                if (oldColors != null) Collections.addAll(colors, oldColors.split(","));
            }
            tags.add(tagName);
            colors.add(ParamConverters.encodeColor(tagColor));
            metadata.put(RadioStation.METADATA_TAGS, String.join(",", tags.toArray(new String[0])));
            metadata.put(RadioStation.METADATA_TAG_COLORS, String.join(",", colors.toArray(new String[0])));
            decorated
                    .add(new RadioStation(station.getName(), station.getFrequency(), station.getModulation(),
                            metadata));
        }
        return Collections.unmodifiableList(decorated);
    }

}
