package com.jbrmmg.home.tfl.line.route.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StopPointSequence {
    public StopPoint[] stopPoint;
}
