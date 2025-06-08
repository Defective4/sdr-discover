package io.github.defective4.sdr.sdrdscv.radio;

public enum Modulation {
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

    public static Modulation fromGqrxMod(String modulationStr) {
        for (Modulation mod : values()) if (mod.gqrxMod.equals(modulationStr)) return mod;
        throw new IllegalArgumentException(modulationStr + " is not a valid gqrx modulation");
    }

    public static Modulation fromSdrppMod(int code) {
        for (Modulation mod : values()) if (mod.sdrppMod == code) return mod;
        throw new IllegalArgumentException("Invalid SDR++ modulation code: " + code);
    }

}