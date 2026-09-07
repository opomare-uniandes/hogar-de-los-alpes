package com.hda.notificaciones.usecase.enviarnotificacion;

import com.hda.notificaciones.model.notificacion.NotificacionEnviada;
import com.hda.notificaciones.model.notificacion.gateways.EmailSender;
import com.hda.notificaciones.model.notificacion.gateways.NotificacionEnviadaPublisher;
import com.hda.notificaciones.model.trabajocreado.TrabajoCreadoEvento;
import reactor.core.publisher.Mono;

import java.time.Instant;

public class EnviarNotificacionUseCase {

    private static final String MENSAJE = "Tu trabajo fue creado y ya estamos trabajando en asignarlo.";

    private final EmailSender emailSender;
    private final NotificacionEnviadaPublisher notificacionEnviadaPublisher;

    public EnviarNotificacionUseCase(EmailSender emailSender,
                                      NotificacionEnviadaPublisher notificacionEnviadaPublisher) {
        this.emailSender = emailSender;
        this.notificacionEnviadaPublisher = notificacionEnviadaPublisher;
    }

    public Mono<Void> ejecutar(TrabajoCreadoEvento evento) {
        return emailSender.enviar(evento)
                .flatMap(destinatario -> {
                    NotificacionEnviada notificacionEnviada = new NotificacionEnviada(
                            evento.trabajoId(),
                            evento.clienteId(),
                            "EMAIL",
                            destinatario,
                            MENSAJE,
                            Instant.now()
                    );
                    return notificacionEnviadaPublisher.publicar(notificacionEnviada);
                });
    }
}
