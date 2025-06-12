package io.github.defective4.sdr.sdrdscv.radio;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RadioStation {
    public static final String METADATA_BANDWIDTH = "bandwidth";
    public static final String METADATA_CUSTOM_MODULATION = "custom_modulation";
    public static final String METADATA_DESCRIPTION = "description";
    public static final String METADATA_SCANNABLE = "scannable";
    public static final String METADATA_SECONDARY_MODULATION = "secondary_modulation";
    public static final String METADATA_TAG_COLORS = "tag_colors";
    public static final String METADATA_TAGS = "tags";

    private final float frequency;
    private final Map<String, Object> metadata = new HashMap<>();
    private final Modulation modulation;
    private final String name;

    public RadioStation(String name, float frequency, Modulation modulation) {
        this(name, frequency, modulation, null);
    }

    public RadioStation(String name, float frequency, Modulation modulation, Map<String, Object> metadata) {
        this.name = name;
        this.frequency = frequency;
        this.modulation = modulation;
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
    }

    public float getFrequency() {
        return frequency;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public Object getMetadataValue(String key) {
        return metadata.get(key);
    }

    public <T> T getMetadataValue(String key, Class<?> type) {
        Object val = metadata.get(key);
        if (val == null) return null;
        if (!type.isInstance(val)) throw new ClassCastException(val + " is not an instance of " + type);
        return (T) val;
    }

    public <T> T getMetadataValue(String key, Class<?> type, T def) {
        Object val = metadata.getOrDefault(key, def);
        if (val == null) return null;
        if (!type.isInstance(val)) throw new ClassCastException(val + " is not an instance of " + type);
        return (T) val;
    }

    public Object getMetadataValue(String key, Object def) {
        return metadata.getOrDefault(key, def);
    }

    public Modulation getModulation() {
        return modulation;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "RadioStation [frequency=" + frequency + ", metadata=" + metadata + ", modulation=" + modulation
                + ", name=" + name + "]";
    }

}
