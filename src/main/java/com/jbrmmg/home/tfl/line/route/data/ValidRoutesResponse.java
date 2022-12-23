package com.jbrmmg.home.tfl.line.route.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidRoutesResponse {
    public StopPointSequence[] stopPointSequences;
}
