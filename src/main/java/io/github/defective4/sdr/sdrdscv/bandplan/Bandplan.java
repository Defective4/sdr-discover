package io.github.defective4.sdr.sdrdscv.bandplan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Bandplan {
    private final List<Band> bands;

    public Bandplan(List<Band> bands) {
        List<Band> sorted = new ArrayList<>(bands);
        sorted.sort((b1, b2) -> (int) (b2.getStartFreq() - b1.getStartFreq()));
        this.bands = Collections.unmodifiableList(sorted);
    }

    public List<Band> getBands() {
        return bands;
    }

}
