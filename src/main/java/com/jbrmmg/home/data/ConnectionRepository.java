package com.jbrmmg.home.data;

import com.jbrmmg.home.data.entity.Connection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionRepository extends JpaRepository<Connection,String> {
}
