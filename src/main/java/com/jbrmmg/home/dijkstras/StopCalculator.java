package com.jbrmmg.home.dijkstras;

import com.jbrmmg.home.data.ConnectionRepository;
import com.jbrmmg.home.data.RouteRepository;
import com.jbrmmg.home.data.StationRepository;
import com.jbrmmg.home.data.entity.Connection;
import com.jbrmmg.home.data.entity.Route;
import com.jbrmmg.home.data.entity.Station;
import com.jbrmmg.home.dijkstras.graph.Graph;
import com.jbrmmg.home.dijkstras.graph.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StopCalculator {
    private static final Logger log = LoggerFactory.getLogger(StopCalculator.class);

    private final StationRepository stationRepository;
    private final ConnectionRepository connectionRepository;
    private final RouteRepository routeRepository;

    @Autowired
    public StopCalculator(StationRepository stationRepository, ConnectionRepository connectionRepository, RouteRepository routeRepository) {
        this.stationRepository = stationRepository;
        this.connectionRepository = connectionRepository;
        this.routeRepository = routeRepository;
    }

    public Graph calculateShortest(Map<String,Station> stations, List<Connection> connections, Station source) {
        Map<String, Node> nodeMap = new HashMap<>();
        Node sourceNode = null;
        for(Station station : stations.values()) {
            nodeMap.put(station.getId(),new Node(station.getId()));

            if(source.getId().equals(station.getId())) {
                sourceNode = nodeMap.get(source.getId());
            }
        }

        for(Connection connection : connections) {
            String[] keys = connection.getId().split("-");

            Node node1 = nodeMap.get(keys[0]);
            Node node2 = nodeMap.get(keys[1]);

            node1.addDestination(node2,1);
            node2.addDestination(node1,1);
        }

        Graph result = new Graph();

        for(Map.Entry<String,Node> nextNode : nodeMap.entrySet()) {
            result.addNode(nextNode.getValue());
        }

        return Graph.calculateShortestPathFromSource(result,sourceNode);
    }

    public void calculateRoutes() {
        routeRepository.deleteAll();

        // Load the stations.
        Map<String,Station> stations = new HashMap<>();
        for(Station station : stationRepository.findAll()) {
            stations.put(station.getId(),station);
        }

        // Load the connections.
        List<Connection> connections = new ArrayList<>(connectionRepository.findAll());

        // Set-up a map of routes.
        Map<String,Route> routes = new HashMap<>();

        // Get the stations and create nodes for each.
        int count = 1;
        for(Station station : stations.values()) {
            log.info("Calculate shortest for {} {} {}", count, stations.size(), station.getName());

            Graph shortest = calculateShortest(stations, connections, station);

            // Create the data from this.
            for(Node nextNode : shortest.getNodes()) {
                String id = Station.getConnectionId(nextNode.getName(),station.getId());

                if(!routes.containsKey(id)) {
                    int stops = nextNode.getDistance();
                    Station other = stations.get(nextNode.getName());

                    int zones = Station.zoneDifference(station, other);

                    Route newRoute = new Route();
                    newRoute.setId(id);
                    newRoute.setStops(stops);
                    newRoute.setZones(zones);
                    newRoute.setStation1(station.getName());
                    newRoute.setStation2(other.getName());

                    routes.put(id,newRoute);
                }
            }

            count++;
        }

        // Save the routes.
        routeRepository.saveAll(routes.values());
        log.info("Done");
    }

    public String find(String stationName, Integer stops, Integer zones) {
        // Load the stations.
        List<Station> stations = new ArrayList<>();

        for(Station station : stationRepository.findAll()) {
            if(station.getName().toLowerCase().contains(stationName.toLowerCase())) {
                stations.add(station);
            }
        }

        String result = "";

        for(Station station : stations) {
            for(Route route : routeRepository.findAll()) {
                if(route.getId().contains(station.getId())) {
                    if(route.getStops() == stops) {
                        if(zones <= 2) {
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

        if(result.length() == 0) {
            return "None";
        }

        return result;
    }
}
