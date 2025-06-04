package io.github.defective4.sdr.sdrdscv.service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.cli.Converter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.Options;

import io.github.defective4.sdr.sdrdscv.ParamConverters;
import io.github.defective4.sdr.sdrdscv.service.impl.BcastFMDiscoveryService;
import io.github.defective4.sdr.sdrdscv.service.impl.DiscoveryServiceBuilder;

public class ServiceManager {
    private static final Map<String, ServiceEntry> SERVICES = new LinkedHashMap<>();

    static {
        try {
            String bcastFM = "bcastfm";
            Class<? extends DiscoveryServiceBuilder<?>> bcastFMBuilder = BcastFMDiscoveryService.Builder.class;
            Map<Option, Method> bcastFMOptions = makeOptionMap(bcastFMBuilder, bcastFM);
            SERVICES
                    .put(bcastFM, new ServiceEntry(bcastFMBuilder, bcastFMOptions,
                            "Discover Broadcast FM stations by scanning the band for RDS services."));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(67);
        }
    }

    public static Options getAllOptions() {
        Options ops = new Options();
        for (String mod : SERVICES.keySet()) {
            Options sub = makeOptionsFor(mod);
            ops.addOptions(sub);
        }
        return ops;
    }

    public static Map<String, ServiceEntry> getServices() {
        return Collections.unmodifiableMap(SERVICES);
    }

    public static Options makeOptionsFor(String service) {
        if (!SERVICES.containsKey(service)) return null;
        ServiceEntry mod = SERVICES.get(service);
        Options ops = new Options();
        for (Option op : mod.getArguments().keySet()) {
            ops.addOption(op);
        }
        return ops;
    }

    private static Map<Option, Method> makeOptionMap(Class<?> clazz, String prefix) {
        Map<Option, Method> ops = new LinkedHashMap<>();
        Object builderInstance = null;
        try {
            builderInstance = clazz.getConstructor().newInstance();
        } catch (Throwable e) {}
        for (Method method : clazz.getMethods()) if (method.isAnnotationPresent(ServiceArgument.class)) {
            if (method.getParameterCount() > 1)
                throw new IllegalStateException("Method " + method.getName() + " has more than one parameter.");
            ServiceArgument arg = method.getAnnotation(ServiceArgument.class);
            String desc = arg.description();
            if (!desc.endsWith(".")) desc = desc + ".";
            if (!arg.defaultField().isEmpty() && builderInstance != null) try {
                Field field = clazz.getDeclaredField(arg.defaultField());
                field.setAccessible(true);
                String val = String.valueOf(field.get(builderInstance));
                if (val != null) {
                    desc += " (Default: " + val + ")";
                }
            } catch (Throwable e) {}
            Builder opt = Option.builder().longOpt(prefix + "-" + arg.argName()).desc(desc);
            if (method.getParameterCount() == 1) {
                Parameter param = method.getParameters()[0];
                Converter<?, Throwable> converter = ParamConverters.getConverter(param.getType());
                if (converter == null) throw new IllegalStateException(
                        "Couldn't find a converter for param type " + param.getType().getName());
                opt.hasArg().argName(param.getName()).converter(converter);
            }
            ops.put(opt.build(), method);
        }
        return Collections.unmodifiableMap(ops);
    }
}
