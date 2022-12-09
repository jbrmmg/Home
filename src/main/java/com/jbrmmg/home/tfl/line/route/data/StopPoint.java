package com.jbrmmg.home.tfl.line.route.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StopPoint {
    public String stationId;
    public String name;
    public String zone;
}
