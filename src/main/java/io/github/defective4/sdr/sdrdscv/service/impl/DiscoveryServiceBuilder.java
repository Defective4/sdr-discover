package io.github.defective4.sdr.sdrdscv.service.impl;

public abstract class DiscoveryServiceBuilder {

    protected boolean verbose;

    public abstract DiscoveryService build();

    public DiscoveryServiceBuilder verbose() {
        verbose = true;
        return this;
    }
}
