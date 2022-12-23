package com.jbrmmg.home.tfl.line.mode.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Line {
    public String id;
    public String name;
}
