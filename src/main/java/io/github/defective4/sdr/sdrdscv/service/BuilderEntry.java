package io.github.defective4.sdr.sdrdscv.service;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.cli.Option;

public class BuilderEntry<T> {
    private final Map<Option, Method> arguments;
    private final Class<T> builderClass;
    private final String description;

    public BuilderEntry(Class<T> builderClass, Map<Option, Method> arguments, String description) {
        this.builderClass = builderClass;
        this.arguments = arguments;
        this.description = description;
    }

    public Map<Option, Method> getArguments() {
        return arguments;
    }

    public Class<T> getBuilderClass() {
        return builderClass;
    }

    public String getDescription() {
        return description;
    }
}
