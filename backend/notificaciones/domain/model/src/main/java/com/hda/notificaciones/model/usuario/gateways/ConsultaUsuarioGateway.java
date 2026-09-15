package com.hda.notificaciones.model.usuario.gateways;

import com.hda.notificaciones.model.usuario.ContactoUsuario;
import reactor.core.publisher.Mono;

public interface ConsultaUsuarioGateway {

    Mono<ContactoUsuario> obtenerContacto(String clienteId);
}
