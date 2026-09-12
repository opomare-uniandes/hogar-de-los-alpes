package com.hda.notificaciones.model.notificacion.gateways;

import com.hda.notificaciones.model.notificacion.NotificacionEnviada;
import com.hda.notificaciones.model.trabajocreado.TrabajoCreadoEvento;
import com.hda.notificaciones.model.usuario.ContactoUsuario;
import reactor.core.publisher.Mono;

/**
 * Generaliza el antiguo puerto EmailSender: un canal de notificacion (email, whatsapp,
 * el que sea) recibe el evento y el contacto ya resuelto, y devuelve el registro de que
 * se envio. Agregar un canal nuevo es agregar un adaptador que implemente este puerto -
 * no toca este puerto ni al caso de uso que lo orquesta (EnviarNotificacionUseCase).
 */
public interface CanalNotificacion {

    Mono<NotificacionEnviada> enviar(TrabajoCreadoEvento evento, ContactoUsuario contacto);
}
