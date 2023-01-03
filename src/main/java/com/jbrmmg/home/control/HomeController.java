package com.jbrmmg.home.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jbrmmg.home.dijkstras.StopCalculator;
import com.jbrmmg.home.octopus.OctopusManager;
import com.jbrmmg.home.octopus.Response;
import com.jbrmmg.home.tfl.TflManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

@RestController
@RequestMapping("/jbr/int/home")
public class HomeController {
    private static final Logger LOG = LoggerFactory.getLogger(HomeController.class);

    public final TflManager tflManager;

    public final OctopusManager octopusManager;
    public final StopCalculator stopCalculator;

    @Autowired
    public HomeController(TflManager tflManager, OctopusManager octopusManager, StopCalculator stopCalculator) {
        this.tflManager = tflManager;
        this.octopusManager = octopusManager;
        this.stopCalculator = stopCalculator;
    }

    @GetMapping(path="/routes")
    public @ResponseBody String getRoutes() {
        LOG.info("Get routes");

        tflManager.refreshLines();
        stopCalculator.calculateRoutes();

        return "OK";
    }

    @GetMapping(path="/query")
    public @ResponseBody String query(@RequestParam(value="station", required = false) String station,
                                      @RequestParam(value="stops", required = false) Integer stops,
                                      @RequestParam(value="zones", required = false) Integer zones ) {
        LOG.info("Get routes");

        return stopCalculator.find(station,stops,zones);
    }

    @GetMapping(path="/gas")
    public @ResponseBody Response gas(@RequestParam(value="Key") String key,
                                      @RequestParam(value="mprn") String mprn,
                                      @RequestParam(value="serial") String serial) throws IOException {
        return octopusManager.gas(key, mprn, serial);
    }

    @GetMapping(path="/electricity")
    public @ResponseBody Response electricity(@RequestParam(value="Key") String key,
                                      @RequestParam(value="mpan") String mpan,
                                      @RequestParam(value="serial") String serial) throws IOException {
        return octopusManager.electricity(key, mpan, serial);
    }
}
