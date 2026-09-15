package com.hda.notificaciones.idempotencia;

import com.hda.notificaciones.model.idempotencia.gateways.EventDeduplicationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Adaptador Redis del puerto EventDeduplicationStore. Usa SET NX EX (atomico) para
 * registrar el id del evento solo si no existia. El TTL evita que el keyspace crezca
 * sin limite: pasado ese tiempo un id se olvida (aceptable porque Pulsar no reentrega
 * indefinidamente). Sirve entre instancias (suscripcion Shared, escenario 2.3), a
 * diferencia de una cache en memoria por instancia.
 */
@Component
public class RedisEventDeduplicationStore implements EventDeduplicationStore {

    private static final Logger log = LoggerFactory.getLogger(RedisEventDeduplicationStore.class);
    private static final String PREFIJO = "notificaciones:evento-visto:";

    private final ReactiveStringRedisTemplate redis;
    private final Duration ttl;

    public RedisEventDeduplicationStore(
            ReactiveStringRedisTemplate redis,
            @Value("${hda.idempotencia.ttl:PT24H}") Duration ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    @Override
    public Mono<Boolean> registrarSiNoVisto(String eventId) {
        String clave = PREFIJO + eventId;
        return redis.opsForValue()
                .setIfAbsent(clave, "1", ttl)
                .doOnNext(primeraVez -> {
                    if (Boolean.FALSE.equals(primeraVez)) {
                        log.info("[IDEMPOTENCIA] Evento duplicado detectado, se salta: id={}", eventId);
                    }
                });
    }
}
