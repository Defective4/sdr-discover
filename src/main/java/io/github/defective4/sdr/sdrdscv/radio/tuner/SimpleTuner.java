package io.github.defective4.sdr.sdrdscv.radio.tuner;

import io.github.defective4.sdr.msg.MessagePair;
import io.github.defective4.sdr.msg.RawMessageSender;

public class SimpleTuner extends Tuner {

    public SimpleTuner(RawMessageSender controller) {
        super(controller);
    }

    @Override
    public void tune(float frequency) {
        getController().sendMessage(new MessagePair(COMMAND_FREQUENCY, (double) frequency));
    }
}
