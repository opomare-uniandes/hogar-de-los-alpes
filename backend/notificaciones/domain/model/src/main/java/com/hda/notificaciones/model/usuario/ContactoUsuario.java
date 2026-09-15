package com.hda.notificaciones.model.usuario;

public record ContactoUsuario(
        String correo,
        String celular,
        boolean notificarPorEmail,
        boolean notificarPorWhatsapp
) {
}
