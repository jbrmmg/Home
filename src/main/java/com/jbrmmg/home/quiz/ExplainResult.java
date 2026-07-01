package com.jbrmmg.home.quiz;

import java.util.List;

public class ExplainResult {
    private String from;
    private String to;
    private PathDetail tfl;
    private PathDetail merged;

    public ExplainResult(String from, String to, PathDetail tfl, PathDetail merged) {
        this.from = from;
        this.to = to;
        this.tfl = tfl;
        this.merged = merged;
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }
    public PathDetail getTfl() { return tfl; }
    public PathDetail getMerged() { return merged; }

    public static class PathDetail {
        private int stops;
        private int zones;
        private List<String> path;

        public PathDetail(int stops, int zones, List<String> path) {
            this.stops = stops;
            this.zones = zones;
            this.path = path;
        }

        public int getStops() { return stops; }
        public int getZones() { return zones; }
        public List<String> getPath() { return path; }
    }
}
