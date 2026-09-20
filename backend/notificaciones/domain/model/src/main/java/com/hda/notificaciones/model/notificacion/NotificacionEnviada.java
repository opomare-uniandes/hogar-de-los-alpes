package com.hda.notificaciones.model.notificacion;

import java.time.Instant;

/** sagaId es nulo para el flujo original (avisar que se creo un Trabajo) y viene poblado
 * solo cuando esta notificacion es el ultimo paso de la saga "Asignacion de trabajo con
 * proveedor" (ver EnviarNotificacionSagaUseCase). */
public record NotificacionEnviada(
        String sagaId,
        String trabajoId,
        String clienteId,
        String canal,
        String destinatario,
        String mensaje,
        Instant fechaEnvio
) {
}
