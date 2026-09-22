package com.hda.trabajosaga.usecase.orquestarsaga;

import com.hda.trabajosaga.model.sagatrabajo.EstadoSaga;
import com.hda.trabajosaga.model.sagatrabajo.SagaTrabajo;
import com.hda.trabajosaga.model.sagatrabajo.gateways.SagaTrabajoRepository;
import reactor.core.publisher.Mono;

/**
 * Maquina de estados de la saga "Asignacion de trabajo con proveedor" (seccion 4.2 del plan).
 * Un metodo publico por cada uno de los 6 eventos que la orquestan; cada uno delega la
 * transicion (y su idempotencia) al agregado SagaTrabajo. La publicacion de los comandos
 * resultantes ya no ocurre aqui: SagaTrabajoRepositoryAdapter.guardar() los inserta en
 * outbox_evento en la misma transaccion del agregado; OutboxRelay los envia a Pulsar despues
 * (ver patron Transactional Outbox).
 */
public class OrquestarSagaTrabajoUseCase {

    private final SagaTrabajoRepository repository;

    public OrquestarSagaTrabajoUseCase(SagaTrabajoRepository repository) {
        this.repository = repository;
    }

    /** TrabajoCreado -> crea saga_trabajo (INICIADA) y pide RESERVAR_PROVEEDOR. Idempotente por
     * trabajoId: no hay sagaId todavia en este punto para usar como clave. */
    public Mono<Void> manejarTrabajoCreado(TrabajoCreadoComando comando) {
        return repository.buscarPorTrabajoId(comando.trabajoId())
                .flatMap(yaIniciada -> Mono.<Void>empty())
                .switchIfEmpty(Mono.defer(() -> {
                    SagaTrabajo saga = SagaTrabajo.iniciar(comando.trabajoId(), comando.clienteId(),
                            comando.categoriaServicio(), comando.ciudad());
                    return guardarYPublicar(saga);
                }));
    }

    public Mono<Void> manejarProveedorReservado(ProveedorReservadoComando comando) {
        return repository.buscarPorId(comando.sagaId())
                .flatMap(saga -> {
                    saga.registrarProveedorReservado(comando.proveedorId(), comando.nombreProveedor());
                    return guardarYPublicar(saga);
                });
    }

    public Mono<Void> manejarProveedorNoDisponible(ProveedorNoDisponibleComando comando) {
        return repository.buscarPorId(comando.sagaId())
                .flatMap(saga -> {
                    saga.registrarProveedorNoDisponible(comando.motivo());
                    return guardarYPublicar(saga);
                });
    }

    public Mono<Void> manejarTrabajoAsignado(TrabajoAsignadoComando comando) {
        return repository.buscarPorId(comando.sagaId())
                .flatMap(saga -> {
                    saga.registrarTrabajoAsignado();
                    return guardarYPublicar(saga);
                });
    }

    public Mono<Void> manejarTrabajoCancelado(TrabajoCanceladoComando comando) {
        return repository.buscarPorId(comando.sagaId())
                .flatMap(saga -> {
                    saga.registrarTrabajoCancelado();
                    return guardarYPublicar(saga);
                });
    }

    /** NotificacionEnviada es el mismo evento (reutilizado desde la Entrega 4) para ambos
     * caminos: se distingue por el estado ACTUAL de la saga, no por el contenido del evento
     * (ver seccion 4.2 del plan). ASIGNADA/COMPLETADA -> camino feliz; COMPENSANDO/CANCELADA
     * -> camino de compensacion (incluir los estados terminales hace esto idempotente tambien
     * ante una redelivery de Pulsar despues de que la saga ya cerro). */
    public Mono<Void> manejarNotificacionEnviada(NotificacionEnviadaComando comando) {
        return repository.buscarPorId(comando.sagaId())
                .flatMap(saga -> {
                    if (saga.getEstado() == EstadoSaga.ASIGNADA || saga.getEstado() == EstadoSaga.COMPLETADA) {
                        saga.completar();
                    } else if (saga.getEstado() == EstadoSaga.COMPENSANDO || saga.getEstado() == EstadoSaga.CANCELADA) {
                        saga.cancelar();
                    } else {
                        return Mono.<Void>empty();
                    }
                    return guardarYPublicar(saga);
                });
    }

    private Mono<Void> guardarYPublicar(SagaTrabajo saga) {
        return repository.guardar(saga)
                .doOnSuccess(SagaTrabajo::limpiarEventos)
                .then();
    }
}
