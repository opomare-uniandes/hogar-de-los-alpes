package com.hda.trabajos.usecase.consultartrabajo;

import com.hda.trabajos.model.trabajo.Trabajo;
import com.hda.trabajos.model.trabajo.gateways.TrabajoRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class ConsultarTrabajoUseCase {

    private final TrabajoRepository trabajoRepository;

    public ConsultarTrabajoUseCase(TrabajoRepository trabajoRepository) {
        this.trabajoRepository = trabajoRepository;
    }

    public Mono<Trabajo> ejecutar(UUID trabajoId) {
        return trabajoRepository.buscarPorId(trabajoId);
    }
}
