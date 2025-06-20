package io.github.defective4.sdr.sdrdscv.service;

import java.util.List;

import io.github.defective4.sdr.msg.RawMessageSender;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.radio.tuner.OffsetTuner;
import io.github.defective4.sdr.sdrdscv.radio.tuner.SimpleTuner;
import io.github.defective4.sdr.sdrdscv.radio.tuner.Tuner;
import io.github.defective4.sdr.sdrdscv.service.decorator.impl.ChainServiceDecorator;

public interface DiscoveryService {

    List<RadioStation> decorate(List<RadioStation> decorate, ChainServiceDecorator decorator) throws Exception;

    List<RadioStation> discover() throws Exception;

    boolean isDecoratingSupported();

    public static Tuner createTuner(RawMessageSender controller, boolean offset) {
        return offset ? new OffsetTuner(controller) : new SimpleTuner(controller);
    }
}
