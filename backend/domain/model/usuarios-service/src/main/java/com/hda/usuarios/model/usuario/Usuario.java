package com.hda.usuarios.model.usuario;

import com.hda.usuarios.model.seedwork.AggregateRoot;

import java.util.UUID;

public class Usuario extends AggregateRoot<UUID> {

    private final String nombre;
    private final String correo;
    private final String celular;
    private boolean notificarPorEmail;
    private boolean notificarPorWhatsapp;

    private Usuario(UUID id, String nombre, String correo, String celular,
                     boolean notificarPorEmail, boolean notificarPorWhatsapp) {
        super(id);
        this.nombre = nombre;
        this.correo = correo;
        this.celular = celular;
        this.notificarPorEmail = notificarPorEmail;
        this.notificarPorWhatsapp = notificarPorWhatsapp;
    }

    /** Factory (patron Factory de DDD tactico): unico punto de creacion valido de un Usuario. */
    public static Usuario crear(String nombre, String correo, String celular,
                                 boolean notificarPorEmail, boolean notificarPorWhatsapp) {
        return new Usuario(UUID.randomUUID(), nombre, correo, celular,
                notificarPorEmail, notificarPorWhatsapp);
    }

    /** Reconstruccion desde persistencia (no dispara eventos de dominio). */
    public static Usuario reconstruir(UUID id, String nombre, String correo, String celular,
                                       boolean notificarPorEmail, boolean notificarPorWhatsapp) {
        return new Usuario(id, nombre, correo, celular, notificarPorEmail, notificarPorWhatsapp);
    }

    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getCelular() { return celular; }
    public boolean isNotificarPorEmail() { return notificarPorEmail; }
    public boolean isNotificarPorWhatsapp() { return notificarPorWhatsapp; }
}
