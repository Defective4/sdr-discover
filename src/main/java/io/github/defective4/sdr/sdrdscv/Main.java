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
                        .desc("Add a discovery service")
                        .longOpt("add-service")
                        .valueSeparator(',')
                        .build())
                .addOption(Option.builder("h").desc("Show this help").longOpt("help").build())
                .addOption(Option
                        .builder("H")
                        .desc("Show help about a service, or don't provide an argument to show a list of services")
                        .longOpt("help-service")
                        .hasArg()
                        .argName("service")
                        .optionalArg(true)
                        .build())
                .addOption(Option.builder("v").desc("Be verbose").longOpt("verbose").build());
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
            new HelpFormatter().printHelp(APP_NAME, null, rootOptions, "\n" + e.getMessage());
            return;
        }
        if (cli.getOptions().length == 0 || cli.hasOption('h')) {
            printHelp(null);
            return;
        }
        if (cli.hasOption('H')) {
            String val = cli.getOptionValue('H');
            if (val == null) printServices();
            else printHelp(val);
            return;
        }

        boolean verbose = cli.hasOption('v');

        if (cli.hasOption('S')) {
            List<RadioStation> stations = new ArrayList<>();
            for (String serviceName : cli.getOptionValues('S')) {
                ServiceEntry service = ServiceManager.getServices().get(serviceName.toLowerCase());
                if (service == null) {
                    System.out.println("Service not found: " + serviceName);
                    printServices();
                    return;
                }

                try {
                    DiscoveryServiceBuilder builder = service.getBuilderClass().getConstructor().newInstance();
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

    private static void printHelp(String service) {
        Options ops;
        String cmdline = APP_NAME;
        String footer = null;
        String header = null;
        if (service == null) {
            ops = rootOptions;
        } else {
            ops = ServiceManager.makeOptionsFor(service);
            if (ops == null) {
                System.out.println("Service not found: " + service);
                printServices();
                return;
            }
            cmdline += " -A " + service + " <options>";
            header = "Valid options are:";
        }

        new HelpFormatter().printHelp(128, cmdline, header, ops, footer);
    }

    private static void printServices() {
        System.out.println("List of available services:");
        for (Entry<String, ServiceEntry> entry : ServiceManager.getServices().entrySet())
            System.out.println(" - " + entry.getKey() + " - " + entry.getValue().getDescription());
    }
}
