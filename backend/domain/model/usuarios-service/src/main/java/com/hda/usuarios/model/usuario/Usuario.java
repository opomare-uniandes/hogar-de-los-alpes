package com.hda.usuarios.model.usuario;

import com.hda.usuarios.model.seedwork.AggregateRoot;

import java.util.UUID;

/**
 * Agregado raiz Usuario (ver Entrega 2 - Vista de Informacion: una de las 9 agregaciones,
 * con invariante de identidad unica de contacto). Esta entrega solo lo usa como fuente
 * de consulta (contacto + preferencias de canal) para notificaciones-service: no publica
 * eventos de dominio todavia ni tiene comandos de creacion/edicion via API (los 3 usuarios
 * de esta entrega vienen semillados). Se modela igual que Trabajo, con el mismo rigor
 * tactico, para no reestructurar nada si en una entrega futura se agrega, por ejemplo,
 * un evento PreferenciaNotificacionActualizada.
 */
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
