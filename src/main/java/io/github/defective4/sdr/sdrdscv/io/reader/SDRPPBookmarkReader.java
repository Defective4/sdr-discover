package io.github.defective4.sdr.sdrdscv.io.reader;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation.Modulation;

public class SDRPPBookmarkReader extends BookmarkReader {

    private final List<String> listFilter;

    public SDRPPBookmarkReader(boolean lenient, boolean verbose, String[] listFilter) {
        super(lenient, verbose);
        this.listFilter = List.of(listFilter == null ? new String[0] : listFilter);
    }

    @Override
    public List<RadioStation> read(Reader reader) throws Exception {
        List<RadioStation> stations = new ArrayList<>();
        JsonObject lists = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("lists");
        System.err.println("Read " + lists.size() + " lists.");
        for (Entry<String, JsonElement> entry : lists.entrySet()) if (entry.getValue() instanceof JsonObject list) {
            String listName = entry.getKey();
            JsonObject bookmarks = list.getAsJsonObject("bookmarks");
            if (listFilter.isEmpty() || listFilter.contains(listName)) {
                System.err.println("List \"" + listName + "\" has " + bookmarks.size() + " bookmark elements.");
                for (Entry<String, JsonElement> subEntry : bookmarks.entrySet())
                    if (subEntry.getValue() instanceof JsonObject bookmark) {
                        String name = subEntry.getKey();
                        int bandwidth = (int) bookmark.get("bandwidth").getAsFloat();
                        float freq = bookmark.get("frequency").getAsFloat();
                        int mode = bookmark.get("mode").getAsInt();
                        Modulation modulation;
                        try {
                            modulation = Modulation.fromSdrppMod(mode);
                        } catch (Exception e) {
                            String msg = "Bookmark \"" + name + "\" uses an unknown modulation code: " + mode;
                            if (lenient) {
                                System.err.println(msg);
                                continue;
                            }
                            throw new IllegalArgumentException(msg);
                        }
                        RadioStation station = new RadioStation(name, freq, modulation,
                                Map
                                        .of(RadioStation.METADATA_BANDWIDTH, bandwidth, RadioStation.METADATA_TAGS,
                                                listName, RadioStation.METADATA_TAG_COLORS, "#ffffff"));
                        stations.add(station);
                    }
            }
        }
        return Collections.unmodifiableList(stations);
    }

}
