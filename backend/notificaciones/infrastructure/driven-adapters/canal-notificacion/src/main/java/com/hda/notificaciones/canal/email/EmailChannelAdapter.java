package com.hda.notificaciones.canal.email;

import com.hda.notificaciones.model.notificacion.NotificacionEnviada;
import com.hda.notificaciones.model.notificacion.gateways.CanalNotificacion;
import com.hda.notificaciones.model.usuario.ContactoUsuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component("emailChannel")
public class EmailChannelAdapter implements CanalNotificacion {

    private static final Logger log = LoggerFactory.getLogger(EmailChannelAdapter.class);

    @Override
    public Mono<NotificacionEnviada> enviar(String sagaId, String trabajoId, String clienteId, String mensaje,
                                             ContactoUsuario contacto) {
        String destinatario = contacto.correo();

        log.info("[EMAIL SIMULADO] Para: {} | Mensaje: {} | TrabajoId: {} | SagaId: {}",
                destinatario, mensaje, trabajoId, sagaId);

        return Mono.just(new NotificacionEnviada(
                sagaId, trabajoId, clienteId, "EMAIL", destinatario, mensaje, Instant.now()));
    }
}
