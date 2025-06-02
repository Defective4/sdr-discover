package io.github.defective4.sdr.sdrdscv.radio.tuner;

import io.github.defective4.sdr.msg.MessagePair;
import io.github.defective4.sdr.msg.RawMessageSender;

public class OffsetTuner extends Tuner {

    public static final float MAX_DISTANCE = 900e3f;

    private float freq = Float.MAX_VALUE;

    public OffsetTuner(RawMessageSender controller) {
        super(controller);
    }

    @Override
    public void tune(float frequency) {
        if (Math.abs(frequency - freq) > MAX_DISTANCE) {
            setCenterFrequency(frequency);
            setOffsetFrequency(0);
        } else {
            setOffsetFrequency(frequency - freq);
        }
    }

    private void setCenterFrequency(float freq) {
        this.freq = freq;
        getController().sendMessage(new MessagePair(COMMAND_FREQUENCY, (double) freq));
    }

    private void setOffsetFrequency(float offset) {
        getController().sendMessage(new MessagePair(COMMAND_FREQUENCY_OFFSET, (double) offset));
    }
}
