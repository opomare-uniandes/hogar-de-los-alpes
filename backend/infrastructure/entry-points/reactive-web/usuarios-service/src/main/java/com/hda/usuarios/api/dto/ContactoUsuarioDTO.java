package com.hda.usuarios.api.dto;

import com.hda.usuarios.model.usuario.Usuario;

/**
 * DTO de lectura expuesto por la API. Solo expone lo que notificaciones-service necesita
 * (contacto + preferencias de canal), nunca la entidad de dominio directamente.
 */
public record ContactoUsuarioDTO(
        String correo,
        String celular,
        boolean notificarPorEmail,
        boolean notificarPorWhatsapp
) {
    public static ContactoUsuarioDTO desde(Usuario usuario) {
        return new ContactoUsuarioDTO(
                usuario.getCorreo(),
                usuario.getCelular(),
                usuario.isNotificarPorEmail(),
                usuario.isNotificarPorWhatsapp()
        );
    }
}
