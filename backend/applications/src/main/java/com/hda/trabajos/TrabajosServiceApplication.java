package com.hda.trabajos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;

import java.util.Map;

@SpringBootApplication(exclude = {DataRedisAutoConfiguration.class, DataRedisReactiveAutoConfiguration.class})
public class TrabajosServiceApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(TrabajosServiceApplication.class);

        app.setDefaultProperties(Map.of("spring.config.name", "application-trabajos"));
        app.run(args);
    }
}
