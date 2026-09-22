package com.hda.trabajos.r2dbc.outbox;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

public interface OutboxEventoR2dbcRepository extends ReactiveCrudRepository<OutboxEventoEntity, UUID> {

    Flux<OutboxEventoEntity> findByPublicadoEnIsNullOrderByOcurridoEnAsc();

    /** UPDATE directo -- no usar save() aqui, OutboxEventoEntity.isNew() siempre es true. */
    @Modifying
    @Query("UPDATE outbox_evento SET publicado_en = :publicadoEn WHERE id = :id")
    Mono<Void> marcarPublicado(UUID id, Instant publicadoEn);
}
