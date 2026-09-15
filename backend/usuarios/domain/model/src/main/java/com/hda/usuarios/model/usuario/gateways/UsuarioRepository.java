package com.hda.usuarios.model.usuario.gateways;

import com.hda.usuarios.model.usuario.Usuario;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UsuarioRepository {

    Mono<Usuario> buscarPorId(UUID id);
}
