package com.hda.notificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;

import java.util.Map;

@SpringBootApplication(exclude = R2dbcAutoConfiguration.class)
public class NotificacionesServiceApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(NotificacionesServiceApplication.class);

        app.setDefaultProperties(Map.of("spring.config.name", "application-notificaciones"));
        app.run(args);
    }
}
