package com.hda.notificaciones.model.notificacion.gateways;

import com.hda.notificaciones.model.notificacion.NotificacionEnviada;
import reactor.core.publisher.Mono;

public interface NotificacionEnviadaPublisher {

    Mono<Void> publicar(NotificacionEnviada notificacionEnviada);
}
