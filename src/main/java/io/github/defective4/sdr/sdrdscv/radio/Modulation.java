package io.github.defective4.sdr.sdrdscv.radio;

import java.util.Arrays;

public enum Modulation {
    AM(10e3f, "AM", 2, "am"), AM_SYNC(10e3f, "AM-Sync", 2, "sam", 0), CUSTOM(48000, "Raw I/Q", 7, "usbd"),
    CW(200, "CW-L", 5, "cw"), DSB(4600, "USB", 3, "usb", 0), LSB(5000, "LSB", 6, "lsb"),
    NFM(10e3f, "Narrow FM", 0, "nfm"), OFF(160e3f, "Demod Off", 7, "usbd", 0), RAW(48000, "Raw I/Q", 7, "usbd"),
    USB(5000, "USB", 4, "usb"), WFM(160e3f, "WFM (mono)", 1, "wfm", 1), WFM_OIRT(160e3f, "WFM (oirt)", 1, "wfm", 0),
    WFM_STEREO(160e3f, "WFM (stereo)", 1, "wfm", 2);

    private final float bandwidth;
    private final String gqrxMod;
    private final String owrxMod;
    private final int priority;
    private final int sdrppMod;

    private Modulation(float bandwidth, String gqrxMod, int sdrppMod, String owrxMod) {
        this(bandwidth, gqrxMod, sdrppMod, owrxMod, 1);
    }

    private Modulation(float bandwidth, String gqrxMod, int sdrppMod, String owrxMod, int priority) {
        this.bandwidth = bandwidth;
        this.gqrxMod = gqrxMod;
        this.sdrppMod = sdrppMod;
        this.owrxMod = owrxMod;
        this.priority = priority;
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
        for (Modulation mod : sortedValues())
            if (mod.gqrxMod != null && mod.gqrxMod.equalsIgnoreCase(modulationStr)) return mod;
        throw new IllegalArgumentException(modulationStr + " is not a valid gqrx modulation");
    }

    public static Modulation fromOwrxMod(String modName) {
        for (Modulation mod : sortedValues())
            if (mod.owrxMod != null && mod.owrxMod.equalsIgnoreCase(modName)) return mod;
        return Modulation.CUSTOM;
    }

    public static Modulation fromSdrppMod(int code) {
        for (Modulation mod : sortedValues()) if (mod.sdrppMod == code) return mod;
        throw new IllegalArgumentException("Invalid SDR++ modulation code: " + code);
    }

    public static Modulation[] sortedValues() {
        Modulation[] mod = values();
        Arrays.sort(mod, 0, mod.length, (e2, e1) -> e1.priority - e2.priority);
        return mod;
    }

}