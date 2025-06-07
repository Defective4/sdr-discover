package io.github.defective4.sdr.sdrdscv.service.decorator.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.github.defective4.sdr.sdrdscv.annotation.BuilderParam;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecorator;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecoratorBuilder;

public class StripServiceDecorator implements ServiceDecorator {

    public static class Builder implements ServiceDecoratorBuilder<StripServiceDecorator> {

        private String[] fields;

        @Override
        public StripServiceDecorator build() {
            return new StripServiceDecorator(fields);
        }

        @BuilderParam(argName = "fields", description = "Comma-separated list of metadata fields that should be removed")
        public void fields(String[] fields) {
            this.fields = fields;
        }
    }

    private final String[] fields;

    private StripServiceDecorator(String[] fields) {
        this.fields = fields;
    }

    @Override
    public List<RadioStation> decorate(List<RadioStation> stations) {
        List<RadioStation> decorated = new ArrayList<>();
        for (RadioStation station : stations) {
            Map<String, Object> metadata = new HashMap<>();
            if (fields != null) {
                for (Entry<String, Object> entry : station.getMetadata().entrySet()) {
                    for (String field : fields)
                        if (!field.equals(entry.getKey())) metadata.put(entry.getKey(), entry.getValue());
                }
            }
            decorated
                    .add(new RadioStation(station.getName(), station.getFrequency(), station.getModulation(),
                            metadata));
        }
        return Collections.unmodifiableList(decorated);
    }

}
