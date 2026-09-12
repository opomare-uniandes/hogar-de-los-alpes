package com.hda.notificaciones.usecase.enviarnotificacion;

import com.hda.notificaciones.model.notificacion.NotificacionEnviada;
import com.hda.notificaciones.model.notificacion.gateways.CanalNotificacion;
import com.hda.notificaciones.model.notificacion.gateways.NotificacionEnviadaPublisher;
import com.hda.notificaciones.model.trabajocreado.TrabajoCreadoEvento;
import com.hda.notificaciones.model.usuario.gateways.ConsultaUsuarioGateway;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class EnviarNotificacionUseCase {

    private final ConsultaUsuarioGateway consultaUsuarioGateway;
    private final CanalNotificacion emailChannel;
    private final CanalNotificacion whatsappChannel;
    private final NotificacionEnviadaPublisher notificacionEnviadaPublisher;

    public EnviarNotificacionUseCase(ConsultaUsuarioGateway consultaUsuarioGateway,
                                      CanalNotificacion emailChannel,
                                      CanalNotificacion whatsappChannel,
                                      NotificacionEnviadaPublisher notificacionEnviadaPublisher) {
        this.consultaUsuarioGateway = consultaUsuarioGateway;
        this.emailChannel = emailChannel;
        this.whatsappChannel = whatsappChannel;
        this.notificacionEnviadaPublisher = notificacionEnviadaPublisher;
    }

    public Mono<Void> ejecutar(TrabajoCreadoEvento evento) {
        return consultaUsuarioGateway.obtenerContacto(evento.clienteId())
                .flatMapMany(contacto -> {
                    List<Mono<NotificacionEnviada>> envios = new ArrayList<>();
                    if (contacto.notificarPorEmail()) {
                        envios.add(emailChannel.enviar(evento, contacto));
                    }
                    if (contacto.notificarPorWhatsapp()) {
                        envios.add(whatsappChannel.enviar(evento, contacto));
                    }
                    return Flux.merge(envios);
                })
                .flatMap(notificacionEnviadaPublisher::publicar)
                .then();
    }
}
