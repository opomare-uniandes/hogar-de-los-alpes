package com.hda.bff.api;

import com.hda.bff.api.dto.CrearTrabajoRequest;
import com.hda.bff.api.dto.SagaTrabajoResponse;
import com.hda.bff.api.dto.TrabajoResponse;
import com.hda.bff.client.SagaClient;
import com.hda.bff.client.TrabajosClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BffControllerTest {

    private final TrabajosClient trabajosClient = mock(TrabajosClient.class);
    private final SagaClient sagaClient = mock(SagaClient.class);
    private WebTestClient client;

    private final UUID trabajoId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();
    private final UUID partnerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToController(new BffController(trabajosClient, sagaClient))
                .controllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void creaTrabajoPorMedioDelBff() {
        TrabajoResponse response = trabajo();
        when(trabajosClient.crear(any(CrearTrabajoRequest.class), anyString())).thenReturn(Mono.just(response));

        client.post().uri("/api/v1/trabajos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new CrearTrabajoRequest(
                        clienteId, "PLOMERIA", "MEDIA", "Bogotá", "MARKETPLACE", partnerId, "COP"))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("X-Correlation-Id")
                .expectBody()
                .jsonPath("$.id").isEqualTo(trabajoId.toString());
    }

    @Test
    void componeTrabajoConSaga() {
        UUID sagaId = UUID.randomUUID();
        SagaTrabajoResponse saga = new SagaTrabajoResponse(
                sagaId, trabajoId, "COMPLETADA", Instant.now(), Instant.now(), List.of());
        when(trabajosClient.consultar(trabajoId, "corr-1")).thenReturn(Mono.just(trabajo()));
        when(sagaClient.consultarOpcionalPorTrabajo(trabajoId, "corr-1"))
                .thenReturn(Mono.just(Optional.of(saga)));

        client.get().uri("/api/v1/trabajos/{id}/seguimiento", trabajoId)
                .header("X-Correlation-Id", "corr-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.trabajo.id").isEqualTo(trabajoId.toString())
                .jsonPath("$.saga.sagaId").isEqualTo(sagaId.toString())
                .jsonPath("$.estadoSeguimiento").isEqualTo("COMPLETADA");
    }

    @Test
    void informaCuandoLaSagaAunNoHaComenzado() {
        when(trabajosClient.consultar(trabajoId, "corr-2")).thenReturn(Mono.just(trabajo()));
        when(sagaClient.consultarOpcionalPorTrabajo(trabajoId, "corr-2"))
                .thenReturn(Mono.just(Optional.empty()));

        client.get().uri("/api/v1/trabajos/{id}/seguimiento", trabajoId)
                .header("X-Correlation-Id", "corr-2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estadoSeguimiento").isEqualTo("SAGA_PENDIENTE")
                .jsonPath("$.saga").doesNotExist();
    }

    @Test
    void rechazaComandoIncompletoConErrorUniforme() {
        client.post().uri("/api/v1/trabajos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"urgencia\":\"MEDIA\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.source").isEqualTo("bff-service")
                .jsonPath("$.detail").isNotEmpty();
    }

    private TrabajoResponse trabajo() {
        return new TrabajoResponse(
                trabajoId, clienteId, "PLOMERIA", "MEDIA", "Bogotá", "MARKETPLACE",
                partnerId, "COP", "CREADO", Instant.now());
    }
}
