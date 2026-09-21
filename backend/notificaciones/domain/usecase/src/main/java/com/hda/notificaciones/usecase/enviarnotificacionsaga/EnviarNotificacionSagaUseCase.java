package com.hda.notificaciones.usecase.enviarnotificacionsaga;

import com.hda.notificaciones.model.notificacion.NotificacionEnviada;
import com.hda.notificaciones.model.notificacion.gateways.CanalNotificacion;
import com.hda.notificaciones.model.notificacion.gateways.NotificacionEnviadaPublisher;
import com.hda.notificaciones.model.usuario.gateways.ConsultaUsuarioGateway;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/** Ultimo paso (NOTIFICAR / NOTIFICAR_FALLO) de la saga "Asignacion de trabajo con proveedor"
 * (seccion 6.2 del plan). Reutiliza el mismo CanalNotificacion (Email/WhatsApp) que
 * EnviarNotificacionUseCase - la unica diferencia es que el mensaje ya viene armado en el
 * comando en vez de construirse aqui. */
public class EnviarNotificacionSagaUseCase {

    private final ConsultaUsuarioGateway consultaUsuarioGateway;
    private final CanalNotificacion emailChannel;
    private final CanalNotificacion whatsappChannel;
    private final NotificacionEnviadaPublisher notificacionEnviadaPublisher;

    public EnviarNotificacionSagaUseCase(ConsultaUsuarioGateway consultaUsuarioGateway,
                                          CanalNotificacion emailChannel,
                                          CanalNotificacion whatsappChannel,
                                          NotificacionEnviadaPublisher notificacionEnviadaPublisher) {
        this.consultaUsuarioGateway = consultaUsuarioGateway;
        this.emailChannel = emailChannel;
        this.whatsappChannel = whatsappChannel;
        this.notificacionEnviadaPublisher = notificacionEnviadaPublisher;
    }

    public Mono<Void> ejecutar(NotificarSagaCommand comando) {
        return consultaUsuarioGateway.obtenerContacto(comando.clienteId())
                .flatMapMany(contacto -> {
                    List<Mono<NotificacionEnviada>> envios = new ArrayList<>();
                    if (contacto.notificarPorEmail()) {
                        envios.add(emailChannel.enviar(comando.sagaId(), comando.trabajoId(),
                                comando.clienteId(), comando.mensaje(), contacto));
                    }
                    if (contacto.notificarPorWhatsapp()) {
                        envios.add(whatsappChannel.enviar(comando.sagaId(), comando.trabajoId(),
                                comando.clienteId(), comando.mensaje(), contacto));
                    }
                    return Flux.merge(envios);
                })
                .flatMap(notificacionEnviadaPublisher::publicar)
                .then();
    }
}
