package com.hda.notificaciones.model.notificacion.gateways;

import com.hda.notificaciones.model.trabajocreado.TrabajoCreadoEvento;
import reactor.core.publisher.Mono;

public interface EmailSender {

    /** Envia (simulado) el email para el Trabajo dado y devuelve el destinatario usado. */
    Mono<String> enviar(TrabajoCreadoEvento evento);
}
