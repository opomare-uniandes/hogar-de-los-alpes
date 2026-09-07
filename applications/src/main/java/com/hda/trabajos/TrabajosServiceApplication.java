package com.hda.trabajos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class TrabajosServiceApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TrabajosServiceApplication.class);

        app.setDefaultProperties(Map.of("spring.config.name", "application-trabajos"));
        app.run(args);
    }
}
