package com.jbrmmg.home.control;

import com.jbrmmg.home.dijkstras.StopCalculator;
import com.jbrmmg.home.tfl.TflManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jbr/int/home")
public class HomeController {
    private static final Logger LOG = LoggerFactory.getLogger(HomeController.class);

    public final TflManager tflManager;
    public final StopCalculator stopCalculator;

    @Autowired
    public HomeController(TflManager tflManager, StopCalculator stopCalculator) {
        this.tflManager = tflManager;
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
}
