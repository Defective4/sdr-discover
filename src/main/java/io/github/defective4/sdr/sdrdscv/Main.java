package io.github.defective4.sdr.sdrdscv;

import io.github.defective4.sdr.sdrdscv.service.BcastFMDiscoveryService;
import io.github.defective4.sdr.sdrdscv.service.BcastFMDiscoveryService.StationNameConflictMode;
import io.github.defective4.sdr.sdrdscv.service.DiscoveryService;

public class Main {
    public static void main(String[] args) {
        try {
            DiscoveryService service = new BcastFMDiscoveryService("rtl_tcp=localhost:55555", 0, 25555, 25556, 25557,
                    100e3f, 2500, 0, -900e3f, 900e3f, 10000, 10000, StationNameConflictMode.SMART, true, true, false);
            service.discover();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
