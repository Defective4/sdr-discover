package io.github.defective4.sdr.sdrdscv.module;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.cli.Option;

import io.github.defective4.sdr.sdrdscv.service.DiscoveryServiceBuilder;

public class Module {
    private final Map<Option, Method> arguments;
    private final Class<? extends DiscoveryServiceBuilder> builderClass;
    private final String description;

    public Module(Class<? extends DiscoveryServiceBuilder> builderClass, Map<Option, Method> arguments,
            String description) {
        this.builderClass = builderClass;
        this.arguments = arguments;
        this.description = description;
    }

    public Map<Option, Method> getArguments() {
        return arguments;
    }

    public Class<? extends DiscoveryServiceBuilder> getBuilderClass() {
        return builderClass;
    }

    public String getDescription() {
        return description;
    }

}
