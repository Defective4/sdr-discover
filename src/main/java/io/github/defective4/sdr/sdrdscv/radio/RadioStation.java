package io.github.defective4.sdr.sdrdscv.radio;

public class RadioStation {
    public static enum Modulation {
        WFM(160e3f, "WFM (mono)", 1, "wfm"), WFM_STEREO(160e3f, "WFM (stereo)", 1, "wfm");

        private final float bandwidth;
        private final String gqrxMod;
        private final String owrxMod;
        private final int sdrppMod;

        private Modulation(float bandwidth, String gqrxMod, int sdrppMod, String owrxMod) {
            this.bandwidth = bandwidth;
            this.gqrxMod = gqrxMod;
            this.sdrppMod = sdrppMod;
            this.owrxMod = owrxMod;
        }

        public float getBandwidth() {
            return bandwidth;
        }

        public String getGqrxMod() {
            return gqrxMod;
        }

        public String getOwrxMod() {
            return owrxMod;
        }

        public int getSdrppMod() {
            return sdrppMod;
        }

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
