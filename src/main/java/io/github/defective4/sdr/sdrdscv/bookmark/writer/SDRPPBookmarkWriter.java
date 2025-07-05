package io.github.defective4.sdr.sdrdscv.bookmark.writer;

import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import io.github.defective4.sdr.sdrdscv.annotation.ConstructorParam;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class SDRPPBookmarkWriter implements BookmarkWriter {

    private final String defaultListName;
    private final Gson gson;
    private final boolean ignoreTags;

    public SDRPPBookmarkWriter(
            @ConstructorParam(argName = "default-list-name", defaultValue = "Discovered", description = "Sets name of the list for untagged stations.") String listName,
            @ConstructorParam(argName = "disable-pretty-print", defaultValue = "true", description = "Disables pretty printing of the resulting JSON, saving space.") boolean prettyPrint,
            @ConstructorParam(argName = "ignore-tags", defaultValue = "false", description = "Ignores station tags assigned by other services and adds all stations in the default list.") boolean ignoreTags) {
        defaultListName = listName;
        this.ignoreTags = ignoreTags;
        GsonBuilder builder = new GsonBuilder();
        if (prettyPrint) {
            builder = builder.setPrettyPrinting();
        }
        gson = builder.create();
    }

    @Override
    public void write(Writer output, List<RadioStation> stations) {
        JsonObject root = new JsonObject();
        JsonObject lists = new JsonObject();
        JsonObject defaultlist = new JsonObject();
        JsonObject bookmarks = new JsonObject();
        Map<String, JsonObject> tagBookmarks = new LinkedHashMap<>();

        for (RadioStation station : stations) {
            JsonObject bookmark = new JsonObject();
            bookmark.addProperty("bandwidth", (float) station.getMetadataValue(RadioStation.METADATA_BANDWIDTH,
                    Integer.class, (int) station.getModulation().getBandwidth()));
            bookmark.addProperty("frequency", station.getFrequency());
            bookmark.addProperty("mode", station.getModulation().getSdrppMod());
            bookmarks.add(station.getName(), bookmark);

            if (!ignoreTags && station.getMetadata().containsKey(RadioStation.METADATA_TAGS)) {
                String[] tags = station.getMetadataValue(RadioStation.METADATA_TAGS, String.class, "").split(",");
                for (String tag : tags) if (!tag.isBlank() && !tag.equals(defaultListName)) {
                    tagBookmarks.computeIfAbsent(tag, e -> new JsonObject()).add(station.getName(), bookmark);
                }
            }
        }

        defaultlist.addProperty("showOnWaterfall", true);
        defaultlist.add("bookmarks", bookmarks);

        lists.add(defaultListName, defaultlist);
        for (Entry<String, JsonObject> entry : tagBookmarks.entrySet()) {
            JsonObject list = new JsonObject();
            list.addProperty("showOnWaterfall", true);
            list.add("bookmarks", entry.getValue());
            lists.add(entry.getKey(), list);
        }

        root.addProperty("bookmarkDisplayMode", 1);
        root.addProperty("selectedList", defaultListName);
        root.add("lists", lists);

        gson.toJson(root, output);
    }
}
