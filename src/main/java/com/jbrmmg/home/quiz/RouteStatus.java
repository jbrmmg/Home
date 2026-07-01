package com.jbrmmg.home.quiz;

import java.time.Instant;

public class RouteStatus {
    private final boolean tflReady;
    private final boolean mergedReady;
    private final Instant lastUpdated;

    public RouteStatus(boolean tflReady, boolean mergedReady, Instant lastUpdated) {
        this.tflReady = tflReady;
        this.mergedReady = mergedReady;
        this.lastUpdated = lastUpdated;
    }

    public boolean isTflReady() { return tflReady; }
    public boolean isMergedReady() { return mergedReady; }
    public boolean isReady() { return tflReady && mergedReady; }
    public Instant getLastUpdated() { return lastUpdated; }
}
