package com.jbrmmg.home.tfl.line.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbrmmg.home.tfl.line.route.data.ValidRoutesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

public class Route {
    private static Logger log = LoggerFactory.getLogger(Route.class);

    public static ValidRoutesResponse getRoutes(String lineId) {
        try {
            String url = "https://api.tfl.gov.uk/Line/" + lineId + "/Route/Sequence/inbound?serviceTypes=Regular";
            RestTemplate restTemplate = new RestTemplate();
            String result = restTemplate.getForObject(url, String.class);

            ObjectMapper objectMapper = new ObjectMapper();

            return objectMapper.readValue(result, ValidRoutesResponse.class);
        } catch (Exception e) {
            log.error("getRoutes failure ",e);
        }

        return null;
    }
}
