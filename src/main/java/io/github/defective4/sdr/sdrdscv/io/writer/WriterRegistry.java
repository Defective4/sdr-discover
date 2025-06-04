package io.github.defective4.sdr.sdrdscv.io.writer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WriterRegistry {
    public static class WriterEntry {
        private final String description;
        private final Class<? extends BookmarkWriter> writerClass;

        public WriterEntry(Class<? extends BookmarkWriter> writerClass, String description) {
            this.writerClass = writerClass;
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public Class<? extends BookmarkWriter> getWriterClass() {
            return writerClass;
        }
    }

    private static final Map<String, WriterEntry> WRITERS = new HashMap<>();

    static {
        WRITERS
                .putAll(Map
                        .of("csv", new WriterEntry(CSVBookmarkWriter.class, "Save stations to a CSV file."), "gqrx",
                                new WriterEntry(GqrxBookmarkWriter.class, "GQRX compatible CSV bookmarks file."),
                                "json-output",
                                new WriterEntry(JSONBookmarkWriter.class,
                                        "Files saved by this output can be loaded with json-input service."),
                                "oneline",
                                new WriterEntry(OneLineBookmarkWriter.class,
                                        "Save stations to an user-readable text file, one line per station."),
                                "openwebrx",
                                new WriterEntry(OpenWebRXBookmarkWriter.class,
                                        "OpenWebRX compatible bookmarks JSON file."),
                                "sdrpp", new WriterEntry(SDRPPBookmarkWriter.class,
                                        "Outputs a JSON file compatible with SDR++'s frequency manager.")));
    }

    public static WriterEntry getWriterForID(String id) {
        return WRITERS.get(id);
    }

    public static Map<String, WriterEntry> getWriters() {
        return Collections.unmodifiableMap(WRITERS);
    }
}
