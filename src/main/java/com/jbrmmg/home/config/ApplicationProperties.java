package com.jbrmmg.home.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix="home")
public class ApplicationProperties {
    private List<String> allowedRecipients;
    private EmailProperties email;
    private List<List<String>> stationMerges;
}
