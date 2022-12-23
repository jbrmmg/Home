package com.jbrmmg.home.data;

import com.jbrmmg.home.data.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationRepository extends JpaRepository<Station,String> {
}
