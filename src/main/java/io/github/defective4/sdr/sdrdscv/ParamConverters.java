package io.github.defective4.sdr.sdrdscv;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.Converter;
import org.apache.commons.cli.ParseException;

import io.github.defective4.sdr.sdrdscv.service.impl.BcastFMDiscoveryService;

public class ParamConverters {

    private static final Map<Class<?>, Converter<?, Throwable>> CONVERTERS = new HashMap<>();

    static {
        CONVERTERS
                .putAll(Map
                        .of(String[].class, e -> e.split(","), Color.class, Color::decode, char.class,
                                str -> str.charAt(0), int.class, Integer::parseInt, long.class, Long::parseLong,
                                float.class, Float::parseFloat, boolean.class, Boolean::parseBoolean, String.class,
                                str -> str, double.class, Double::parseDouble,
                                BcastFMDiscoveryService.StationNameConflictMode.class,
                                str -> BcastFMDiscoveryService.StationNameConflictMode.valueOf(str.toUpperCase())));
    }

    private ParamConverters() {}

    public static Object convert(Converter<?, ?> conv, String val) throws ParseException {
        try {
            return conv.apply(val);
        } catch (Throwable e) {
            throw new ParseException(e);
        }
    }

    public static String encodeColor(Color color) {
        String encoded = Integer.toHexString(color.getRGB() & 0xffffff);
        while (encoded.length() < 6) encoded = "0" + encoded;
        return "#" + encoded;
    }

    public static Converter<?, Throwable> getConverter(Class<?> paramType) {
        return CONVERTERS.get(paramType);
    }
}
