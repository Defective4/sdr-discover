package io.github.defective4.sdr.sdrdscv.service.impl;

import java.util.List;

import io.github.defective4.sdr.msg.RawMessageSender;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.radio.tuner.OffsetTuner;
import io.github.defective4.sdr.sdrdscv.radio.tuner.SimpleTuner;
import io.github.defective4.sdr.sdrdscv.radio.tuner.Tuner;

public interface DiscoveryService {
    List<RadioStation> discover() throws Exception;

    public static Tuner createTuner(RawMessageSender controller, boolean offset) {
        return offset ? new OffsetTuner(controller) : new SimpleTuner(controller);
    }
}
