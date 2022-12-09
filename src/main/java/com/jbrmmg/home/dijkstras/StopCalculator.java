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
    private static Logger log = LoggerFactory.getLogger(StopCalculator.class);

    private final StationRepository stationRepository;
    private final ConnectionRepository connectionRepository;
    private final RouteRepository routeRepository;

    @Autowired
    public StopCalculator(StationRepository stationRepository, ConnectionRepository connectionRepository, RouteRepository routeRepository) {
        this.stationRepository = stationRepository;
        this.connectionRepository = connectionRepository;
        this.routeRepository = routeRepository;
    }

    public Graph calculateShortest(Station source) {
        Map<String, Node> nodeMap = new HashMap<>();
        Node sourceNode = null;
        for(Station station : stationRepository.findAll()) {
            nodeMap.put(station.getId(),new Node(station.getId()));

            if(source.getId().equals(station.getId())) {
                sourceNode = nodeMap.get(source.getId());
            }
        }

        for(Connection connection : connectionRepository.findAll()) {
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
        // Load the stations.
        Map<String,Station> stations = new HashMap<>();
        for(Station station : stationRepository.findAll()) {
            stations.put(station.getId(),station);
        }

        // Get the stations and create nodes for each.
        for(Station station : stations.values()) {
            log.info("Calculate shortest for {}", station.getName());

            Graph shortest = calculateShortest(station);

            // Create the data from this.
            for(Node nextNode : shortest.getNodes()) {
                String id = Station.getConnectionId(nextNode.getName(),station.getId());

                Optional<Route> alreadyExist = routeRepository.findById(id);
                if(alreadyExist.isEmpty()) {
                    int stops = nextNode.getDistance();
                    Station other = stations.get(nextNode.getName());

                    int zones = Station.zoneDifference(station, other);

                    Route newRoute = new Route();
                    newRoute.setId(id);
                    newRoute.setStops(stops);
                    newRoute.setZones(zones);
                    newRoute.setStation1(station.getName());
                    newRoute.setStation2(other.getName());

                    routeRepository.save(newRoute);
                }
            }
        }
    }
}
