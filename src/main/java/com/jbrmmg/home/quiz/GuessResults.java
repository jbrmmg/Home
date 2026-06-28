package com.jbrmmg.home.quiz;

import java.util.List;

public class GuessResults {
    private final List<String> tfl;
    private final List<String> merged;

    public GuessResults(List<String> tfl, List<String> merged) {
        this.tfl = tfl;
        this.merged = merged;
    }

    public List<String> getTfl() { return tfl; }
    public List<String> getMerged() { return merged; }
}
