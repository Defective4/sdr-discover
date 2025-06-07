package io.github.defective4.sdr.sdrdscv.io.writer;

import java.io.Writer;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.defective4.sdr.sdrdscv.annotation.ConstructorParam;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class OpenWebRXBookmarkWriter implements BookmarkWriter {

    private final Gson gson;
    private final boolean includeDescription, scannable;

    public OpenWebRXBookmarkWriter(
            @ConstructorParam(argName = "include-description", defaultValue = "false", description = "If enabled, includes additional received info (for example radiotext) as stations' descriptions.") boolean includeDescription,
            @ConstructorParam(argName = "scannable", defaultValue = "false", description = "Marks all detected stations as scannable.") boolean scannable,
            @ConstructorParam(argName = "disable-pretty-print", defaultValue = "true", description = "Disables pretty printing of the resulting JSON, saving space.") boolean prettyPrint) {
        this.includeDescription = includeDescription;
        this.scannable = scannable;
        GsonBuilder builder = new GsonBuilder();
        if (prettyPrint) builder = builder.setPrettyPrinting();
        gson = builder.create();
    }

    @Override
    public void write(Writer output, List<RadioStation> stations) {
        JsonArray array = new JsonArray();
        for (RadioStation station : stations) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", station.getName());
            obj.addProperty("frequency", (int) station.getFrequency());
            obj.addProperty("modulation", station.getModulation().getOwrxMod());
            obj.addProperty("underlying", "");
            obj
                    .addProperty("description",
                            includeDescription
                                    ? station.getMetadataValue(RadioStation.METADATA_DESCRIPTION, String.class, "")
                                    : "");
            obj.addProperty("scannable", scannable);
            array.add(obj);
        }
        gson.toJson(array, output);
    }
}
