package com.hda.usuarios.r2dbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("usuario")
public class UsuarioEntity {

    @Id
    private UUID id;
    private String nombre;
    private String correo;
    private String celular;
    private boolean notificarPorEmail;
    private boolean notificarPorWhatsapp;

    public UsuarioEntity() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }
    public boolean isNotificarPorEmail() { return notificarPorEmail; }
    public void setNotificarPorEmail(boolean notificarPorEmail) { this.notificarPorEmail = notificarPorEmail; }
    public boolean isNotificarPorWhatsapp() { return notificarPorWhatsapp; }
    public void setNotificarPorWhatsapp(boolean notificarPorWhatsapp) { this.notificarPorWhatsapp = notificarPorWhatsapp; }
}
