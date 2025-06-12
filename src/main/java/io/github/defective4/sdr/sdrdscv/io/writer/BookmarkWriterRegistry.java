package io.github.defective4.sdr.sdrdscv.io.writer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import io.github.defective4.sdr.sdrdscv.ParamConverters;
import io.github.defective4.sdr.sdrdscv.annotation.ConstructorParam;

public class BookmarkWriterRegistry {
    public static class WriterEntry {
        private final String description;
        private final Map<Parameter, ConstructorParam> params = new HashMap<>();
        private final Class<? extends BookmarkWriter> writerClass;

        public WriterEntry(Class<? extends BookmarkWriter> writerClass, String description) {
            this.writerClass = writerClass;
            this.description = description;
            Constructor<?> constructor = writerClass.getConstructors()[0];
            for (Parameter param : constructor.getParameters()) {
                if (!param.isAnnotationPresent(ConstructorParam.class)) throw new IllegalArgumentException(
                        writerClass + " does not have all constructor parameters annotated.");
                ConstructorParam wp = param.getAnnotation(ConstructorParam.class);
                params.put(param, wp);
            }
        }

        public String getDescription() {
            return description;
        }

        public Map<Parameter, ConstructorParam> getParams() {
            return Collections.unmodifiableMap(params);
        }

        public Class<? extends BookmarkWriter> getWriterClass() {
            return writerClass;
        }
    }

    private static final Map<String, WriterEntry> WRITERS = new HashMap<>();

    static {
        WRITERS.putAll(Map.of("csv", new WriterEntry(CSVBookmarkWriter.class, "Save stations to a CSV file."), "gqrx",
                new WriterEntry(GqrxBookmarkWriter.class, "GQRX compatible CSV bookmarks file."), "json",
                new WriterEntry(
                        JSONBookmarkWriter.class, "Files saved by this output can be loaded with json-input service."),
                "oneline",
                new WriterEntry(OneLineBookmarkWriter.class,
                        "Save stations to an user-readable text file, one line per station."),
                "openwebrx",
                new WriterEntry(OpenWebRXBookmarkWriter.class, "OpenWebRX compatible bookmarks JSON file."), "sdrpp",
                new WriterEntry(SDRPPBookmarkWriter.class,
                        "Outputs a JSON file compatible with SDR++'s frequency manager.")));
    }

    public static Options constructOptions() {
        Options ops = new Options();
        for (Entry<String, WriterEntry> entry : WRITERS.entrySet()) {
            ops.addOptions(constructOptions(entry.getKey(), entry.getValue()));
        }
        return ops;
    }

    public static Options constructOptions(String id, WriterEntry entry) {
        Options ops = new Options();
        for (Entry<Parameter, ConstructorParam> e : entry.getParams().entrySet()) {
            Parameter param = e.getKey();
            ConstructorParam wp = e.getValue();
            String def = wp.defaultValue();
            Option.Builder builder = Option.builder().longOpt(id.toLowerCase() + "-" + wp.argName());
            if (!def.isEmpty() && param.getType() != boolean.class) {
                builder.desc(wp.description() + " (Default: " + def + ")");
            } else {
                builder.desc(wp.description());
            }
            if (param.getType() != boolean.class) {
                builder.argName(param.getName()).hasArgs().converter(ParamConverters.getConverter(param.getType()));
            }
            ops.addOption(builder.build());
        }
        return ops;
    }

    public static WriterEntry getWriterForID(String id) {
        return WRITERS.get(id.toLowerCase());
    }

    public static Map<String, WriterEntry> getWriters() {
        return Collections.unmodifiableMap(WRITERS);
    }
}
