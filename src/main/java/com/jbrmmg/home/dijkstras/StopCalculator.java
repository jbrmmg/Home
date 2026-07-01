package com.jbrmmg.home.dijkstras;

import com.jbrmmg.home.config.ApplicationProperties;
import com.jbrmmg.home.data.ConnectionRepository;
import com.jbrmmg.home.data.MergedRouteRepository;
import com.jbrmmg.home.data.RouteRepository;
import com.jbrmmg.home.data.SettingRepository;
import com.jbrmmg.home.data.StationRepository;
import com.jbrmmg.home.data.entity.Setting;
import com.jbrmmg.home.data.entity.Connection;
import com.jbrmmg.home.data.entity.MergedRoute;
import com.jbrmmg.home.data.entity.Route;
import com.jbrmmg.home.data.entity.Station;
import com.jbrmmg.home.dijkstras.graph.Graph;
import com.jbrmmg.home.dijkstras.graph.Node;
import com.jbrmmg.home.quiz.ExplainResult;
import com.jbrmmg.home.quiz.Guess;
import com.jbrmmg.home.quiz.GuessBreakdown;
import com.jbrmmg.home.quiz.GuessResults;
import com.jbrmmg.home.quiz.RouteStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class StopCalculator {
    private static final Logger log = LoggerFactory.getLogger(StopCalculator.class);

    private final AtomicBoolean tflReady = new AtomicBoolean(false);
    private final AtomicBoolean mergedReady = new AtomicBoolean(false);
    private volatile Instant tflLastUpdated = null;

    private static final String SETTING_TFL_LAST_UPDATED = "tfl.lastUpdated";

    private final StationRepository stationRepository;
    private final ConnectionRepository connectionRepository;
    private final RouteRepository routeRepository;
    private final MergedRouteRepository mergedRouteRepository;
    private final SettingRepository settingRepository;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public StopCalculator(StationRepository stationRepository,
                          ConnectionRepository connectionRepository,
                          RouteRepository routeRepository,
                          MergedRouteRepository mergedRouteRepository,
                          SettingRepository settingRepository,
                          ApplicationProperties applicationProperties) {
        this.stationRepository = stationRepository;
        this.connectionRepository = connectionRepository;
        this.routeRepository = routeRepository;
        this.mergedRouteRepository = mergedRouteRepository;
        this.settingRepository = settingRepository;
        this.applicationProperties = applicationProperties;
    }

    public Graph calculateShortest(Map<String, Station> stations, List<Connection> connections, Station source) {
        Map<String, Node> nodeMap = new HashMap<>();
        Node sourceNode = null;
        for (Station station : stations.values()) {
            nodeMap.put(station.getId(), new Node(station.getId()));
            if (source.getId().equals(station.getId())) {
                sourceNode = nodeMap.get(source.getId());
            }
        }

        for (Connection connection : connections) {
            String[] keys = connection.getId().split("-");
            Node node1 = nodeMap.get(keys[0]);
            Node node2 = nodeMap.get(keys[1]);
            node1.addDestination(node2, 1);
            node2.addDestination(node1, 1);
        }

        Graph result = new Graph();
        for (Map.Entry<String, Node> nextNode : nodeMap.entrySet()) {
            result.addNode(nextNode.getValue());
        }

        return Graph.calculateShortestPathFromSource(result, sourceNode);
    }

    @PostConstruct
    public void init() {
        tflReady.set(routeRepository.count() > 0);
        mergedReady.set(mergedRouteRepository.count() > 0);
        settingRepository.findById(SETTING_TFL_LAST_UPDATED)
                .ifPresent(s -> tflLastUpdated = Instant.parse(s.getValue()));
        log.info("Route status on startup — TFL: {}, merged: {}, lastUpdated: {}", tflReady.get(), mergedReady.get(), tflLastUpdated);
    }

    public RouteStatus getStatus() {
        return new RouteStatus(tflReady.get(), mergedReady.get(), tflLastUpdated);
    }

    @Async
    public void calculateRoutes() {
        tflReady.set(false);
        routeRepository.deleteAll();

        Map<String, Station> stations = new HashMap<>();
        for (Station station : stationRepository.findAll()) {
            stations.put(station.getId(), station);
        }

        List<Connection> connections = new ArrayList<>(connectionRepository.findAll());
        Map<String, Route> routes = new HashMap<>();

        int count = 1;
        for (Station station : stations.values()) {
            log.info("Calculate shortest for {} {} {}", count, stations.size(), station.getName());

            Graph shortest = calculateShortest(stations, connections, station);

            for (Node nextNode : shortest.getNodes()) {
                String id = Station.getConnectionId(nextNode.getName(), station.getId());

                if (!routes.containsKey(id)) {
                    int stops = nextNode.getDistance();
                    Station other = stations.get(nextNode.getName());
                    String[] idParts = id.split("-");

                    Route newRoute = new Route();
                    newRoute.setId(id);
                    newRoute.setStops(stops);
                    newRoute.setZones(Station.zoneDifference(station, other));
                    // station1 always corresponds to ids[0], station2 to ids[1]
                    if (idParts[0].equals(station.getId())) {
                        newRoute.setStation1(station.getName());
                        newRoute.setStation2(other.getName());
                    } else {
                        newRoute.setStation1(other.getName());
                        newRoute.setStation2(station.getName());
                    }

                    routes.put(id, newRoute);
                }
            }

            count++;
        }

        routeRepository.saveAll(routes.values());
        tflLastUpdated = Instant.now();
        Setting setting = new Setting();
        setting.setId(SETTING_TFL_LAST_UPDATED);
        setting.setValue(tflLastUpdated.toString());
        settingRepository.save(setting);
        tflReady.set(true);
        log.info("Done TFL routes");
    }

    @Async
    public void calculateMergedRoutes() {
        mergedReady.set(false);
        mergedRouteRepository.deleteAll();

        Map<String, Station> stations = new HashMap<>();
        for (Station station : stationRepository.findAll()) {
            stations.put(station.getId(), station);
        }

        Map<String, String> mergeMap = buildMergeMap(stations);

        // Build merged station set (canonical stations only)
        Map<String, Station> mergedStations = new HashMap<>();
        for (Map.Entry<String, String> entry : mergeMap.entrySet()) {
            String canonicalId = entry.getValue();
            mergedStations.putIfAbsent(canonicalId, stations.get(canonicalId));
        }

        // Remap connections to canonical IDs, dropping self-connections and duplicates
        List<Connection> mergedConnections = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (Connection c : connectionRepository.findAll()) {
            String s1 = mergeMap.get(c.getStation1Id());
            String s2 = mergeMap.get(c.getStation2Id());
            if (s1 == null || s2 == null || s1.equals(s2)) continue;

            String newId = Station.getConnectionId(s1, s2);
            if (seenIds.add(newId)) {
                Connection newConn = new Connection();
                newConn.setId(newId);
                newConn.setStation1Id(s1);
                newConn.setStation2Id(s2);
                mergedConnections.add(newConn);
            }
        }

        Map<String, MergedRoute> routes = new HashMap<>();
        int count = 1;
        for (Station station : mergedStations.values()) {
            log.info("Calculate merged shortest for {} {} {}", count, mergedStations.size(), station.getName());

            Graph shortest = calculateShortest(mergedStations, mergedConnections, station);

            for (Node nextNode : shortest.getNodes()) {
                String id = Station.getConnectionId(nextNode.getName(), station.getId());

                if (!routes.containsKey(id)) {
                    Station other = mergedStations.get(nextNode.getName());
                    String[] idParts = id.split("-");

                    MergedRoute route = new MergedRoute();
                    route.setId(id);
                    route.setStops(nextNode.getDistance());
                    route.setZones(Station.zoneDifference(station, other));
                    // station1 always corresponds to ids[0], station2 to ids[1]
                    if (idParts[0].equals(station.getId())) {
                        route.setStation1(station.getName());
                        route.setStation2(other.getName());
                    } else {
                        route.setStation1(other.getName());
                        route.setStation2(station.getName());
                    }

                    routes.put(id, route);
                }
            }

            count++;
        }

        mergedRouteRepository.saveAll(routes.values());
        mergedReady.set(true);
        log.info("Done merged routes");
    }

    public GuessResults findResults(List<Guess> guesses) {
        if (guesses.isEmpty()) {
            return new GuessResults(Collections.emptyList(), Collections.emptyList());
        }

        Map<String, Station> stations = new HashMap<>();
        for (Station s : stationRepository.findAll()) {
            stations.put(s.getId(), s);
        }
        Map<String, String> mergeMap = buildMergeMap(stations);

        List<Route> allTflRoutes = routeRepository.findAll();
        List<MergedRoute> allMergedRoutes = mergedRouteRepository.findAll();

        Set<String> tflMatches = null;
        Set<String> mergedMatches = null;

        for (Guess guess : guesses) {
            Set<String> tfl = findMatchingTfl(guess, stations, allTflRoutes);
            Set<String> merged = findMatchingMerged(guess, stations, mergeMap, allMergedRoutes);

            if (tflMatches == null) {
                tflMatches = tfl;
                mergedMatches = merged;
            } else {
                tflMatches.retainAll(tfl);
                mergedMatches.retainAll(merged);
            }
        }

        List<String> tflList = new ArrayList<>(tflMatches);
        List<String> mergedList = new ArrayList<>(mergedMatches);
        Collections.sort(tflList);
        Collections.sort(mergedList);

        return new GuessResults(tflList, mergedList);
    }

    public boolean stationExists(String name) {
        for (Station s : stationRepository.findAll()) {
            if (s.getName().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    public List<GuessBreakdown> findBreakdown(List<Guess> guesses) {
        Map<String, Station> stations = new HashMap<>();
        for (Station s : stationRepository.findAll()) {
            stations.put(s.getId(), s);
        }
        Map<String, String> mergeMap = buildMergeMap(stations);
        List<Route> allTflRoutes = routeRepository.findAll();
        List<MergedRoute> allMergedRoutes = mergedRouteRepository.findAll();

        List<GuessBreakdown> result = new ArrayList<>();
        for (Guess guess : guesses) {
            List<String> tfl = new ArrayList<>(findMatchingTfl(guess, stations, allTflRoutes));
            List<String> merged = new ArrayList<>(findMatchingMerged(guess, stations, mergeMap, allMergedRoutes));
            Collections.sort(tfl);
            Collections.sort(merged);
            result.add(new GuessBreakdown(guess.getStation(), guess.getStops(), guess.getZones(), tfl, merged));
        }
        return result;
    }

    private Set<String> findMatchingTfl(Guess guess, Map<String, Station> stations, List<Route> routes) {
        Set<String> result = new HashSet<>();

        for (Station station : stations.values()) {
            if (!station.getName().equalsIgnoreCase(guess.getStation())) continue;

            for (Route route : routes) {
                if (!route.getId().contains(station.getId())) continue;
                if (!route.getStops().equals(guess.getStops())) continue;
                if (!zonesMatch(route.getZones(), guess.getZones())) continue;

                String[] ids = route.getId().split("-");
                result.add(ids[0].equals(station.getId()) ? route.getStation2() : route.getStation1());
            }
        }

        return result;
    }

    private Set<String> findMatchingMerged(Guess guess, Map<String, Station> stations,
                                            Map<String, String> mergeMap, List<MergedRoute> routes) {
        Set<String> result = new HashSet<>();

        // Find canonical IDs for any station matching the guess name
        Set<String> canonicalIds = new HashSet<>();
        for (Station station : stations.values()) {
            if (station.getName().equalsIgnoreCase(guess.getStation())) {
                canonicalIds.add(mergeMap.get(station.getId()));
            }
        }

        for (String canonicalId : canonicalIds) {
            for (MergedRoute route : routes) {
                if (!route.getId().contains(canonicalId)) continue;
                if (!route.getStops().equals(guess.getStops())) continue;
                if (!zonesMatch(route.getZones(), guess.getZones())) continue;

                String[] ids = route.getId().split("-");
                result.add(ids[0].equals(canonicalId) ? route.getStation2() : route.getStation1());
            }
        }

        return result;
    }

    private boolean zonesMatch(int routeZones, int guessZones) {
        return guessZones <= 2 ? routeZones == guessZones : routeZones > 2;
    }

    private Map<String, String> buildMergeMap(Map<String, Station> stations) {
        Map<String, String> mergeMap = new HashMap<>();
        for (String id : stations.keySet()) {
            mergeMap.put(id, id);
        }

        List<List<String>> merges = applicationProperties.getStationMerges();
        if (merges == null) return mergeMap;

        for (List<String> group : merges) {
            String canonicalId = findStationIdByName(group.get(0), stations);
            if (canonicalId == null) {
                log.warn("Merge canonical station not found: {}", group.get(0));
                continue;
            }
            for (String name : group) {
                String id = findStationIdByName(name, stations);
                if (id == null) {
                    log.warn("Merge station not found: {}", name);
                    continue;
                }
                mergeMap.put(id, canonicalId);
            }
        }

        return mergeMap;
    }

    private String findStationIdByName(String name, Map<String, Station> stations) {
        for (Station s : stations.values()) {
            if (s.getName().equalsIgnoreCase(name)) return s.getId();
        }
        return null;
    }

    private Station findStationByName(String name, Map<String, Station> stations) {
        for (Station s : stations.values()) {
            if (s.getName().equalsIgnoreCase(name)) return s;
        }
        return null;
    }

    public ExplainResult explain(String fromName, String toName) {
        Map<String, Station> stations = new HashMap<>();
        for (Station s : stationRepository.findAll()) {
            stations.put(s.getId(), s);
        }

        Station fromStation = findStationByName(fromName, stations);
        Station toStation = findStationByName(toName, stations);
        if (fromStation == null || toStation == null) return null;

        List<Connection> connections = new ArrayList<>(connectionRepository.findAll());
        Map<String, String> mergeMap = buildMergeMap(stations);

        ExplainResult.PathDetail tflPath = buildTflPath(fromStation, toStation, stations, connections);
        ExplainResult.PathDetail mergedPath = buildMergedPath(fromStation, toStation, stations, mergeMap, connections);

        return new ExplainResult(fromStation.getName(), toStation.getName(), tflPath, mergedPath);
    }

    private ExplainResult.PathDetail buildTflPath(Station from, Station to,
                                                   Map<String, Station> stations,
                                                   List<Connection> connections) {
        Graph graph = calculateShortest(stations, connections, from);

        Node destNode = graph.getNodes().stream()
                .filter(n -> n.getName().equals(to.getId()))
                .findFirst().orElse(null);

        if (destNode == null || destNode.getDistance() == Integer.MAX_VALUE) {
            return new ExplainResult.PathDetail(-1, -1, Collections.emptyList());
        }

        List<String> path = new ArrayList<>();
        for (Node n : destNode.getShortestPath()) {
            Station s = stations.get(n.getName());
            path.add(s != null ? s.getName() : n.getName());
        }
        path.add(to.getName());

        return new ExplainResult.PathDetail(destNode.getDistance(), Station.zoneDifference(from, to), path);
    }

    private ExplainResult.PathDetail buildMergedPath(Station from, Station to,
                                                      Map<String, Station> stations,
                                                      Map<String, String> mergeMap,
                                                      List<Connection> allConnections) {
        Map<String, Station> mergedStations = new HashMap<>();
        for (Map.Entry<String, String> entry : mergeMap.entrySet()) {
            mergedStations.putIfAbsent(entry.getValue(), stations.get(entry.getValue()));
        }

        List<Connection> mergedConnections = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        for (Connection c : allConnections) {
            String s1 = mergeMap.get(c.getStation1Id());
            String s2 = mergeMap.get(c.getStation2Id());
            if (s1 == null || s2 == null || s1.equals(s2)) continue;
            String newId = Station.getConnectionId(s1, s2);
            if (seenIds.add(newId)) {
                Connection newConn = new Connection();
                newConn.setId(newId);
                newConn.setStation1Id(s1);
                newConn.setStation2Id(s2);
                mergedConnections.add(newConn);
            }
        }

        String canonicalFromId = mergeMap.get(from.getId());
        String canonicalToId = mergeMap.get(to.getId());
        Station canonicalFrom = mergedStations.get(canonicalFromId);
        Station canonicalTo = mergedStations.get(canonicalToId);

        if (canonicalFrom == null || canonicalTo == null) {
            return new ExplainResult.PathDetail(-1, -1, Collections.emptyList());
        }

        if (canonicalFromId.equals(canonicalToId)) {
            return new ExplainResult.PathDetail(0, 0, List.of(canonicalFrom.getName()));
        }

        Graph graph = calculateShortest(mergedStations, mergedConnections, canonicalFrom);

        Node destNode = graph.getNodes().stream()
                .filter(n -> n.getName().equals(canonicalToId))
                .findFirst().orElse(null);

        if (destNode == null || destNode.getDistance() == Integer.MAX_VALUE) {
            return new ExplainResult.PathDetail(-1, -1, Collections.emptyList());
        }

        List<String> path = new ArrayList<>();
        for (Node n : destNode.getShortestPath()) {
            Station s = mergedStations.get(n.getName());
            path.add(s != null ? s.getName() : n.getName());
        }
        path.add(canonicalTo.getName());

        return new ExplainResult.PathDetail(destNode.getDistance(), Station.zoneDifference(canonicalFrom, canonicalTo), path);
    }

    public String find(String stationName, Integer stops, Integer zones) {
        List<Station> stations = new ArrayList<>();
        for (Station station : stationRepository.findAll()) {
            if (station.getName().toLowerCase().contains(stationName.toLowerCase())) {
                stations.add(station);
            }
        }

        String result = "";
        for (Station station : stations) {
            for (Route route : routeRepository.findAll()) {
                if (route.getId().contains(station.getId())) {
                    if (route.getStops() == stops) {
                        if (zones <= 2) {
                            if (route.getZones() == zones) {
                                result += route.getStation1() + " " + route.getStation2() + " " + route.getStops() + " " + route.getZones();
                                result += "\n";
                            }
                        } else {
                            if (route.getZones() > 2) {
                                result += route.getStation1() + " " + route.getStation2() + " " + route.getStops() + " " + route.getZones();
                                result += "\n";
                            }
                        }
                    }
                }
            }
        }

        return result.isEmpty() ? "None" : result;
    }
}
