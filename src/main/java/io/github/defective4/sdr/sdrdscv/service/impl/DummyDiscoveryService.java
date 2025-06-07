package io.github.defective4.sdr.sdrdscv.service.impl;

import java.util.Collections;
import java.util.List;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.service.DiscoveryService;
import io.github.defective4.sdr.sdrdscv.service.DiscoveryServiceBuilder;

public class DummyDiscoveryService implements DiscoveryService {

    public static class Builder extends DiscoveryServiceBuilder<DummyDiscoveryService> {

        @Override
        public DummyDiscoveryService build() throws Exception {
            return new DummyDiscoveryService();
        }

    }

    @Override
    public List<RadioStation> discover() throws Exception {
        return Collections.emptyList();
    }

}
