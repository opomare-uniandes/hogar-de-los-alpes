package com.hda.bff.client;

import com.hda.bff.api.dto.SagaTrabajoResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Component
public class SagaClient {

    private final WebClient webClient;

    public SagaClient(@Qualifier("sagaWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<SagaTrabajoResponse> consultarPorSaga(UUID sagaId, String correlationId) {
        return consultar("/sagas/{id}", sagaId, correlationId);
    }

    public Mono<Optional<SagaTrabajoResponse>> consultarOpcionalPorTrabajo(UUID trabajoId, String correlationId) {
        return webClient.get()
                .uri("/sagas/trabajos/{id}", trabajoId)
                .header("X-Correlation-Id", correlationId)
                .exchangeToMono(response -> {
                    if (response.statusCode().equals(HttpStatus.NOT_FOUND)) {
                        return Mono.just(Optional.empty());
                    }
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new UpstreamException(
                                        "trabajo-saga-service", response.statusCode(), body)));
                    }
                    return response.bodyToMono(SagaTrabajoResponse.class).map(Optional::of);
                });
    }

    public Mono<SagaTrabajoResponse> consultarPorTrabajo(UUID trabajoId, String correlationId) {
        return consultar("/sagas/trabajos/{id}", trabajoId, correlationId);
    }

    private Mono<SagaTrabajoResponse> consultar(String path, UUID id, String correlationId) {
        return webClient.get()
                .uri(path, id)
                .header("X-Correlation-Id", correlationId)
                .retrieve()
                .onStatus(status -> status.isError(), response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new UpstreamException("trabajo-saga-service", response.statusCode(), body)))
                .bodyToMono(SagaTrabajoResponse.class);
    }
}
