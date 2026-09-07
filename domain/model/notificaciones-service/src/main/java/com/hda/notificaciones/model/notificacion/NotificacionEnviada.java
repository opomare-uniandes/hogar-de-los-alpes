package com.hda.notificaciones.model.notificacion;

import java.time.Instant;

public record NotificacionEnviada(
        String trabajoId,
        String clienteId,
        String canal,
        String destinatario,
        String mensaje,
        Instant fechaEnvio
) {
}
