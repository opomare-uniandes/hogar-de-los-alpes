package com.hda.trabajos.config;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PulsarConfig {

    @Bean(destroyMethod = "close")
    public PulsarClient pulsarClient(@Value("${hda.pulsar.service-url}") String serviceUrl)
            throws PulsarClientException {
        return PulsarClient.builder()
                .serviceUrl(serviceUrl)
                .build();
    }
}
