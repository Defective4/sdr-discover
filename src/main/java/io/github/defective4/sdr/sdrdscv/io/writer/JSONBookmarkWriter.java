package io.github.defective4.sdr.sdrdscv.io.writer;

import java.io.Writer;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

import io.github.defective4.sdr.sdrdscv.annotation.ConstructorParam;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;

public class JSONBookmarkWriter implements BookmarkWriter {

    private final Gson gson;

    public JSONBookmarkWriter(
            @ConstructorParam(argName = "disable-pretty-print", defaultValue = "true", description = "Disables pretty printing of the resulting JSON, saving space.") boolean prettyPrint) {
        GsonBuilder builder = new GsonBuilder();
        if (prettyPrint) {
            builder = builder.setPrettyPrinting();
        }
        gson = builder.create();
    }

    @Override
    public void write(Writer output, List<RadioStation> stations) {
        JsonArray array = new JsonArray();
        for (RadioStation station : stations) {
            array.add(gson.toJsonTree(station));
        }
        gson.toJson(array, output);
    }

}
