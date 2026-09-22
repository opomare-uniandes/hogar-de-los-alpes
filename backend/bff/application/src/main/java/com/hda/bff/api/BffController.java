package com.hda.bff.api;

import com.hda.bff.api.dto.CrearTrabajoRequest;
import com.hda.bff.api.dto.SagaTrabajoResponse;
import com.hda.bff.api.dto.SeguimientoTrabajoResponse;
import com.hda.bff.api.dto.TrabajoResponse;
import com.hda.bff.client.SagaClient;
import com.hda.bff.client.TrabajosClient;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class BffController {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final TrabajosClient trabajosClient;
    private final SagaClient sagaClient;

    public BffController(TrabajosClient trabajosClient, SagaClient sagaClient) {
        this.trabajosClient = trabajosClient;
        this.sagaClient = sagaClient;
    }

    @PostMapping("/trabajos")
    public Mono<ResponseEntity<TrabajoResponse>> crearTrabajo(
            @Valid @RequestBody CrearTrabajoRequest request,
            @RequestHeader(name = CORRELATION_HEADER, required = false) String correlationId) {
        String correlation = correlationId(correlationId);
        return trabajosClient.crear(request, correlation)
                .map(trabajo -> ResponseEntity
                        .created(URI.create("/api/v1/trabajos/" + trabajo.id()))
                        .header(CORRELATION_HEADER, correlation)
                        .body(trabajo));
    }

    @GetMapping("/trabajos/{trabajoId}")
    public Mono<ResponseEntity<TrabajoResponse>> consultarTrabajo(
            @PathVariable UUID trabajoId,
            @RequestHeader(name = CORRELATION_HEADER, required = false) String correlationId) {
        String correlation = correlationId(correlationId);
        return trabajosClient.consultar(trabajoId, correlation)
                .map(trabajo -> ResponseEntity.ok()
                        .header(CORRELATION_HEADER, correlation)
                        .body(trabajo));
    }

    @GetMapping("/trabajos/{trabajoId}/seguimiento")
    public Mono<ResponseEntity<SeguimientoTrabajoResponse>> consultarSeguimiento(
            @PathVariable UUID trabajoId,
            @RequestHeader(name = CORRELATION_HEADER, required = false) String correlationId) {
        String correlation = correlationId(correlationId);
        return Mono.zip(
                        trabajosClient.consultar(trabajoId, correlation),
                        sagaClient.consultarOpcionalPorTrabajo(trabajoId, correlation))
                .map(tuple -> tuple.getT2()
                        .map(saga -> SeguimientoTrabajoResponse.conSaga(tuple.getT1(), saga))
                        .orElseGet(() -> SeguimientoTrabajoResponse.sagaPendiente(tuple.getT1())))
                .map(response -> ResponseEntity.ok()
                        .header(CORRELATION_HEADER, correlation)
                        .body(response));
    }

    @GetMapping("/sagas/{sagaId}")
    public Mono<ResponseEntity<SagaTrabajoResponse>> consultarSaga(
            @PathVariable UUID sagaId,
            @RequestHeader(name = CORRELATION_HEADER, required = false) String correlationId) {
        String correlation = correlationId(correlationId);
        return sagaClient.consultarPorSaga(sagaId, correlation)
                .map(saga -> ResponseEntity.ok()
                        .header(CORRELATION_HEADER, correlation)
                        .body(saga));
    }

    @GetMapping("/sagas/trabajos/{trabajoId}")
    public Mono<ResponseEntity<SagaTrabajoResponse>> consultarSagaPorTrabajo(
            @PathVariable UUID trabajoId,
            @RequestHeader(name = CORRELATION_HEADER, required = false) String correlationId) {
        String correlation = correlationId(correlationId);
        return sagaClient.consultarPorTrabajo(trabajoId, correlation)
                .map(saga -> ResponseEntity.ok()
                        .header(CORRELATION_HEADER, correlation)
                        .body(saga));
    }

    private String correlationId(String value) {
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }
}
