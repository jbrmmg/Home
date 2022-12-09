package com.jbrmmg.home.tfl.line.mode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbrmmg.home.tfl.line.mode.data.Line;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

public class Mode {
    private static Logger log = LoggerFactory.getLogger(Mode.class);

    public static Line[] getLines() {
        try {
            String url = "https://api.tfl.gov.uk/Line/Mode/tube";
            RestTemplate restTemplate = new RestTemplate();
            String result = restTemplate.getForObject(url, String.class);

            ObjectMapper objectMapper = new ObjectMapper();

            return objectMapper.readValue(result, Line[].class);
        } catch (Exception e) {
            log.error("getRoutes failure ", e);
        }

        return null;
    }
}
