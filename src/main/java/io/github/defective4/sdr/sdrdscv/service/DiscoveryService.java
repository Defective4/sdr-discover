package io.github.defective4.sdr.sdrdscv.service;

import java.util.List;

import io.github.defective4.sdr.sdrdscv.RadioStation;

public interface DiscoveryService {
    List<RadioStation> discover() throws Exception;
}
