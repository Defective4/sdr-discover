package io.github.defective4.sdr.sdrdscv.service.impl;

public abstract class DiscoveryServiceBuilder<T extends DiscoveryService> {

    protected boolean verbose;

    public abstract T build() throws Exception;

    public DiscoveryServiceBuilder<T> verbose() {
        verbose = true;
        return this;
    }
}
