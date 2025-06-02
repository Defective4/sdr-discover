package io.github.defective4.sdr.sdrdscv.radio.tuner;

import io.github.defective4.sdr.msg.RawMessageSender;

public abstract class Tuner {
    public static final String COMMAND_FREQUENCY = "freq";
    public static final String COMMAND_FREQUENCY_OFFSET = "freq_offset";
    private final RawMessageSender controller;

    protected Tuner(RawMessageSender controller) {
        this.controller = controller;
    }

    public RawMessageSender getController() {
        return controller;
    }

    public abstract void tune(float frequency);

}
