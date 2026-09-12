package com.hda.notificaciones.canal.whatsapp;

import com.hda.notificaciones.model.notificacion.NotificacionEnviada;
import com.hda.notificaciones.model.notificacion.gateways.CanalNotificacion;
import com.hda.notificaciones.model.trabajocreado.TrabajoCreadoEvento;
import com.hda.notificaciones.model.usuario.ContactoUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component("whatsappChannel")
public class WhatsAppChannelAdapter implements CanalNotificacion {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppChannelAdapter.class);
    private static final String MENSAJE = "Tu trabajo fue creado y ya estamos trabajando en asignarlo.";

    @Override
    public Mono<NotificacionEnviada> enviar(TrabajoCreadoEvento evento, ContactoUsuario contacto) {
        String destinatario = contacto.celular();

        log.info("[WHATSAPP SIMULADO] Para: {} | Mensaje: Tu trabajo {} fue creado | "
                        + "Categoria: {} | Ciudad: {} | Urgencia: {}",
                destinatario, evento.trabajoId(), evento.categoriaServicio(),
                evento.ciudad(), evento.urgencia());

        return Mono.just(new NotificacionEnviada(
                evento.trabajoId(), evento.clienteId(), "WHATSAPP", destinatario, MENSAJE, Instant.now()));
    }
}
