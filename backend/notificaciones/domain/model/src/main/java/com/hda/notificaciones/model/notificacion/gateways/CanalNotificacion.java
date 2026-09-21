package com.hda.notificaciones.model.notificacion.gateways;

import com.hda.notificaciones.model.notificacion.NotificacionEnviada;
import com.hda.notificaciones.model.usuario.ContactoUsuario;
import reactor.core.publisher.Mono;

/**
 * Generalizado en la seccion 6.2 del plan (Entrega 5): antes tomaba un TrabajoCreadoEvento
 * completo, pero eso acoplaba el canal al flujo original y hardcodeaba su mensaje - no
 * servia para reenviar el mensaje que ya trae NotificarAsignacionCommandV1/NotificarFalloCommandV1
 * desde la saga. Ahora recibe exactamente lo que un canal necesita para enviar y para
 * construir la NotificacionEnviada resultante; EnviarNotificacionUseCase y
 * EnviarNotificacionSagaUseCase son sus dos unicos llamadores.
 */
public interface CanalNotificacion {

    Mono<NotificacionEnviada> enviar(String sagaId, String trabajoId, String clienteId, String mensaje,
                                      ContactoUsuario contacto);
}
