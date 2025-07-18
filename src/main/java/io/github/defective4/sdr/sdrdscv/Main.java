package io.github.defective4.sdr.sdrdscv;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Converter;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.github.defective4.sdr.sdrdscv.annotation.ConstructorParam;
import io.github.defective4.sdr.sdrdscv.bookmark.writer.BookmarkWriter;
import io.github.defective4.sdr.sdrdscv.bookmark.writer.BookmarkWriterRegistry;
import io.github.defective4.sdr.sdrdscv.bookmark.writer.BookmarkWriterRegistry.WriterEntry;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.service.BuilderEntry;
import io.github.defective4.sdr.sdrdscv.service.DiscoveryService;
import io.github.defective4.sdr.sdrdscv.service.DiscoveryServiceBuilder;
import io.github.defective4.sdr.sdrdscv.service.ServiceManager;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecorator;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecoratorBuilder;
import io.github.defective4.sdr.sdrdscv.service.decorator.impl.ChainServiceDecorator;
import io.github.defective4.sdr.sdrdscv.service.impl.BookmarksDiscoveryService.ReaderId;

public class Main {
    public static final String APP_NAME = "sdr-discover";
    private static final Options rootOptions;

    static {
        rootOptions = new Options()
                .addOption(Option.builder("D").argName("decorator").hasArg()
                        .desc("Attach a decorator to current service.").longOpt("decorate").build())
                .addOption(Option.builder("O").argName("output-file").hasArg()
                        .desc("Specify where to write command's output. Use \"-\" for standard output.")
                        .longOpt("output").build())
                .addOption(Option.builder("F").argName("format").hasArg().desc("Specify which output format to use.")
                        .longOpt("output-format").build())
                .addOption(Option.builder("S").argName("service").hasArg().desc("Add a discovery service.")
                        .longOpt("add-service").build())
                .addOption(Option.builder("h").desc("Show this help.").longOpt("help").build())
                .addOption(Option.builder()
                        .desc("Show help about a service, or don't provide an argument to show help for all services.")
                        .longOpt("help-service").hasArg().argName("service").optionalArg(true).build())
                .addOption(Option.builder().desc(
                        "Show help about a decorator, or don't provide an argument to show help for all decorators.")
                        .longOpt("help-decorator").hasArg().argName("decorator").optionalArg(true).build())
                .addOption(Option.builder().desc(
                        "Show help about an output format, or don't probide an argument to show help for all output formats.")
                        .longOpt("help-output").hasArg().argName("output").optionalArg(true).build())
                .addOption(Option.builder("v").desc("Be verbose.").longOpt("verbose").build());
    }

