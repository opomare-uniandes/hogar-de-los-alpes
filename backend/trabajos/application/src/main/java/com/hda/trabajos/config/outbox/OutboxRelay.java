package com.hda.trabajos.config.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hda.trabajos.model.seedwork.DomainEvent;
import com.hda.trabajos.model.trabajo.TrabajoAsignadoDomainEvent;
import com.hda.trabajos.model.trabajo.TrabajoCanceladoDomainEvent;
import com.hda.trabajos.model.trabajo.TrabajoCreadoDomainEvent;
import com.hda.trabajos.model.trabajo.gateways.TrabajoEventPublisher;
import com.hda.trabajos.r2dbc.outbox.OutboxEventoEntity;
import com.hda.trabajos.r2dbc.outbox.OutboxEventoR2dbcRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Relay del patron Transactional Outbox: TrabajoRepositoryAdapter ya inserto los eventos de
 * dominio en outbox_evento en la misma transaccion que el agregado (ver seccion de UoW del
 * plan). Este componente solo hace de correa de transmision hacia Pulsar (via el mismo
 * TrabajoEventPublisher/PulsarEventPublisherAdapter que antes llamaban los casos de uso
 * directamente), con reintento automatico si un ciclo de poll falla.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Map<String, Class<? extends DomainEvent>> TIPOS = Map.of(
            "TrabajoCreadoDomainEvent", TrabajoCreadoDomainEvent.class,
            "TrabajoAsignadoDomainEvent", TrabajoAsignadoDomainEvent.class,
            "TrabajoCanceladoDomainEvent", TrabajoCanceladoDomainEvent.class
    );

    private final OutboxEventoR2dbcRepository outboxRepository;
    private final TrabajoEventPublisher eventPublisher;
    private final Duration intervalo;

    public OutboxRelay(OutboxEventoR2dbcRepository outboxRepository,
                        TrabajoEventPublisher eventPublisher,
                        @Value("${hda.outbox.polling-interval:PT2S}") Duration intervalo) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
        this.intervalo = intervalo;
    }

    @EventListener(ApplicationReadyEvent.class)
    void iniciar() {
        Flux.interval(intervalo)
                .concatMap(tick -> publicarPendientes()
                        .onErrorResume(e -> {
                            log.error("[OUTBOX] Fallo publicando pendientes, se reintenta en el proximo ciclo", e);
                            return Mono.empty();
                        }))
                .subscribe();
        log.info("[OUTBOX] Relay iniciado, poll cada {}", intervalo);
    }

    private Mono<Void> publicarPendientes() {
        return outboxRepository.findByPublicadoEnIsNullOrderByOcurridoEnAsc()
                .concatMap(this::publicarYMarcar)
                .then();
    }

    private Mono<Void> publicarYMarcar(OutboxEventoEntity fila) {
        DomainEvent evento = aDominio(fila);
        if (evento == null) {
            log.warn("[OUTBOX] Tipo de evento desconocido, se omite: {} (id={})", fila.getTipoEvento(), fila.getId());
            return Mono.empty();
        }
        return eventPublisher.publicarTodos(List.of(evento))
                .then(marcarPublicado(fila.getId()))
                .doOnSuccess(v -> log.info("[OUTBOX] Evento publicado y marcado: {} (id={})",
                        fila.getTipoEvento(), fila.getId()));
    }

    private Mono<Void> marcarPublicado(UUID id) {
        return outboxRepository.marcarPublicado(id, Instant.now());
    }

    private DomainEvent aDominio(OutboxEventoEntity fila) {
        Class<? extends DomainEvent> tipo = TIPOS.get(fila.getTipoEvento());
        if (tipo == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(fila.getPayload(), tipo);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo deserializar el evento de outbox " + fila.getId(), e);
        }
    }
}
