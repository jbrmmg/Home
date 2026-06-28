package com.jbrmmg.home.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI homeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Home API")
                        .description("API for transport route queries and energy consumption data")
                        .version("v1"));
    }
}
