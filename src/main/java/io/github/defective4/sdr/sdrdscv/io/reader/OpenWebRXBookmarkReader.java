package io.github.defective4.sdr.sdrdscv.io.reader;

import static io.github.defective4.sdr.sdrdscv.radio.RadioStation.METADATA_CUSTOM_MODULATION;
import static io.github.defective4.sdr.sdrdscv.radio.RadioStation.METADATA_DESCRIPTION;
import static io.github.defective4.sdr.sdrdscv.radio.RadioStation.METADATA_SCANNABLE;
import static io.github.defective4.sdr.sdrdscv.radio.RadioStation.METADATA_SECONDARY_MODULATION;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.defective4.sdr.sdrdscv.radio.Modulation;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class OpenWebRXBookmarkReader extends BookmarkReader {

    private final boolean includeCustomModulation;

    public OpenWebRXBookmarkReader(boolean lenient, boolean verbose, boolean includeCustomModulation) {
        super(lenient, verbose);
        this.includeCustomModulation = includeCustomModulation;
    }

    @Override
    public List<RadioStation> read(Reader reader) throws Exception {
        JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
        if (verbose) {
            System.err.println("Loaded array with " + array.size() + " elements.");
        }
        List<RadioStation> stations = new ArrayList<>();
        for (JsonElement element : array) if (element instanceof JsonObject bookmark) {
            String name = bookmark.get("name").getAsString();
            float frequency = bookmark.get("frequency").getAsInt();
            String modName = bookmark.get("modulation").getAsString();
            Modulation mod = Modulation.fromOwrxMod(modName);
            if (mod == Modulation.CUSTOM && !includeCustomModulation) {
                if (verbose) {
                    System.err.println("Bookmark \"" + name + "\" was excluded, because it uses a custom modulation.");
                }
                continue;
            }
            boolean scannable;
            try {
                scannable = bookmark.get("scannable").getAsBoolean();
            } catch (Exception e) {
                scannable = false;
            }
            String underlying = null;
            try {
                underlying = bookmark.get("underlying").getAsString();
                if (underlying.isBlank()) {
                    underlying = null;
                }
            } catch (Exception e) {
            }

            String description = null;
            try {
                description = bookmark.get("description").getAsString();
                if (description.isBlank()) {
                    description = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Map<String, Object> metadata = new HashMap<>();
            if (scannable) {
                metadata.put(METADATA_SCANNABLE, true);
            }
            if (underlying != null) {
                metadata.put(METADATA_SECONDARY_MODULATION, underlying);
            }
            if (mod == Modulation.CUSTOM) {
                metadata.put(METADATA_CUSTOM_MODULATION, modName);
            }
            if (description != null) {
                metadata.put(METADATA_DESCRIPTION, description);
            }
            stations.add(new RadioStation(name, frequency, mod, metadata));
        }
        return Collections.unmodifiableList(stations);
    }

}
