package io.github.defective4.sdr.sdrdscv.service.impl;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.github.defective4.sdr.sdrdscv.io.reader.BookmarkReader;
import io.github.defective4.sdr.sdrdscv.io.reader.GqrxBookmarkReader;
import io.github.defective4.sdr.sdrdscv.io.reader.JSONBookmarkReader;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.service.ServiceArgument;

public class BookmarksDiscoveryService implements DiscoveryService {

    public static class Builder extends DiscoveryServiceBuilder<BookmarksDiscoveryService> {

        private char gqrxSeparatorChar = ';';
        private boolean lenient;
        private String readerId;
        private String source = "-";

        @Override
        public BookmarksDiscoveryService build() {
            return new BookmarksDiscoveryService(lenient, readerId, source, verbose, gqrxSeparatorChar);
        }

        @ServiceArgument(argName = "lenient", description = "If enabled, invalid/malformed bookmark entries will be ignored instead of throwing an error")
        public Builder lenient() {
            lenient = true;
            return this;
        }

        @ServiceArgument(argName = "gqrx-separator", description = "Use a non-standard CSV separator character for gqrx bookmarks", defaultField = "gqrxSeparatorChar")
        public Builder withGqrxSeparatorChar(char gqrxSeparatorChar) {
            this.gqrxSeparatorChar = gqrxSeparatorChar;
            return this;
        }

        @ServiceArgument(argName = "reader", description = "Bookmark reader to use")
        public Builder withReaderId(String readerId) {
            this.readerId = readerId;
            return this;
        }

        @ServiceArgument(argName = "source", description = "Bookmark file source. Use \"-\" for standard input.", defaultField = "source")
        public Builder withSource(String source) {
            this.source = source;
            return this;
        }

    }

    public static enum ReaderId {
        GQRX("Read bookmarks from gqrx's CSV file"),
        JSON("Read stations stored in a JSON file output by the \"json\" writer.");

        private final String description;

        private ReaderId(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

    }

    private final char gqrxSeparatorChar;
    private final boolean lenient, verbose;
    private final ReaderId readerId;

    private final String source;

    private BookmarksDiscoveryService(boolean lenient, String readerId, String source, boolean verbose,
            char gqrxSeparatorChar) {
        if (readerId == null) throw new IllegalArgumentException("Option \"bookmarks-reader\" is required.");
        try {
            this.readerId = ReaderId.valueOf(readerId.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Reader not found: " + readerId);
        }
        this.gqrxSeparatorChar = gqrxSeparatorChar;
        this.lenient = lenient;
        this.verbose = verbose;
        this.source = source;
    }

    @Override
    public List<RadioStation> discover() throws Exception {
        Reader reader = null;
        try {
            reader = "-".equals(source) ? new InputStreamReader(System.in)
                    : new FileReader(source, StandardCharsets.UTF_8);
            BookmarkReader br = switch (readerId) {
                case JSON -> new JSONBookmarkReader(lenient, verbose);
                case GQRX -> new GqrxBookmarkReader(lenient, verbose, gqrxSeparatorChar);
            };
            return br.read(reader);
        } finally {
            if (reader != null && !"-".equals(source)) reader.close();
        }
    }

}
