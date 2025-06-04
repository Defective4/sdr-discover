package io.github.defective4.sdr.sdrdscv.service.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation.Modulation;
import io.github.defective4.sdr.sdrdscv.service.ServiceArgument;

public class JSONDiscoveryService implements DiscoveryService {

    public static class Builder extends DiscoveryServiceBuilder<JSONDiscoveryService> {

        private boolean dropInvalidEntries = false;
        private String source = "-";

        @Override
        public JSONDiscoveryService build() {
            return new JSONDiscoveryService(source, dropInvalidEntries, verbose);
        }

        @ServiceArgument(defaultField = "dropInvalidEntries", argName = "drop-invalid-entries", description = "If enabled, invalid entries in the JSON file will be ignored instead of throwing an error")
        public Builder dropInvalidEntries() {
            dropInvalidEntries = true;
            return this;
        }

        @ServiceArgument(defaultField = "source", argName = "source", description = "Source of JSON data. Can either point to a file, or be \"-\" to read from standard input")
        public Builder withSource(String source) {
            this.source = source;
            return this;
        }

    }

    private final boolean dropInvalidEntries;
    private final String source;
    private final boolean verbose;

    private JSONDiscoveryService(String source, boolean dropInvalidEntries, boolean verbose) {
        this.source = source;
        this.dropInvalidEntries = dropInvalidEntries;
        this.verbose = verbose;
    }

    @Override
    public List<RadioStation> discover() throws Exception {
        List<RadioStation> stations = new ArrayList<>();
        Reader reader = null;
        try {
            reader = new InputStreamReader("-".equals(source) ? System.in : Files.newInputStream(Path.of(source)));
            JsonElement el = JsonParser.parseReader(reader);
            if (!(el instanceof JsonArray array)) throw new IOException("Root element is not an array");
            if (verbose) System.err.println("The array contains " + array.size() + " elements");
            for (JsonElement child : array) {
                if (child instanceof JsonObject obj) {
                    try {
                        String name = obj.get("name").getAsString();
                        Modulation mod = Modulation.valueOf(obj.get("modulation").getAsString().toUpperCase());
                        String description = obj.has("description") ? obj.get("description").getAsString() : null;
                        float frequency = obj.get("frequency").getAsFloat();
                        stations.add(new RadioStation(name, description, frequency, mod));
                    } catch (Exception e) {
                        if (!dropInvalidEntries) throw e;
                        System.err.println("Dropped an entry: " + e.getMessage());
                    }
                } else if (!dropInvalidEntries) throw new IOException("Found an invalid object in the array");
                else System.err.println("Dropped an entry, because it's not a valid object");
            }
        } finally {
            if (reader != null && !"-".equals(source)) reader.close();
        }
        return Collections.unmodifiableList(stations);
    }

}
