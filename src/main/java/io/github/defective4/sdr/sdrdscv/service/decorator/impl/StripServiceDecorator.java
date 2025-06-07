package io.github.defective4.sdr.sdrdscv.service.decorator.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecorator;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecoratorBuilder;

public class StripServiceDecorator implements ServiceDecorator {

    public static class Builder implements ServiceDecoratorBuilder<StripServiceDecorator> {
        @Override
        public StripServiceDecorator build() {
            return new StripServiceDecorator();
        }
    }

    @Override
    public List<RadioStation> decorate(List<RadioStation> stations) {
        List<RadioStation> decorated = new ArrayList<>();
        for (RadioStation station : stations)
            decorated.add(new RadioStation(station.getName(), station.getFrequency(), station.getModulation()));
        return Collections.unmodifiableList(decorated);
    }

}
