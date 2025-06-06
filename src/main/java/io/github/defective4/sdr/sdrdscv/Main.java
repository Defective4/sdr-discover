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

import io.github.defective4.sdr.sdrdscv.io.writer.BookmarkWriter;
import io.github.defective4.sdr.sdrdscv.io.writer.BookmarkWriterRegistry;
import io.github.defective4.sdr.sdrdscv.io.writer.BookmarkWriterRegistry.WriterEntry;
import io.github.defective4.sdr.sdrdscv.io.writer.WriterParam;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.service.ServiceEntry;
import io.github.defective4.sdr.sdrdscv.service.ServiceManager;
import io.github.defective4.sdr.sdrdscv.service.impl.BookmarksDiscoveryService.ReaderId;
import io.github.defective4.sdr.sdrdscv.service.impl.DiscoveryService;
import io.github.defective4.sdr.sdrdscv.service.impl.DiscoveryServiceBuilder;

public class Main {
    public static final String APP_NAME = "sdr-discover";
    private static final Options rootOptions;

    static {
        rootOptions = new Options()
                .addOption(Option
                        .builder("O")
                        .argName("output-file")
                        .hasArgs()
                        .desc("Specify where to write command's output. Use \"-\" for standard output.")
                        .longOpt("output")
                        .build())
                .addOption(Option
                        .builder("F")
                        .argName("format")
                        .hasArgs()
                        .desc("Specify which output format to use.")
                        .longOpt("output-format")
                        .build())
                .addOption(Option
                        .builder("S")
                        .argName("service")
                        .hasArg()
                        .desc("Add a discovery service.")
                        .longOpt("add-service")
                        .build())
                .addOption(Option.builder("h").desc("Show this help.").longOpt("help").build())
                .addOption(Option
                        .builder("H")
                        .desc("Show help about a service, or don't provide an argument to show help for all services.")
                        .longOpt("help-service")
                        .hasArg()
                        .argName("service")
                        .optionalArg(true)
                        .build())
                .addOption(Option
                        .builder()
                        .desc("Show help about an output format, or don't probide an argument to show help for all output formats")
                        .longOpt("help-output")
                        .hasArg()
                        .argName("output")
                        .optionalArg(true)
                        .build())
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
        if (cli.hasOption('H')) {
            String val = cli.getOptionValue('H');
            printServiceHelp(val);
            return;
        }
        if (cli.hasOption("--help-output")) {
            String val = cli.getOptionValue("--help-output");
            printOutputHelp(val);
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
            for (Entry<Parameter, WriterParam> entry : writerEntry.getParams().entrySet()) {
                WriterParam paramAnnotation = entry.getValue();
                Parameter param = entry.getKey();
                Object value;
                String key = outputFormatName.toLowerCase() + "-" + paramAnnotation.argName();
                if (param.getType() == boolean.class) {
                    boolean defVal = "true".equalsIgnoreCase(paramAnnotation.defaultValue());
                    if (cli.hasOption(key)) defVal = !defVal;
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
                            System.err
                                    .println(String
                                            .format("Invalid value \"%s\" for option %s", cli.getOptionValue(key),
                                                    key));
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
                for (Parameter param : constructor.getParameters()) params.add(writerParams.get(param.getName()));
                writer = (BookmarkWriter) constructor.newInstance(params.toArray(new Object[0]));
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }

            try (Writer oWriter = new OutputStreamWriter(
                    "-".equals(oName) ? System.out : Files.newOutputStream(Path.of(oName)))) {
                List<RadioStation> stations = new ArrayList<>();
                String[] values = cli.getOptionValues('S');
                for (int i = 0; i < values.length; i++) {
                    String serviceName = values[i];
                    ServiceEntry service = ServiceManager.getService(serviceName);
                    if (service == null) {
                        System.out.println("Service not found: " + serviceName);
                        return;
                    }

                    try {
                        DiscoveryServiceBuilder<?> builder = service.getBuilderClass().getConstructor().newInstance();
                        if (verbose) builder.verbose();
                        for (Entry<Option, Method> entry : service.getArguments().entrySet()) {
                            Option key = entry.getKey();
                            if (cli.hasOption(key)) {
                                try {
                                    String[] opVals = cli.getOptionValues(key);
                                    if (opVals == null) {
                                        int numTimes = 0;
                                        for (Option op : cli.getOptions())
                                            if (op.getKey().equals(key.getKey())) numTimes++;
                                        if (numTimes > i) entry.getValue().invoke(builder);
                                        continue;
                                    }
                                    if (i >= opVals.length) continue;
                                    String rawVal = opVals[i];
                                    Object value;
                                    if (key.getConverter() != null) {
                                        value = ParamConverters.convert(key.getConverter(), rawVal);
                                    } else {
                                        value = rawVal;
                                    }
                                    entry.getValue().invoke(builder, value);
                                } catch (ParseException e) {
                                    System.err
                                            .println(String
                                                    .format("Invalid value \"%s\" for option %s",
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
                        stations.addAll(svc.discover());
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
        for (ReaderId reader : ReaderId.values())
            builder.append(" - " + reader.name().toLowerCase() + " - " + reader.getDescription() + "\n");
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
        for (Entry<String, ServiceEntry> entry : ServiceManager.getServices().entrySet()) {
            builder.append(String.format(" - %s - %s\n", entry.getKey(), entry.getValue().getDescription()));
        }
        return builder.toString();
    }

    private static void printHelp(String message) {
        new HelpFormatter()
                .printHelp(APP_NAME + " [options] [-F format] [-O filename]", null, rootOptions,
                        message == null
                                ? "\nAvailable services:\n" + createServicesString() + "\nAvailable outputs:\n"
                                        + createOutputsString() + "\nAvailable bookmark readers:\n"
                                        + createBookmarkReadersString()
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
        new HelpFormatter()
                .printHelp(128, APP_NAME + " -O " + (output == null ? "<format>" : output) + " [output]", null, ops,
                        footer);
    }

    private static void printServiceHelp(String service) {
        Options ops = new Options();
        String footer;
        if (service == null) {
            ops.addOptions(ServiceManager.getAllOptions());
            footer = "\nAvailable services:\n" + createServicesString();
        } else {
            footer = null;
            ServiceEntry svc = ServiceManager.getService(service);
            if (svc == null) {
                System.err.println("Service not found: " + service);
                return;
            }
            for (Option op : svc.getArguments().keySet()) ops.addOption(op);
        }
        new HelpFormatter()
                .printHelp(128, APP_NAME + " -S " + (service == null ? "<service>" : service) + " [options] [output]",
                        null, ops, footer);
    }
}
