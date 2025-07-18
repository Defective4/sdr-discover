package io.github.defective4.sdr.sdrdscv.bandplan;

import io.github.defective4.sdr.sdrdscv.radio.Modulation;

public class Band {
    private Modulation modulation;
    private final String name;
    private final float startFreq, endFreq;

    public Band(String name, float startFreq, float endFreq, Modulation modulation) {
        this.name = name;
        this.startFreq = startFreq;
        this.endFreq = endFreq;
        this.modulation = modulation;
    }

    public float getBandwidth() {
        return endFreq - startFreq;
    }

    public float getEndFreq() {
        return endFreq;
    }

    public Modulation getModulation() {
        return modulation;
    }

    public String getName() {
        return name;
    }

    public float getStartFreq() {
        return startFreq;
    }

    public void setModulation(Modulation modulation) {
        this.modulation = modulation;
    }

    @Override
    public String toString() {
        return "Band [name=" + name + ", startFreq=" + startFreq + ", endFreq=" + endFreq + ", modulation=" + modulation
                + "]";
    }

}
