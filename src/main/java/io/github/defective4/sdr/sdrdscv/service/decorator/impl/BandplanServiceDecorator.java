package io.github.defective4.sdr.sdrdscv.service.decorator.impl;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.defective4.sdr.sdrdscv.annotation.BuilderParam;
import io.github.defective4.sdr.sdrdscv.bandplan.Band;
import io.github.defective4.sdr.sdrdscv.bandplan.Bandplan;
import io.github.defective4.sdr.sdrdscv.bandplan.reader.BandplanReader;
import io.github.defective4.sdr.sdrdscv.bandplan.reader.GqrxBandplanReader;
import io.github.defective4.sdr.sdrdscv.bandplan.reader.JSONBandplanReader;
import io.github.defective4.sdr.sdrdscv.radio.RadioStation;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecorator;
import io.github.defective4.sdr.sdrdscv.service.decorator.ServiceDecoratorBuilder;

public class BandplanServiceDecorator implements ServiceDecorator {

    public static class Builder implements ServiceDecoratorBuilder<BandplanServiceDecorator> {

        private ReaderId readerId = ReaderId.JSON;
        private String source;

        @Override
        public BandplanServiceDecorator build() {
            return new BandplanServiceDecorator(readerId, source);
        }

        @BuilderParam(argName = "reader", defaultField = "readerId", description = "Bandplan reader. Valid values are:\n"
                + " - json - read bandplan from JSON file\n" + " - gqrx - read bandplan from gqrx's bandplan.csv file")
        public void setReaderId(ReaderId readerId) {
            this.readerId = readerId;
        }

        @BuilderParam(argName = "source", description = "File to read the bandplan from. If not specified, read the built-in bandplan")
        public void setSource(String source) {
            this.source = source;
        }

    }

    public static enum ReaderId {
        GQRX("Read a gqrx CSV bandplan file."), JSON("Read bandplan from a file in SDR-Discover's JSON format.");

        private final String description;

        private ReaderId(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

    }

    private final Bandplan bandplan;
    private final ReaderId readerId;
    private final boolean verbose = false;

    private BandplanServiceDecorator(ReaderId readerId, String source) {
        this.readerId = readerId;
        BandplanReader reader = switch (readerId) {
            case GQRX -> new GqrxBandplanReader();
            case JSON -> new JSONBandplanReader();
        };
        try (Reader fr = source == null
                ? new InputStreamReader(BandplanServiceDecorator.class.getResourceAsStream("/bandplan.json"))
                : new FileReader(source)) {
            bandplan = reader.read(fr, verbose);
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't read bandplan from the specified source!", e);
        }
    }

    @Override
    public List<RadioStation> decorate(List<RadioStation> stations) {
        List<RadioStation> decorated = new ArrayList<>();
        for (RadioStation station : stations) {
            float freq = station.getFrequency();
            Band matching = null;
            for (Band band : bandplan.getBands()) if (freq >= band.getStartFreq() && freq <= band.getEndFreq()) {
                matching = band;
                break;
            }
            if (matching != null) {
                decorated.add(
                        new RadioStation(station.getName(), freq, matching.getModulation(), station.getMetadata()));
            } else
                decorated.add(station);
        }
        return Collections.unmodifiableList(decorated);
    }

}
