package com.jbrmmg.home.quiz;

import java.util.List;

public class GuessBreakdown {
    private final String station;
    private final int stops;
    private final int zones;
    private final List<String> tfl;
    private final List<String> merged;

    public GuessBreakdown(String station, int stops, int zones, List<String> tfl, List<String> merged) {
        this.station = station;
        this.stops = stops;
        this.zones = zones;
        this.tfl = tfl;
        this.merged = merged;
    }

    public String getStation() { return station; }
    public int getStops() { return stops; }
    public int getZones() { return zones; }
    public List<String> getTfl() { return tfl; }
    public List<String> getMerged() { return merged; }
}
