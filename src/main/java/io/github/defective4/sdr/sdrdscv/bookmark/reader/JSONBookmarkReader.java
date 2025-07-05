package io.github.defective4.sdr.sdrdscv.bookmark.reader;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import io.github.defective4.sdr.sdrdscv.radio.Modulation;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class JSONBookmarkReader extends BookmarkReader {

    public JSONBookmarkReader(boolean lenient, boolean verbose) {
        super(lenient, verbose);
    }

    @Override
    public List<RadioStation> read(Reader reader) throws Exception {
        List<RadioStation> stations = new ArrayList<>();
        JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
        if (verbose) {
            System.err.println("Parsed a JSON array with " + array.size() + " entries");
        }
        int skipped = 0;
        for (JsonElement element : array) if (element instanceof JsonObject child) {
            String name = null;
            try {
                float freq = child.get("frequency").getAsFloat();
                Modulation modulation = Modulation.valueOf(child.get("modulation").getAsString().toUpperCase());
                name = child.get("name").getAsString();
                JsonElement metaElement = child.get("metadata");
                Map<String, Object> metadata = new HashMap<>();
                if (metaElement instanceof JsonObject metaObj) {
                    for (Entry<String, JsonElement> entry : metaObj.entrySet())
                        if (entry.getValue() instanceof JsonPrimitive primitive) {
                            Object value;
                            if (primitive.isBoolean()) {
                                value = primitive.getAsBoolean();
                            } else if (primitive.isNumber()) {
                                value = primitive.getAsInt();
                            } else if (primitive.isString()) {
                                value = primitive.getAsString();
                            } else {
                                continue;
                            }
                            metadata.put(entry.getKey(), value);
                        }
                }
                stations.add(new RadioStation(name, freq, modulation, metadata));
            } catch (Exception e) {
                if (!lenient) throw e;
                skipped++;
                if (name != null && verbose) {
                    System.err.println("Stopped parsing station " + name + ", because if was malformed.");
                }
            }
        } else if (!isLenient())
            throw new IOException("Found an invalid element in JSON array");
        else {
            skipped++;
        }

        if (verbose) {
            System.err.println("JSON parsing finished. Found " + stations.size() + " sations.");
            if (skipped > 0) {
                System.err.println(skipped + " entries were skipped because they were malformed.");
            }
        }
        return Collections.unmodifiableList(stations);
    }

}
