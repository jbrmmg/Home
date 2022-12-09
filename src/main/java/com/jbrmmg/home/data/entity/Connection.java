package com.jbrmmg.home.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Connection {
    @Id
    private String id;

    @Column
    private String lineId;

    @Column(name="station_1_id")
    private String station1Id;

    @Column(name="station_2_id")
    private String station2Id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLineId() {
        return lineId;
    }

    public void setLineId(String lineId) {
        this.lineId = lineId;
    }

    public String getStation1Id() {
        return station1Id;
    }

    public void setStation1Id(String station1Id) {
        this.station1Id = station1Id;
    }

    public String getStation2Id() {
        return station2Id;
    }

    public void setStation2Id(String station2Id) {
        this.station2Id = station2Id;
    }
}
