package com.hda.notificaciones.model.notificacion.gateways;

import com.hda.notificaciones.model.notificacion.NotificacionEnviada;
import com.hda.notificaciones.model.trabajocreado.TrabajoCreadoEvento;
import com.hda.notificaciones.model.usuario.ContactoUsuario;
import reactor.core.publisher.Mono;

public interface CanalNotificacion {

    Mono<NotificacionEnviada> enviar(TrabajoCreadoEvento evento, ContactoUsuario contacto);
}
