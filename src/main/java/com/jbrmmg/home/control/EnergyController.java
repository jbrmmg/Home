package com.jbrmmg.home.control;

import com.jbrmmg.home.octopus.OctopusManager;
import com.jbrmmg.home.octopus.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/energy")
@Tag(name = "Energy", description = "Octopus Energy gas and electricity consumption")
public class EnergyController {
    private static final Logger LOG = LoggerFactory.getLogger(EnergyController.class);

    public final OctopusManager octopusManager;

    @Autowired
    public EnergyController(OctopusManager octopusManager) {
        this.octopusManager = octopusManager;
    }

    @GetMapping("/gas")
    @Operation(summary = "Gas consumption", description = "Retrieve gas consumption data from Octopus Energy")
    public @ResponseBody Response gas(
            @Parameter(description = "Octopus API key", required = true) @RequestParam(value = "Key") String key,
            @Parameter(description = "Meter Point Reference Number", required = true) @RequestParam(value = "mprn") String mprn,
            @Parameter(description = "Meter serial number", required = true) @RequestParam(value = "serial") String serial) throws IOException {
        LOG.info("Get gas");
        return octopusManager.gas(key, mprn, serial);
    }

    @GetMapping("/electricity")
    @Operation(summary = "Electricity consumption", description = "Retrieve electricity consumption data from Octopus Energy")
    public @ResponseBody Response electricity(
            @Parameter(description = "Octopus API key", required = true) @RequestParam(value = "Key") String key,
            @Parameter(description = "Meter Point Administration Number", required = true) @RequestParam(value = "mpan") String mpan,
            @Parameter(description = "Meter serial number", required = true) @RequestParam(value = "serial") String serial) throws IOException {
        LOG.info("Get electricity");
        return octopusManager.electricity(key, mpan, serial);
    }
}
