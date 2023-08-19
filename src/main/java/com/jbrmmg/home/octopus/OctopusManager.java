package com.jbrmmg.home.octopus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbrmmg.home.control.HomeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;

@Component
public class OctopusManager {
    private static final Logger LOG = LoggerFactory.getLogger(OctopusManager.class);

    private static class BaseOctopusUrl {
        private final String urlText;

        protected BaseOctopusUrl(String base, String id, String serial) {
            this.urlText = "https://api.octopus.energy/v1/" + base + "/" + id + "/meters/" + serial + "/consumption/";
            // /consumption/?period_from=yyyy-mm-ddThh:mmZ
            LOG.info(urlText);
        }
        public URL url() throws MalformedURLException {
            return new URL(urlText);
        }
    }

    private static class GasUrl extends BaseOctopusUrl {
        public GasUrl(String mprn, String serial) {
            super("gas-meter-points", mprn, serial);
        }
    }

    private static class ElectricityUrl extends BaseOctopusUrl {
        public ElectricityUrl(String mpan, String serial) {
            super("electricity-meter-points", mpan, serial);
        }
    }

    private Response callApi(String key, BaseOctopusUrl url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.url().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        String auth = key + ":";
        String authHeaderValue = Base64.getEncoder().encodeToString(auth.getBytes());
        connection.setRequestProperty("Authorization", "Basic " + authHeaderValue);

        if(connection.getResponseCode() != 200) {
            LOG.info("Failed " + connection.getResponseCode());

            throw new RuntimeException("Failed: HTTP Error Code: " + connection.getResponseCode());
        }

        InputStreamReader in = new InputStreamReader(connection.getInputStream());
        BufferedReader br = new BufferedReader(in);

        StringBuilder result = new StringBuilder();
        String output;
        while((output = br.readLine()) != null) {
            result.append(output);
        }
        connection.disconnect();

        LOG.info(result.toString());

        Response resultResponse = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            resultResponse = mapper.readValue(result.toString(), Response.class);
        } catch(Exception e) {
            LOG.error("Error:",e);
        }

        return resultResponse;
    }

    public Response gas(String key, String mprn, String serial) throws IOException {
        return callApi(key, new GasUrl(mprn,serial));
    }

    public Response electricity(String key, String mpan, String serial) throws IOException {
        return callApi(key, new ElectricityUrl(mpan,serial));
    }
}
