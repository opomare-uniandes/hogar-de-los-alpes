package com.hda.integracion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;

import java.util.Map;

@SpringBootApplication(exclude = R2dbcAutoConfiguration.class)
public class IntegracionServiceApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(IntegracionServiceApplication.class);

        app.setDefaultProperties(Map.of("spring.config.name", "application-integracion"));
        app.run(args);
    }
}
