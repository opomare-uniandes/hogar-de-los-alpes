package com.hda.notificaciones.config;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El bean PulsarClient es infraestructura compartida entre el entry point
 * (pulsar-event-handler, el consumidor de trabajo-creado) y el driven adapter
 * (pulsar-event-bus, el productor de notificacion-enviada). Vive aqui, en
 * applications/, para no crear una dependencia artificial entre esos dos modulos
 * de infrastructure (mismo patron que trabajos-service/integracion-service).
 */
@Configuration
public class PulsarClientConfig {

    @Bean(destroyMethod = "close")
    public PulsarClient pulsarClient(@Value("${hda.pulsar.service-url}") String serviceUrl)
            throws PulsarClientException {
        return PulsarClient.builder()
                .serviceUrl(serviceUrl)
                .build();
    }
}
