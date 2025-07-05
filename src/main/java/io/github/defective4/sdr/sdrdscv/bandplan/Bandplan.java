package io.github.defective4.sdr.sdrdscv.bandplan;

import java.util.Collections;
import java.util.List;

public class Bandplan {
    private final List<Band> bands;

    public Bandplan(List<Band> bands) {
        this.bands = Collections.unmodifiableList(bands);
    }

    public List<Band> getBands() {
        return bands;
    }

}
