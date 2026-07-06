package com.jbrmmg.home.quiz;

import java.time.Instant;

public class RouteStatus {
    private final boolean tflReady;
    private final boolean mergedReady;
    private final boolean calculating;
    private final Instant lastUpdated;

    public RouteStatus(boolean tflReady, boolean mergedReady, boolean calculating, Instant lastUpdated) {
        this.tflReady = tflReady;
        this.mergedReady = mergedReady;
        this.calculating = calculating;
        this.lastUpdated = lastUpdated;
    }

    public boolean isTflReady() { return tflReady; }
    public boolean isMergedReady() { return mergedReady; }
    public boolean isReady() { return tflReady && mergedReady; }
    public boolean isCalculating() { return calculating; }
    public Instant getLastUpdated() { return lastUpdated; }
}
