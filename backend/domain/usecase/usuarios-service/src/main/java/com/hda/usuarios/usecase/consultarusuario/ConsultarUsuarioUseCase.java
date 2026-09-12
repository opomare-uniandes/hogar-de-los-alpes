package com.hda.usuarios.usecase.consultarusuario;

import com.hda.usuarios.model.usuario.Usuario;
import com.hda.usuarios.model.usuario.gateways.UsuarioRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class ConsultarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;

    public ConsultarUsuarioUseCase(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Mono<Usuario> ejecutar(UUID clienteId) {
        return usuarioRepository.buscarPorId(clienteId);
    }
}
