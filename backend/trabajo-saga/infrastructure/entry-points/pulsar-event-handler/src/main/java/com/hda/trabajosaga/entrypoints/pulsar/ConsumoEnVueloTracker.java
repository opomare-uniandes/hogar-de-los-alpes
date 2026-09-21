package com.hda.trabajosaga.entrypoints.pulsar;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cuenta mensajes en vuelo por listener para permitir un shutdown que drena el procesamiento
 * pendiente antes de cerrar el consumidor Pulsar. Necesario para escalar trabajo-saga-service:
 * KEDA puede terminar un pod en cualquier momento, y sin esto un mensaje interrumpido a mitad
 * de SagaTrabajoRepositoryAdapter.guardar() puede quedar en un estado parcial (saga_trabajo
 * actualizado pero saga_paso no insertado), lo que en la redelivery reprocesa el paso y publica
 * su comando de salida por segunda vez.
 */
final class ConsumoEnVueloTracker {

    private static final Logger log = LoggerFactory.getLogger(ConsumoEnVueloTracker.class);
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(25);
    private static final Duration INTERVALO_SONDEO = Duration.ofMillis(100);

    private final AtomicInteger enVuelo = new AtomicInteger();

    void iniciar() {
        enVuelo.incrementAndGet();
    }

    void finalizar() {
        enVuelo.decrementAndGet();
    }

    /** Bloquea hasta que no haya mensajes en vuelo o se agote ESPERA_MAXIMA. Se llama desde
     * @PreDestroy, antes de cerrar el consumidor Pulsar. */
    void esperarDrenaje(String nombreListener) {
        long limite = System.currentTimeMillis() + ESPERA_MAXIMA.toMillis();
        while (enVuelo.get() > 0 && System.currentTimeMillis() < limite) {
            try {
                Thread.sleep(INTERVALO_SONDEO.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        int restantes = enVuelo.get();
        if (restantes > 0) {
            log.warn("[{}] Cerrando con {} mensaje(s) aun en procesamiento tras {}s de espera de drenaje",
                    nombreListener, restantes, ESPERA_MAXIMA.getSeconds());
        }
    }
}
