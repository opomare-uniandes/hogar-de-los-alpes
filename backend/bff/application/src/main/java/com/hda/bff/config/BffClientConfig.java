package com.hda.bff.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class BffClientConfig {

    @Bean
    WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean("trabajosWebClient")
    WebClient trabajosWebClient(WebClient.Builder builder,
                                @Value("${hda.services.trabajos-base-url}") String baseUrl,
                                @Value("${hda.services.timeout:3s}") Duration timeout) {
        return client(builder, baseUrl, timeout);
    }

    @Bean("sagaWebClient")
    WebClient sagaWebClient(WebClient.Builder builder,
                            @Value("${hda.services.saga-base-url}") String baseUrl,
                            @Value("${hda.services.timeout:3s}") Duration timeout) {
        return client(builder, baseUrl, timeout);
    }

    private WebClient client(WebClient.Builder builder, String baseUrl, Duration timeout) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(timeout.toMillis()))
                .responseTimeout(timeout)
                .doOnConnected(connection -> connection.addHandlerLast(
                        new ReadTimeoutHandler(timeout.toMillis(), TimeUnit.MILLISECONDS)));

        return builder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
