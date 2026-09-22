package com.hda.bff.client;

import com.hda.bff.api.dto.CrearTrabajoRequest;
import com.hda.bff.api.dto.TrabajoResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TrabajosClient {

    private final WebClient webClient;

    public TrabajosClient(@Qualifier("trabajosWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<TrabajoResponse> crear(CrearTrabajoRequest request, String correlationId) {
        return webClient.post()
                .uri("/trabajos")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-Id", correlationId)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new UpstreamException("trabajos-service", response.statusCode(), body)))
                .bodyToMono(TrabajoResponse.class);
    }

    public Mono<TrabajoResponse> consultar(UUID trabajoId, String correlationId) {
        return webClient.get()
                .uri("/trabajos/{id}", trabajoId)
                .header("X-Correlation-Id", correlationId)
                .retrieve()
                .onStatus(status -> status.isError(), response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new UpstreamException("trabajos-service", response.statusCode(), body)))
                .bodyToMono(TrabajoResponse.class);
    }
}
