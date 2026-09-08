package com.hda.trabajos.model.trabajo;

import com.hda.trabajos.model.seedwork.DomainEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifica el campo `id` del evento de dominio TrabajoCreado: cada ocurrencia del evento
 * tiene su propia identidad (distinta de la identidad del agregado), lo que habilita
 * deduplicacion/idempotencia en consumidores con suscripcion Shared.
 */
class TrabajoCrearEventIdTest {

    private static Trabajo nuevoTrabajo() {
        return Trabajo.crear(
                UUID.randomUUID(),
                new CategoriaServicio("plomeria"),
                Urgencia.ALTA,
                "Bogota",
                OrigenTrabajo.MARKETPLACE,
                null,
                Moneda.cop());
    }

    @Test
    void crear_registra_un_TrabajoCreado_con_id_no_nulo() {
        Trabajo trabajo = nuevoTrabajo();

        assertEquals(1, trabajo.eventosDeDominio().size(), "debe registrarse exactamente un evento");
        DomainEvent evento = trabajo.eventosDeDominio().getFirst();
        TrabajoCreadoDomainEvent creado = assertInstanceOf(TrabajoCreadoDomainEvent.class, evento);

        assertNotNull(creado.id(), "el id del evento no puede ser nulo");
        assertEquals(creado.id(), evento.id(), "id() del contrato DomainEvent y del record deben coincidir");
    }

    @Test
    void id_del_evento_es_distinto_del_id_del_agregado() {
        Trabajo trabajo = nuevoTrabajo();
        TrabajoCreadoDomainEvent creado =
                (TrabajoCreadoDomainEvent) trabajo.eventosDeDominio().getFirst();

        assertEquals(trabajo.getId(), creado.trabajoId(), "trabajoId del evento = id del agregado");
        assertNotEquals(creado.id(), creado.trabajoId(),
                "el id del evento (identidad de la ocurrencia) debe diferir del id del agregado");
    }

    @Test
    void cada_trabajo_creado_produce_un_id_de_evento_unico() {
        UUID id1 = ((TrabajoCreadoDomainEvent) nuevoTrabajo().eventosDeDominio().getFirst()).id();
        UUID id2 = ((TrabajoCreadoDomainEvent) nuevoTrabajo().eventosDeDominio().getFirst()).id();

        assertNotEquals(id1, id2, "cada ocurrencia del evento debe tener su propio id");
    }
}
