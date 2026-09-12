package com.hda.notificaciones.model.usuario;

/**
 * Lo que notificaciones-service necesita saber de un Usuario para decidir por donde
 * notificarlo. Se obtiene via ConsultaUsuarioGateway (unico punto sincrono del sistema,
 * hacia usuarios-service) - no es una entidad propia de este servicio.
 */
public record ContactoUsuario(
        String correo,
        String celular,
        boolean notificarPorEmail,
        boolean notificarPorWhatsapp
) {
}