    @SuppressWarnings("resource")
    public static void main(String[] a) {
        System.err.print("> ");
        String[] args = new Scanner(System.in).nextLine().split(" ");

        Options allOptions = new Options();
        allOptions.addOptions(rootOptions);
        allOptions.addOptions(ServiceManager.getAllOptions());
        allOptions.addOptions(BookmarkWriterRegistry.constructOptions());

        CommandLineParser parser = new DefaultParser();

        CommandLine cli;
        try {
            cli = parser.parse(allOptions, args, false);
        } catch (ParseException e) {
            printHelp(e.getMessage());
            return;
        }
        if (cli.getOptions().length == 0 || cli.hasOption('h')) {
            printHelp(null);
            return;
        }
        if (cli.hasOption("--help-service")) {
            String val = cli.getOptionValue("--help-service");
            printServiceHelp(val);
            return;
        }
        if (cli.hasOption("--help-output")) {
            String val = cli.getOptionValue("--help-output");
            printOutputHelp(val);
            return;
        }
        if (cli.hasOption("--help-decorator")) {
            String val = cli.getOptionValue("--help-decorator");
            printDecoratorHelp(val);
            return;
        }

        boolean verbose = cli.hasOption('v');

        if (cli.hasOption('S')) {
            if (!cli.hasOption('F')) {
                printHelp("You must specify an output format");
                return;
            }
            if (!cli.hasOption('O')) {
                printHelp("Missing output file name");
                return;
            }
            String oName = cli.getOptionValue('O');
            String outputFormatName = cli.getOptionValue('F');
            WriterEntry writerEntry = BookmarkWriterRegistry.getWriterForID(outputFormatName);
            if (writerEntry == null) {
                printHelp("Unknown output format: " + outputFormatName);
                return;
            }
            Map<String, Object> writerParams = new HashMap<>();
            for (Entry<Parameter, ConstructorParam> entry : writerEntry.getParams().entrySet()) {
                ConstructorParam paramAnnotation = entry.getValue();
                Parameter param = entry.getKey();
                Object value;
                String key = outputFormatName.toLowerCase() + "-" + paramAnnotation.argName();
                if (param.getType() == boolean.class) {
                    boolean defVal = "true".equalsIgnoreCase(paramAnnotation.defaultValue());
                    if (cli.hasOption(key)) {
                        defVal = !defVal;
                    }
                    value = defVal;
                } else {
                    Converter<?, ?> conv = ParamConverters.getConverter(param.getType());
                    if (conv == null)
                        throw new IllegalStateException("Couldn't find a param converter for type " + param.getType());
                    try {
                        value = ParamConverters.convert(conv, paramAnnotation.defaultValue());
                    } catch (ParseException e) {
                        throw new IllegalStateException(e);
                    }
                    if (cli.hasOption(key)) {
                        try {
                            value = ParamConverters.convert(conv, cli.getOptionValue(key));
                        } catch (ParseException e) {
                            System.err.println(
                                    String.format("Invalid value \"%s\" for option %s", cli.getOptionValue(key), key));
                            return;
                        }
                    }

                }

                writerParams.put(param.getName(), value);
            }

            BookmarkWriter writer;

            try {
                Class<? extends BookmarkWriter> writerClass = writerEntry.getWriterClass();
                Constructor<?> constructor = writerClass.getConstructors()[0];
                List<Object> params = new ArrayList<>();
                for (Parameter param : constructor.getParameters()) {
                    params.add(writerParams.get(param.getName()));
                }
                writer = (BookmarkWriter) constructor.newInstance(params.toArray(new Object[0]));
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }

            try (Writer oWriter = new OutputStreamWriter(
                    "-".equals(oName) ? System.out : Files.newOutputStream(Path.of(oName)))) {

                Map<Integer, Map<Option, String>> serviceOptions = new HashMap<>();
                Map<Integer, String[]> serviceDecorators = new HashMap<>();
                Map<Option, Integer> opTrack = new HashMap<>();
                Map<Option, Method> lastService = new HashMap<>();
                int svcId = -1;

                for (Option op : cli.getOptions()) {
                    int opNum = opTrack.getOrDefault(op, 0);
                    switch (op.getKey()) {
                        case "S": {
                            BuilderEntry<? extends DiscoveryServiceBuilder<?>> service = ServiceManager
                                    .getService(cli.getOptionValues(op)[opNum]);
                            lastService.clear();
                            if (service != null) {
                                lastService.putAll(service.getArguments());
                            }
                            svcId++;
                            break;
                        }
                        case "D": {
                            if (svcId < 0) {
                                break;
                            }
                            String[] decorators = cli.getOptionValues(op)[opNum].split(",");
                            for (int i = 0; i < decorators.length; i++) {
                                decorators[i] = decorators[i].trim();
                            }
                            serviceDecorators.put(svcId, decorators);
                            break;
                        }
                        default: {
                            if (svcId < 0) {
                                break;
                            }
                            String val = null;
                            String[] vals = cli.getOptionValues(op);
                            if (vals != null) {
                                val = vals[opNum];
                            }
                            serviceOptions.computeIfAbsent(svcId, e -> new HashMap<>()).put(op, val);
                            break;
                        }
                    }
                    opTrack.compute(op, (o, e) -> e == null ? 1 : e + 1);
                }

                opTrack.clear();
                lastService.clear();

                List<RadioStation> stations = new ArrayList<>();
                String[] values = cli.getOptionValues('S');
                List<RadioStation> lastToDecorate = null;
                ChainServiceDecorator lastChainDecorator = null;

                for (int sid = 0; sid < values.length; sid++) {
                    String serviceName = values[sid];
                    BuilderEntry<? extends DiscoveryServiceBuilder<?>> service = ServiceManager.getService(serviceName);
                    if (service == null) {
                        System.out.println("Service not found: " + serviceName);
                        return;
                    }

                    try {
                        DiscoveryServiceBuilder<?> builder = service.getBuilderClass().getConstructor().newInstance();
                        if (verbose) {
                            builder.verbose();
                        }
                        Map<Option, String> sOps = serviceOptions.getOrDefault(sid, new HashMap<>());
                        String[] decoratorIds = serviceDecorators.get(sid);
                        List<ServiceDecorator> decorators = new ArrayList<>();

                        if (decoratorIds != null) {
                            for (String id : decoratorIds) {
                                BuilderEntry<? extends ServiceDecoratorBuilder<?>> decoratorEntry = ServiceManager
                                        .getDecorator(id);
                                if (decoratorEntry == null) {
                                    printHelp("Unknown decorator: " + id);
                                    return;
                                }
                                ServiceDecoratorBuilder<?> decBuilder = decoratorEntry.getBuilderClass()
                                        .getConstructor().newInstance();
                                for (Entry<Option, Method> entry : decoratorEntry.getArguments().entrySet()) {
                                    Option key = entry.getKey();
                                    if (cli.hasOption(key) && sOps.containsKey(key)) {
                                        try {
                                            String rawVal = sOps.get(key);
                                            if (rawVal == null) {
                                                entry.getValue().invoke(decBuilder);
                                                continue;
                                            }
                                            Object value;
                                            if (key.getConverter() != null) {
                                                value = ParamConverters.convert(key.getConverter(), rawVal);
                                            } else {
                                                value = rawVal;
                                            }
                                            entry.getValue().invoke(decBuilder, value);
                                        } catch (ParseException e) {
                                            System.err.println(String.format("Invalid value \"%s\" for option %s",
                                                    cli.getOptionValue(key), key.getKey()));
                                            return;
                                        }
                                    }
                                }
                                try {
                                    decorators.add(decBuilder.build());
                                } catch (Exception e) {
                                    printDecoratorHelp(id);
                                    System.err.println("Couldn't create decorator: " + e.getMessage());
                                    return;
                                }
                            }
                        }

                        for (Entry<Option, Method> entry : service.getArguments().entrySet()) {
                            Option key = entry.getKey();
                            if (cli.hasOption(key) && sOps.containsKey(key)) {
                                try {
                                    String rawVal = sOps.get(key);
                                    if (rawVal == null) {
                                        entry.getValue().invoke(builder);
                                        continue;
                                    }
                                    Object value;
                                    if (key.getConverter() != null) {
                                        value = ParamConverters.convert(key.getConverter(), rawVal);
                                    } else {
                                        value = rawVal;
                                    }
                                    entry.getValue().invoke(builder, value);
                                } catch (ParseException e) {
                                    System.err.println(String.format("Invalid value \"%s\" for option %s",
                                            cli.getOptionValue(key), key.getKey()));
                                    return;
                                }
                            }
                        }
                        DiscoveryService svc;
                        try {
                            svc = builder.build();
                        } catch (Exception e) {
                            printServiceHelp(serviceName);
                            System.err.println("Couldn't create service: " + e.getMessage());
                            return;
                        }
                        if (lastToDecorate != null && !svc.isDecoratingSupported()) {
                            System.err.println("Service \"" + serviceName + "\" doesn't support chain decorating.");
                            return;
                        }
                        List<RadioStation> discovered = lastToDecorate == null ? svc.discover()
                                : svc.decorate(lastToDecorate, lastChainDecorator);
                        if (lastToDecorate != null) {
                            lastToDecorate = null;
                        }
                        if (verbose) {
                            System.err.println(
                                    "Service \"" + serviceName + "\" discovered " + discovered.size() + " stations.");
                            if (decorators.size() > 0) {
                                System.err.println("Running the result through " + decorators.size() + " decorators.");
                            }
                        }
                        int prev = discovered.size();
                        boolean decorateMode = false;
                        for (ServiceDecorator decorator : decorators) {
                            if (decorator instanceof ChainServiceDecorator csd) {
                                lastChainDecorator = csd;
                                decorateMode = true;
                                break;
                            }
                            discovered = decorator.decorate(discovered);
                            if (discovered.size() != prev) throw new IllegalStateException(
                                    "One of decorators returned an invalid amount of stations.");
                        }
                        if (decorateMode) {
                            lastToDecorate = discovered;
                        } else {
                            stations.addAll(discovered);
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }

                writer.write(oWriter, stations);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("You must add at least one service");
        }
    }

    private static String createBookmarkReadersString() {
        StringBuilder builder = new StringBuilder();
        for (ReaderId reader : ReaderId.values()) {
            builder.append(" - " + reader.name().toLowerCase() + " - " + reader.getDescription() + "\n");
        }
        return builder.toString();
    }

    private static String createDecoratorsString() {
        StringBuilder builder = new StringBuilder();
        for (Entry<String, BuilderEntry<? extends ServiceDecoratorBuilder<?>>> entry : ServiceManager.getDecorators()
                .entrySet()) {
            builder.append(String.format(" - %s - %s\n", entry.getKey(), entry.getValue().getDescription()));
        }
        return builder.toString();
    }

    private static String createOutputsString() {
        StringBuilder builder = new StringBuilder();
        for (Entry<String, WriterEntry> entry : BookmarkWriterRegistry.getWriters().entrySet()) {
            builder.append(String.format(" - %s - %s\n", entry.getKey(), entry.getValue().getDescription()));
        }
        return builder.toString();
    }

    private static String createServicesString() {
        StringBuilder builder = new StringBuilder();
        for (Entry<String, BuilderEntry<? extends DiscoveryServiceBuilder<?>>> entry : ServiceManager.getServices()
                .entrySet()) {
            builder.append(String.format(" - %s - %s\n", entry.getKey(), entry.getValue().getDescription()));
        }
        return builder.toString();
    }

    private static void printDecoratorHelp(String decorator) {
        Options ops = new Options();
        String footer;
        if (decorator == null) {
            ops.addOptions(ServiceManager.getDecoratorOptions());
            footer = "\nAvailable decorators:\n" + createDecoratorsString();
        } else {
            footer = null;
            BuilderEntry<? extends ServiceDecoratorBuilder<?>> svc = ServiceManager.getDecorator(decorator);
            if (svc == null) {
                System.err.println("Decorator not found: " + decorator);
                return;
            }
            for (Option op : svc.getArguments().keySet()) {
                ops.addOption(op);
            }
        }
        new HelpFormatter().printHelp(128, APP_NAME + " -S <service> <options> -D "
                + (decorator == null ? "<decorator>" : decorator) + " [options] [output]", null, ops, footer);
    }

    private static void printHelp(String message) {
        new HelpFormatter().printHelp(APP_NAME + " [-S service] [options] [-F format] [-O filename]", null, rootOptions,
                message == null
                        ? "\nAvailable services:\n" + createServicesString() + "\nAvailable outputs:\n"
                                + createOutputsString() + "\nAvailable bookmark readers:\n"
                                + createBookmarkReadersString() + "\nAvailable decorators:\n" + createDecoratorsString()
                        : "\n" + message);
    }

    private static void printOutputHelp(String output) {
        Options ops = new Options();
        String footer;
        if (output == null) {
            ops.addOptions(BookmarkWriterRegistry.constructOptions());
            footer = "\nAvailable output formats:\n" + createOutputsString();
        } else {
            footer = null;
            WriterEntry entry = BookmarkWriterRegistry.getWriterForID(output);
            if (entry == null) {
                System.err.println("Output format not found: " + output);
                return;
            }
            ops.addOptions(BookmarkWriterRegistry.constructOptions(output, entry));
        }
        new HelpFormatter().printHelp(128, APP_NAME + " -O " + (output == null ? "<format>" : output) + " [output]",
                null, ops, footer);
    }

    private static void printServiceHelp(String service) {
        Options ops = new Options();
        String footer;
        if (service == null) {
            ops.addOptions(ServiceManager.getServiceOptions());
            footer = "\nAvailable services:\n" + createServicesString();
        } else {
            footer = null;
            BuilderEntry<? extends DiscoveryServiceBuilder<?>> svc = ServiceManager.getService(service);
            if (svc == null) {
                System.err.println("Service not found: " + service);
                return;
            }
            for (Option op : svc.getArguments().keySet()) {
                ops.addOption(op);
            }
        }
        new HelpFormatter().printHelp(128,
                APP_NAME + " -S " + (service == null ? "<service>" : service) + " [options] [output]", null, ops,
                footer);
    }
}
