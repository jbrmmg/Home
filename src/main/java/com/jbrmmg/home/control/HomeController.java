package com.jbrmmg.home.control;

import com.jbrmmg.home.dijkstras.StopCalculator;
import com.jbrmmg.home.tfl.TflManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
}
