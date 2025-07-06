package io.github.defective4.sdr.sdrdscv.bandplan.reader;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.defective4.sdr.sdrdscv.bandplan.Band;
import io.github.defective4.sdr.sdrdscv.bandplan.Bandplan;
import io.github.defective4.sdr.sdrdscv.radio.Modulation;

public class JSONBandplanReader implements BandplanReader {

    @Override
    public Bandplan read(Reader reader, boolean verbose) throws IOException {
        List<Band> bands = new ArrayList<>();
        try {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("bands")) if (element instanceof JsonObject obj) {
                String name = obj.get("name").getAsString();
                float start = obj.get("start").getAsFloat();
                float end = obj.get("end").getAsFloat();
                Modulation modulation = Modulation.valueOf(obj.get("modulation").getAsString().toUpperCase());
                bands.add(new Band(name, start, end, modulation));
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
        return new Bandplan(bands);
    }
}
