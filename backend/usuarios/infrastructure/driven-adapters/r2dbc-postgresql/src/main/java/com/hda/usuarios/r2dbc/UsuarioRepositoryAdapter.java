package com.hda.usuarios.r2dbc;

import com.hda.usuarios.model.usuario.Usuario;
import com.hda.usuarios.model.usuario.gateways.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioR2dbcRepository r2dbcRepository;

    private static final Logger log = LoggerFactory.getLogger(UsuarioRepositoryAdapter.class);

    public UsuarioRepositoryAdapter(UsuarioR2dbcRepository r2dbcRepository) {
        this.r2dbcRepository = r2dbcRepository;
    }

    @Override
    public Mono<Usuario> buscarPorId(UUID clienteId) {

        log.info("[GET: OBTENER-CONTACTO -> RECIBIDO: clienteId={}]", clienteId);
        return r2dbcRepository.findById(clienteId)
                .map(this::aDominio);
    }

    private Usuario aDominio(UsuarioEntity usuarioEntity) {
        return Usuario.reconstruir(
                usuarioEntity.getId(),
                usuarioEntity.getNombre(),
                usuarioEntity.getCorreo(),
                usuarioEntity.getCelular(),
                usuarioEntity.isNotificarPorEmail(),
                usuarioEntity.isNotificarPorWhatsapp()
        );
    }
}
