package com.hda.trabajos.api;

import com.hda.trabajos.api.dto.TrabajoDTO;
import com.hda.trabajos.usecase.consultartrabajo.ConsultarTrabajoUseCase;
import com.hda.trabajos.usecase.creartrabajo.CrearTrabajoCommand;
import com.hda.trabajos.usecase.creartrabajo.CrearTrabajoUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/trabajos")
public class TrabajoController {

    private final CrearTrabajoUseCase crearTrabajoUseCase;
    private final ConsultarTrabajoUseCase consultarTrabajoUseCase;

    public TrabajoController(CrearTrabajoUseCase crearTrabajoUseCase,
                              ConsultarTrabajoUseCase consultarTrabajoUseCase) {
        this.crearTrabajoUseCase = crearTrabajoUseCase;
        this.consultarTrabajoUseCase = consultarTrabajoUseCase;
    }

    // Comando
    @PostMapping
    public Mono<ResponseEntity<TrabajoDTO>> crearTrabajo(@RequestBody CrearTrabajoCommand comando) {
        return crearTrabajoUseCase.ejecutar(comando)
                .map(TrabajoDTO::desde)
                .map(dto -> ResponseEntity.created(URI.create("/trabajos/" + dto.id())).body(dto));
    }

    // Consulta
    @GetMapping("/{id}")
    public Mono<ResponseEntity<TrabajoDTO>> consultarTrabajo(@PathVariable UUID id) {
        return consultarTrabajoUseCase.ejecutar(id)
                .map(TrabajoDTO::desde)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
