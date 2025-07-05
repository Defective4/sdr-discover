package io.github.defective4.sdr.sdrdscv.bookmark.writer;

import static io.github.defective4.sdr.sdrdscv.radio.RadioStation.METADATA_CUSTOM_MODULATION;
import static io.github.defective4.sdr.sdrdscv.radio.RadioStation.METADATA_DESCRIPTION;
import static io.github.defective4.sdr.sdrdscv.radio.RadioStation.METADATA_SCANNABLE;
import static io.github.defective4.sdr.sdrdscv.radio.RadioStation.METADATA_SECONDARY_MODULATION;

import java.io.Writer;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.github.defective4.sdr.sdrdscv.annotation.ConstructorParam;
import io.github.defective4.sdr.sdrdscv.radio.Modulation;
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
        if (prettyPrint) {
            builder = builder.setPrettyPrinting();
        }
        gson = builder.create();
    }

    @Override
    public void write(Writer output, List<RadioStation> stations) {
        JsonArray array = new JsonArray();
        for (RadioStation station : stations) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", station.getName());
            obj.addProperty("frequency", (int) station.getFrequency());
            Modulation modulation = station.getModulation();
            obj.addProperty("modulation",
                    modulation == Modulation.CUSTOM && station.getMetadata().containsKey(METADATA_CUSTOM_MODULATION)
                            ? station.getMetadataValue(METADATA_CUSTOM_MODULATION, String.class,
                                    Modulation.RAW.getOwrxMod())
                            : modulation.getOwrxMod());
            obj.addProperty("underlying", station.getMetadataValue(METADATA_SECONDARY_MODULATION, String.class, ""));
            obj.addProperty("description",
                    includeDescription ? station.getMetadataValue(METADATA_DESCRIPTION, String.class, "") : "");
            obj.addProperty("scannable",
                    station.getMetadata().containsKey(METADATA_SCANNABLE)
                            ? station.getMetadataValue(METADATA_SCANNABLE, Boolean.class, false)
                            : scannable);
            array.add(obj);
        }
        gson.toJson(array, output);
    }
}
