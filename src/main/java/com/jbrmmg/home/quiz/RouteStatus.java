package com.jbrmmg.home.quiz;

public class RouteStatus {
    private final boolean tflReady;
    private final boolean mergedReady;

    public RouteStatus(boolean tflReady, boolean mergedReady) {
        this.tflReady = tflReady;
        this.mergedReady = mergedReady;
    }

    public boolean isTflReady() { return tflReady; }
    public boolean isMergedReady() { return mergedReady; }
    public boolean isReady() { return tflReady && mergedReady; }
}
