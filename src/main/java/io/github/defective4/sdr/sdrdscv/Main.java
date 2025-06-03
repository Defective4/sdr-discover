package io.github.defective4.sdr.sdrdscv;

import java.util.Arrays;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.github.defective4.sdr.sdrdscv.module.Module;
import io.github.defective4.sdr.sdrdscv.module.ModuleManager;

public class Main {
    public static final String APP_NAME = "sdr-discover";
    private static final Options rootOptions;

    static {
        rootOptions = new Options()
                .addOption(Option
                        .builder("A")
                        .argName("module")
                        .hasArg()
                        .desc("Add a discovery module")
                        .longOpt("add-module")
                        .build())
                .addOption(Option.builder("h").desc("Show this help").longOpt("help").build())
                .addOption(Option
                        .builder("H")
                        .desc("Show help about a module, or don't provide an argument to show a list of modules")
                        .longOpt("help-module")
                        .hasArg()
                        .argName("module")
                        .optionalArg(true)
                        .build())
                .addOption(Option.builder("v").desc("Be verbose").longOpt("verbose").build());
    }

    public static void main(String[] a) throws ParseException {
        String[] args = {
                "-H"
        };

        CommandLineParser parser = new DefaultParser();

        CommandLine cli = parser.parse(rootOptions, args, true);
        if (cli.hasOption('h')) {
            printHelp(null);
            return;
        }
        if (cli.hasOption('H')) {
            String val = cli.getOptionValue('H');
            if (val == null) printModules();
            else printHelp(val);
            return;
        }

        if (cli.hasOption('A')) {
            System.out.println(Arrays.toString(cli.getOptionValues('A')));
        } else {
            System.err.println("You must add at least one module");
        }
    }

    private static void printHelp(String module) {
        Options ops;
        String cmdline = APP_NAME;
        String footer = null;
        String header = null;
        if (module == null) {
            ops = rootOptions;
        } else {
            ops = ModuleManager.makeOptionsFor(module);
            if (ops == null) {
                footer = "\nModule not found: " + module;
                ops = rootOptions;
            } else {
                cmdline += " -A " + module + " <options>";
                header = "Valid options are:";
            }
        }

        new HelpFormatter().printHelp(128, cmdline, header, ops, footer);
    }

    private static void printModules() {
        System.out.println("List of available modules:");
        for (Entry<String, Module> entry : ModuleManager.getModules().entrySet())
            System.out.println(" - " + entry.getKey() + " - " + entry.getValue().getDescription());
    }
}
