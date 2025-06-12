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
import io.github.defective4.sdr.sdrdscv.annotation.BuilderParam;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecoratorBuilder;
import io.github.defective4.sdr.sdrdscv.service.decorator.impl.ChainServiceDecorator;
import io.github.defective4.sdr.sdrdscv.service.decorator.impl.StripServiceDecorator;
import io.github.defective4.sdr.sdrdscv.service.decorator.impl.TagServiceDecorator;
import io.github.defective4.sdr.sdrdscv.service.impl.BcastFMDiscoveryService;
import io.github.defective4.sdr.sdrdscv.service.impl.BookmarksDiscoveryService;

public class ServiceManager {
    private static final Map<String, BuilderEntry<? extends ServiceDecoratorBuilder<?>>> DECORATORS = new LinkedHashMap<>();
    private static final Map<String, BuilderEntry<? extends DiscoveryServiceBuilder<?>>> SERVICES = new LinkedHashMap<>();

    static {
        try {
            putServiceEntry("bcastfm", BcastFMDiscoveryService.Builder.class,
                    "Discover Broadcast FM stations by scanning the band for RDS services.");
            putServiceEntry("bookmarks", BookmarksDiscoveryService.Builder.class, "Read stations from a bookmark file");

            putDecoratorEntry("tag", TagServiceDecorator.Builder.class,
                    "Decorates detected stations with colored tags. The tags are compatible with Gqrx.");
            putDecoratorEntry("strip", StripServiceDecorator.Builder.class,
                    "Strips metadata from discovered stations. Unless specified otherwise, this decorator will remove ALL metadata values.");
            putDecoratorEntry("chain", ChainServiceDecorator.Builder.class,
                    "Use results of the next service to decorate this one.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(67);
        }
    }

    public static Options getAllOptions() {
        Options ops = new Options();
        ops.addOptions(getServiceOptions());
        ops.addOptions(getDecoratorOptions());
        return ops;
    }

    public static BuilderEntry<? extends ServiceDecoratorBuilder<?>> getDecorator(String service) {
        return DECORATORS.get(service.toLowerCase());
    }

    public static Options getDecoratorOptions() {
        Options ops = new Options();
        for (String mod : DECORATORS.keySet()) {
            Options sub = makeOptionsForDecorator(mod);
            ops.addOptions(sub);
        }
        return ops;
    }

    public static Map<String, BuilderEntry<? extends ServiceDecoratorBuilder<?>>> getDecorators() {
        return Collections.unmodifiableMap(DECORATORS);
    }

    public static BuilderEntry<? extends DiscoveryServiceBuilder<?>> getService(String service) {
        return SERVICES.get(service.toLowerCase());
    }

    public static Options getServiceOptions() {
        Options ops = new Options();
        for (String mod : SERVICES.keySet()) {
            Options sub = makeOptionsForService(mod);
            ops.addOptions(sub);
        }
        return ops;
    }

    public static Map<String, BuilderEntry<? extends DiscoveryServiceBuilder<?>>> getServices() {
        return Collections.unmodifiableMap(SERVICES);
    }

    public static Options makeOptionsForDecorator(String decorator) {
        if (!DECORATORS.containsKey(decorator)) return null;
        BuilderEntry<? extends ServiceDecoratorBuilder<?>> mod = DECORATORS.get(decorator);
        Options ops = new Options();
        for (Option op : mod.getArguments().keySet()) {
            ops.addOption(op);
        }
        return ops;
    }

    public static Options makeOptionsForService(String service) {
        if (!SERVICES.containsKey(service)) return null;
        BuilderEntry<? extends DiscoveryServiceBuilder<?>> mod = SERVICES.get(service);
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
        } catch (Throwable e) {
        }
        for (Method method : clazz.getMethods()) if (method.isAnnotationPresent(BuilderParam.class)) {
            if (method.getParameterCount() > 1)
                throw new IllegalStateException("Method " + method.getName() + " has more than one parameter.");
            BuilderParam arg = method.getAnnotation(BuilderParam.class);
            String desc = arg.description();
            if (!desc.endsWith(".")) {
                desc = desc + ".";
            }
            if (!arg.defaultField().isEmpty() && builderInstance != null) {
                try {
                    Field field = clazz.getDeclaredField(arg.defaultField());
                    field.setAccessible(true);
                    String val = String.valueOf(field.get(builderInstance));
                    if (val != null) {
                        desc += " (Default: " + val + ")";
                    }
                } catch (Throwable e) {
                }
            }
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

    private static void putDecoratorEntry(String id, Class<? extends ServiceDecoratorBuilder<?>> builderClass,
            String description) {
        Map<Option, Method> options = makeOptionMap(builderClass, id);
        DECORATORS.put(id, new BuilderEntry<>(builderClass, options, description));
    }

    private static void putServiceEntry(String id, Class<? extends DiscoveryServiceBuilder<?>> builderClass,
            String description) {
        Map<Option, Method> options = makeOptionMap(builderClass, id);
        SERVICES.put(id, new BuilderEntry<>(builderClass, options, description));
    }
}
