package com.jbrmmg.home.data;

import com.jbrmmg.home.data.entity.Setting;
import org.springframework.data.repository.CrudRepository;

public interface SettingRepository extends CrudRepository<Setting, String> {
}
