package io.github.defective4.sdr.sdrdscv;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.Converter;

import io.github.defective4.sdr.sdrdscv.service.impl.BcastFMDiscoveryService;

public class ParamConverters {

    private static final Map<Class<?>, Converter<?, Throwable>> CONVERTERS = new HashMap<>();

    static {
        CONVERTERS
                .putAll(Map
                        .of(int.class, Integer::parseInt, long.class, Long::parseLong, float.class, Float::parseFloat,
                                boolean.class, Boolean::parseBoolean, String.class, str -> str, double.class,
                                Double::parseDouble, BcastFMDiscoveryService.StationNameConflictMode.class,
                                str -> BcastFMDiscoveryService.StationNameConflictMode.valueOf(str.toUpperCase())));
    }

    private ParamConverters() {}

    public static Converter<?, Throwable> getConverter(Class<?> paramType) {
        return CONVERTERS.get(paramType);
    }
}
