package com.hda.usuarios.api;

import com.hda.usuarios.api.dto.ContactoUsuarioDTO;
import com.hda.usuarios.usecase.consultarusuario.ConsultarUsuarioUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final ConsultarUsuarioUseCase consultarUsuarioUseCase;

    public UsuarioController(ConsultarUsuarioUseCase consultarUsuarioUseCase) {
        this.consultarUsuarioUseCase = consultarUsuarioUseCase;
    }

    @GetMapping("/{clienteId}/contacto")
    public Mono<ResponseEntity<ContactoUsuarioDTO>> obtenerContacto(@PathVariable UUID clienteId) {
        return consultarUsuarioUseCase.ejecutar(clienteId)
                .map(ContactoUsuarioDTO::desde)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
