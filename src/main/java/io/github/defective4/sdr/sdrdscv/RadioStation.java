package io.github.defective4.sdr.sdrdscv;

public class RadioStation {
    public static enum Modulation {
        WFM, WFM_STEREO
    }

    private final float frequency;
    private final Modulation modulation;
    private final String name, description;

    public RadioStation(String name, String description, float frequency, Modulation modulation) {
        this.name = name;
        this.description = description;
        this.frequency = frequency;
        this.modulation = modulation;
    }

    public String getDescription() {
        return description;
    }

    public float getFrequency() {
        return frequency;
    }

    public Modulation getModulation() {
        return modulation;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "RadioStation [name=" + name + ", description=" + description + ", frequency=" + frequency
                + ", modulation=" + modulation + "]";
    }

}
