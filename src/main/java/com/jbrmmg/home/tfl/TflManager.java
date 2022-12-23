package com.jbrmmg.home.tfl;

import com.jbrmmg.home.data.ConnectionRepository;
import com.jbrmmg.home.data.LineRepository;
import com.jbrmmg.home.data.StationRepository;
import com.jbrmmg.home.data.entity.Connection;
import com.jbrmmg.home.data.entity.Station;
import com.jbrmmg.home.tfl.line.mode.Mode;
import com.jbrmmg.home.tfl.line.mode.data.Line;
import com.jbrmmg.home.tfl.line.route.Route;
import com.jbrmmg.home.tfl.line.route.data.StopPoint;
import com.jbrmmg.home.tfl.line.route.data.StopPointSequence;
import com.jbrmmg.home.tfl.line.route.data.ValidRoutesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TflManager {
    private static final Logger log = LoggerFactory.getLogger(TflManager.class);

    private final LineRepository lineRepository;
    private final StationRepository stationRepository;
    private final ConnectionRepository connectionRepository;

    @Autowired
    public TflManager(LineRepository lineRepository, StationRepository stationRepository, ConnectionRepository connectionRepository) {
        this.lineRepository = lineRepository;
        this.stationRepository = stationRepository;
        this.connectionRepository = connectionRepository;
    }

    private Station createStation(StopPoint stationData) {
        Station station = new Station();

        station.setId(stationData.stationId.substring(stationData.stationId.length()-3));
        station.setName(stationData.name.replace(" Underground Station",""));
        station.setFullId(stationData.stationId);

        String specialZone = stationData.zone.replace("/", "+");
        String[] zones = specialZone.split("\\+");

        station.setZone1(Integer.parseInt(zones[0]));
        if(zones.length > 1) {
            station.setZone2(Integer.parseInt(zones[1]));
        }

        return station;
    }

    public void refreshRoutes(String lineId) {
        ValidRoutesResponse response = Route.getRoutes(lineId);

        for(StopPointSequence next : response.stopPointSequences) {
            Station previous = null;

            for(StopPoint next2 : next.stopPoint) {
                // Create a station.
                Station station = createStation(next2);

                Optional<Station> found = stationRepository.findById(station.getId());
                if(found.isEmpty()) {
                    stationRepository.save(station);
                }

                if(previous != null) {
                    Connection connection = new Connection();

                    connection.setId(Station.getConnectionId(previous,station));
                    connection.setLineId(lineId);
                    connection.setStation1Id(previous.getId());
                    connection.setStation2Id(station.getId());

                    Optional<Connection> foundConnection = connectionRepository.findById(connection.getId());

                    // Create the connection (if not already done)
                    if(foundConnection.isEmpty()) {
                        connectionRepository.save(connection);
                    }
                }

                log.info("Found: {} {} {}", next2.name, next2.stationId, next2.zone);

                previous = station;
            }
        }
    }

    public void refreshLines() {
        lineRepository.deleteAll();

        Line[] lines = Mode.getLines();

        for(Line next : lines) {
            log.info("Found {} {}", next.id, next.name);

            com.jbrmmg.home.data.entity.Line newLine = new com.jbrmmg.home.data.entity.Line();
            newLine.setId(next.id);
            newLine.setName(next.name);

            lineRepository.save(newLine);

            refreshRoutes(next.id);
        }
    }
}
