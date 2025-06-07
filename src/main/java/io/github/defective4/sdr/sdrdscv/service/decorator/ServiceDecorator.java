package io.github.defective4.sdr.sdrdscv.service.decorator;

import java.util.List;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public interface ServiceDecorator {
    List<RadioStation> decorate(List<RadioStation> stations);
}
