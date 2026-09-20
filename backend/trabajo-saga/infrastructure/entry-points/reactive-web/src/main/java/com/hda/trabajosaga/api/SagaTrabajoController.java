package com.hda.trabajosaga.api;

import com.hda.trabajosaga.api.dto.SagaTrabajoDTO;
import com.hda.trabajosaga.usecase.consultarsaga.ConsultarSagaTrabajoUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/sagas")
public class SagaTrabajoController {

    private final ConsultarSagaTrabajoUseCase consultarSagaTrabajoUseCase;

    public SagaTrabajoController(ConsultarSagaTrabajoUseCase consultarSagaTrabajoUseCase) {
        this.consultarSagaTrabajoUseCase = consultarSagaTrabajoUseCase;
    }

    @GetMapping("/trabajos/{trabajoId}")
    public Mono<ResponseEntity<SagaTrabajoDTO>> consultarPorTrabajoId(@PathVariable UUID trabajoId) {
        return consultarSagaTrabajoUseCase.buscarPorTrabajoId(trabajoId)
                .map(SagaTrabajoDTO::desde)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/{sagaId}")
    public Mono<ResponseEntity<SagaTrabajoDTO>> consultarPorSagaId(@PathVariable UUID sagaId) {
        return consultarSagaTrabajoUseCase.buscarPorSagaId(sagaId)
                .map(SagaTrabajoDTO::desde)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
