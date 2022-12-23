package com.jbrmmg.home.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;

@Entity
public class Route {
    @Id
    private String id;

    @Column
    private Integer stops;

    @Column
    private Integer zones;

    @Column(name="station_1")
    private String station1;

    @Column(name="station_2")
    private String station2;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getStops() {
        return stops;
    }

    public void setStops(Integer stops) {
        this.stops = stops;
    }

    public Integer getZones() {
        return zones;
    }

    public void setZones(Integer zones) {
        this.zones = zones;
    }

    public String getStation1() {
        return station1;
    }

    public void setStation1(String station1) {
        this.station1 = station1;
    }

    public String getStation2() {
        return station2;
    }

    public void setStation2(String station2) {
        this.station2 = station2;
    }
}
