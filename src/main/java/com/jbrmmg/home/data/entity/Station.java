package com.jbrmmg.home.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Station {
    @Id
    private String id;

    @Column
    private String name;

    @Column(name="zone_1")
    private Integer zone1;

    @Column(name="zone_2")
    private Integer zone2;

    @Column
    private String fullId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getZone1() {
        return zone1;
    }

    public void setZone1(Integer zone1) {
        this.zone1 = zone1;
    }

    public Integer getZone2() {
        if(zone2 == null) {
            return getZone1();
        }

        return zone2;
    }

    public boolean hasZone2() {
        return zone2 != null;
    }


    public void setZone2(Integer zone2) {
        this.zone2 = zone2;
    }

    public String getFullId() {
        return fullId;
    }

    public void setFullId(String fullId) {
        this.fullId = fullId;
    }

    public static String getConnectionId(Station one, Station two) {
        return getConnectionId(one.getId(),two.getId());
    }

    public static String getConnectionId(String one, String two) {
        if(one.compareTo(two) < 0) {
            return one + "-" + two;
        }

        return two + "-" + one;
    }

    public static int zoneDifference(Station one, Station two) {
        int[] diffs = new int[4];

        diffs[0] = Math.abs(one.getZone1() - two.getZone1());
        diffs[1] = Math.abs(one.getZone2() - two.getZone1());
        diffs[2] = Math.abs(one.getZone1() - two.getZone2());
        diffs[3] = Math.abs(one.getZone2() - two.getZone2());

        return Math.min(diffs[0],Math.min(diffs[1],Math.min(diffs[2],diffs[3])));
    }
}
