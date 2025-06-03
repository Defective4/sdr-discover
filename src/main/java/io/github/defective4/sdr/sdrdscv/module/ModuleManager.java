package io.github.defective4.sdr.sdrdscv.module;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.Options;

import io.github.defective4.sdr.sdrdscv.service.BcastFMDiscoveryService;

public class ModuleManager {
    private static final Map<String, Module> MODULES = new LinkedHashMap<>();

    static {
        try {
            String bcastFM = "bcastfm";
            Class<?> bcastFMBuilder = BcastFMDiscoveryService.Builder.class;
            Map<Option, Method> bcastFMOptions = makeOptionMap(bcastFMBuilder, bcastFM);
            MODULES.put(bcastFM, new Module(bcastFMBuilder, bcastFMOptions, "Broadcast FM discovery service"));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(67);
        }
    }

    public static Options getAllOptions() {
        Options ops = new Options();
        for (String mod : MODULES.keySet()) {
            Options sub = makeOptionsFor(mod);
            ops.addOptions(sub);
        }
        return ops;
    }

    public static Map<String, Module> getModules() {
        return Collections.unmodifiableMap(MODULES);
    }

    public static Options makeOptionsFor(String module) {
        if (!MODULES.containsKey(module)) return null;
        Module mod = MODULES.get(module);
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
        for (Method method : clazz.getMethods()) if (method.isAnnotationPresent(ModuleArgument.class)) {
            if (method.getParameterCount() > 1)
                throw new IllegalStateException("Method " + method.getName() + " has more than one parameter.");
            ModuleArgument arg = method.getAnnotation(ModuleArgument.class);
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
                opt.hasArg().argName(method.getParameters()[0].getName());
            }
            ops.put(opt.build(), method);
        }
        return Collections.unmodifiableMap(ops);
    }
}
