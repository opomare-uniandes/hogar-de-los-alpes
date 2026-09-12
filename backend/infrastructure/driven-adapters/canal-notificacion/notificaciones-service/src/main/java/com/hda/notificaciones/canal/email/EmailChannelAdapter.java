package com.hda.notificaciones.canal.email;

import com.hda.notificaciones.model.notificacion.NotificacionEnviada;
import com.hda.notificaciones.model.notificacion.gateways.CanalNotificacion;
import com.hda.notificaciones.model.trabajocreado.TrabajoCreadoEvento;
import com.hda.notificaciones.model.usuario.ContactoUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

// Nombre de bean explicito ("emailChannel"): EnviarNotificacionUseCase distingue este
// canal del de WhatsApp por nombre de parametro/bean, no por tipo (los dos implementan
// CanalNotificacion). Ver el comentario en ese caso de uso.
@Component("emailChannel")
public class EmailChannelAdapter implements CanalNotificacion {

    private static final Logger log = LoggerFactory.getLogger(EmailChannelAdapter.class);
    private static final String MENSAJE = "Tu trabajo fue creado y ya estamos trabajando en asignarlo.";

    @Override
    public Mono<NotificacionEnviada> enviar(TrabajoCreadoEvento evento, ContactoUsuario contacto) {
        String destinatario = contacto.correo();

        log.info("[EMAIL SIMULADO] Para: {} | Asunto: Tu trabajo {} fue creado | "
                        + "Categoria: {} | Ciudad: {} | Urgencia: {}",
                destinatario, evento.trabajoId(), evento.categoriaServicio(),
                evento.ciudad(), evento.urgencia());

        return Mono.just(new NotificacionEnviada(
                evento.trabajoId(), evento.clienteId(), "EMAIL", destinatario, MENSAJE, Instant.now()));
    }
}
