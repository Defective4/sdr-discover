package io.github.defective4.sdr.sdrdscv.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation.Modulation;
import io.github.defective4.sdr.sdrdscv.service.DiscoveryService;
import io.github.defective4.sdr.sdrdscv.service.DiscoveryServiceBuilder;
import io.github.defective4.sdr.sdrdscv.service.decorator.impl.ChainServiceDecorator;

public class DummyDiscoveryService implements DiscoveryService {

    public static class Builder extends DiscoveryServiceBuilder<DummyDiscoveryService> {

        @Override
        public DummyDiscoveryService build() throws Exception {
            return new DummyDiscoveryService();
        }

    }

    @Override
    public List<RadioStation> decorate(List<RadioStation> decorate, ChainServiceDecorator decorator) throws Exception {
        List<RadioStation> decorated = new ArrayList<>();
        for (RadioStation st : decorate) decorated
                .add(new RadioStation(st.getName() + " - DUMMY", st.getFrequency(), st.getModulation(),
                        st.getMetadata()));
        return Collections.unmodifiableList(decorated);
    }

    @Override
    public List<RadioStation> discover() throws Exception {
        return Collections.singletonList(new RadioStation("TEST", 137e6f, Modulation.WFM));
    }

    @Override
    public boolean isDecoratingSupported() {
        return true;
    }

}
