package io.github.defective4.sdr.sdrdscv;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.github.defective4.sdr.sdrdscv.io.writer.BookmarkWriterRegistry;
import io.github.defective4.sdr.sdrdscv.io.writer.BookmarkWriterRegistry.WriterEntry;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.service.ServiceEntry;
import io.github.defective4.sdr.sdrdscv.service.ServiceManager;
import io.github.defective4.sdr.sdrdscv.service.impl.DiscoveryServiceBuilder;

public class Main {
    public static final String APP_NAME = "sdr-discover";
    private static final Options rootOptions;

    static {
        rootOptions = new Options()
                .addOption(Option
                        .builder("S")
                        .argName("service")
                        .hasArg()
                        .desc("Add a discovery service.")
                        .longOpt("add-service")
                        .valueSeparator(',')
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

        CommandLineParser parser = new DefaultParser();

        CommandLine cli;
        try {
            cli = parser.parse(allOptions, args);
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
            List<RadioStation> stations = new ArrayList<>();
            for (String serviceName : cli.getOptionValues('S')) {
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
                                Object value = cli.getParsedOptionValue(key);
                                if (value == null) entry.getValue().invoke(builder);
                                else entry.getValue().invoke(builder, value);
                            } catch (ParseException e) {
                                System.err
                                        .println(String
                                                .format("Invalid value \"%s\" for option %s", cli.getOptionValue(key),
                                                        key.getKey()));
                            }
                        }
                    }
                    stations.addAll(builder.build().discover());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            System.out.println(stations);
        } else {
            System.err.println("You must add at least one service");
        }
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
                .printHelp(APP_NAME + " [options] [output]", null, rootOptions,
                        message == null
                                ? "\nAvailable services:\n" + createServicesString() + "\nAvailable outputs:\n"
                                        + createOutputsString()
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
