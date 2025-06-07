package io.github.defective4.sdr.sdrdscv.io.writer;

import java.io.Writer;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class SDRPPBookmarkWriter implements BookmarkWriter {

    private final Gson gson;
    private final String listName;

    public SDRPPBookmarkWriter(
            @WriterParam(argName = "list-name", defaultValue = "Discovered", description = "Sets name of the list containing detected stations.") String listName,
            @WriterParam(argName = "disable-pretty-print", defaultValue = "true", description = "Disables pretty printing of the resulting JSON, saving space.") boolean prettyPrint) {
        this.listName = listName;
        GsonBuilder builder = new GsonBuilder();
        if (prettyPrint) builder = builder.setPrettyPrinting();
        gson = builder.create();
    }

    @Override
    public void write(Writer output, List<RadioStation> stations) {
        JsonObject root = new JsonObject();
        JsonObject lists = new JsonObject();
        JsonObject list = new JsonObject();
        JsonObject bookmarks = new JsonObject();

        for (RadioStation station : stations) {
            JsonObject bookmark = new JsonObject();
            bookmark
                    .addProperty("bandwidth",
                            (float) station
                                    .getMetadataValue(RadioStation.METADATA_BANDWIDTH, Integer.class,
                                            (int) station.getModulation().getBandwidth()));
            bookmark.addProperty("frequency", station.getFrequency());
            bookmark.addProperty("mode", station.getModulation().getSdrppMod());
            bookmarks.add(station.getName(), bookmark);
        }

        list.addProperty("showOnWaterfall", true);
        list.add("bookmarks", bookmarks);

        lists.add(listName, list);

        root.addProperty("bookmarkDisplayMode", 1);
        root.addProperty("selectedList", listName);
        root.add("lists", lists);

        gson.toJson(root, output);
    }
}
