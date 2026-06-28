package com.jbrmmg.home.tfl.line.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbrmmg.home.tfl.line.route.data.ValidRoutesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class Route {
    private static Logger log = LoggerFactory.getLogger(Route.class);

    public static ValidRoutesResponse getRoutes(String lineId) {
        try {
            String url = "https://api.tfl.gov.uk/Line/" + lineId + "/Route/Sequence/inbound?serviceTypes=Regular";
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10000);
            factory.setReadTimeout(30000);
            RestTemplate restTemplate = new RestTemplate(factory);
            String result = restTemplate.getForObject(url, String.class);

            ObjectMapper objectMapper = new ObjectMapper();

            return objectMapper.readValue(result, ValidRoutesResponse.class);
        } catch (Exception e) {
            log.error("getRoutes failure ",e);
        }

        return null;
    }
}
