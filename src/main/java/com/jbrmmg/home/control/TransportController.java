package com.jbrmmg.home.control;

import com.jbrmmg.home.data.StationRepository;
import com.jbrmmg.home.data.entity.Station;
import com.jbrmmg.home.dijkstras.StopCalculator;
import com.jbrmmg.home.quiz.Guess;
import com.jbrmmg.home.quiz.GuessBreakdown;
import com.jbrmmg.home.quiz.GuessResults;
import com.jbrmmg.home.quiz.GuessStore;
import com.jbrmmg.home.quiz.RouteStatus;
import com.jbrmmg.home.tfl.TflManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transport")
@Tag(name = "Transport", description = "TFL route and stop queries")
public class TransportController {
    private static final Logger LOG = LoggerFactory.getLogger(TransportController.class);

    private final TflManager tflManager;
    private final StopCalculator stopCalculator;
    private final GuessStore guessStore;
    private final StationRepository stationRepository;

    @Autowired
    public TransportController(TflManager tflManager, StopCalculator stopCalculator,
                               GuessStore guessStore, StationRepository stationRepository) {
        this.tflManager = tflManager;
        this.stopCalculator = stopCalculator;
        this.guessStore = guessStore;
        this.stationRepository = stationRepository;
    }

    @GetMapping("/routes")
    @Operation(summary = "Refresh TFL routes", description = "Fetches TFL line data then calculates routes in the background — poll /routes/status to check progress")
    public @ResponseBody String getRoutes() {
        LOG.info("Get routes");
        tflManager.refreshLines();
        stopCalculator.calculateRoutes();
        stopCalculator.calculateMergedRoutes();
        return "Route calculation started";
    }

    @GetMapping("/routes/status")
    @Operation(summary = "Route calculation status", description = "Check whether the TFL and merged route calculations have completed")
    public @ResponseBody RouteStatus getRoutesStatus() {
        return stopCalculator.getStatus();
    }

    @GetMapping("/stations")
    @Operation(summary = "List stations", description = "Returns all known station names sorted alphabetically")
    public @ResponseBody List<String> getStations() {
        return StreamSupport.stream(stationRepository.findAll().spliterator(), false)
                .map(Station::getName)
                .sorted()
                .collect(Collectors.toList());
    }

    @GetMapping("/stops")
    @Operation(summary = "Query stops (TFL view)", description = "Find stops reachable from a given station within optional stop or zone limits")
    public @ResponseBody String getStops(
            @Parameter(description = "Starting station name") @RequestParam(value = "station", required = false) String station,
            @Parameter(description = "Maximum number of stops") @RequestParam(value = "stops", required = false) Integer stops,
            @Parameter(description = "Maximum number of zones") @RequestParam(value = "zones", required = false) Integer zones) {
        LOG.info("Get stops");
        return stopCalculator.find(station, stops, zones);
    }

    @PostMapping("/guess")
    @Operation(summary = "Add a guess", description = "Record a station guess with the stop and zone clue returned by the quiz")
    public ResponseEntity<String> addGuess(@RequestBody Guess guess) {
        if (!stopCalculator.stationExists(guess.getStation())) {
            return ResponseEntity.badRequest().body("Unknown station: " + guess.getStation());
        }
        LOG.info("Add guess: {}", guess.getStation());
        guessStore.add(guess);
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/guess")
    @Operation(summary = "Get guesses", description = "View all recorded guesses for the current session")
    public @ResponseBody List<Guess> getGuesses() {
        return guessStore.getAll();
    }

    @DeleteMapping("/guess")
    @Operation(summary = "Reset guesses", description = "Clear all recorded guesses and start a new session")
    public @ResponseBody String resetGuesses() {
        LOG.info("Reset guesses");
        guessStore.reset();
        return "OK";
    }

    @GetMapping("/guess/results")
    @Operation(summary = "Get results", description = "Find stations satisfying all recorded guesses, shown in both TFL and merged (quiz) views")
    public @ResponseBody GuessResults getResults() {
        return stopCalculator.findResults(guessStore.getAll());
    }

    @GetMapping("/guess/breakdown")
    @Operation(summary = "Per-guess breakdown", description = "Returns the individual matches for each guess before intersection")
    public @ResponseBody List<GuessBreakdown> getBreakdown() {
        return stopCalculator.findBreakdown(guessStore.getAll());
    }
}
