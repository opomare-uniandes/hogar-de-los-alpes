package com.hda.notificaciones.usecase.enviarnotificacionsaga;

/** Comando compartido por los dos comandos entrantes de la saga (NotificarAsignacionCommandV1
 * y NotificarFalloCommandV1): mismo mecanismo de envio, solo cambia el mensaje. */
public record NotificarSagaCommand(String sagaId, String trabajoId, String clienteId, String mensaje) {
}
