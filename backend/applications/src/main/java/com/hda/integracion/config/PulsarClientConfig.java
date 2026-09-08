package com.hda.integracion.config;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El bean PulsarClient es infraestructura compartida entre el entry point
 * (pulsar-event-handler, el consumidor) y el driven adapter (pulsar-event-bus, el
 * productor). Vive aqui, en applications/app-service, para no crear una dependencia
 * artificial entre esos dos modulos de infrastructure.
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
