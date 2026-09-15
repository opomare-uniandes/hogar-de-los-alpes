package com.hda.usuarios.r2dbc;

import com.hda.usuarios.model.usuario.Usuario;
import com.hda.usuarios.model.usuario.gateways.UsuarioRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioR2dbcRepository r2dbcRepository;

    public UsuarioRepositoryAdapter(UsuarioR2dbcRepository r2dbcRepository) {
        this.r2dbcRepository = r2dbcRepository;
    }

    @Override
    public Mono<Usuario> buscarPorId(UUID id) {
        return r2dbcRepository.findById(id).map(this::aDominio);
    }

    private Usuario aDominio(UsuarioEntity e) {
        return Usuario.reconstruir(
                e.getId(),
                e.getNombre(),
                e.getCorreo(),
                e.getCelular(),
                e.isNotificarPorEmail(),
                e.isNotificarPorWhatsapp()
        );
    }
}
