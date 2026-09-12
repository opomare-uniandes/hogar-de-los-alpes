package com.hda.notificaciones.model.usuario.gateways;

import com.hda.notificaciones.model.usuario.ContactoUsuario;
import reactor.core.publisher.Mono;

/**
 * Puerto de consulta hacia usuarios-service. Es el unico punto sincrono de todo el
 * sistema: una consulta de solo lectura (contacto/preferencias), no un comando -
 * la comunicacion entre servicios sigue siendo 100% por eventos sobre Pulsar en
 * todo lo demas.
 */
public interface ConsultaUsuarioGateway {

    Mono<ContactoUsuario> obtenerContacto(String clienteId);
}
