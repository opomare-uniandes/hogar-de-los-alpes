package com.hda.usuarios.api.dto;

import com.hda.usuarios.model.usuario.Usuario;

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
