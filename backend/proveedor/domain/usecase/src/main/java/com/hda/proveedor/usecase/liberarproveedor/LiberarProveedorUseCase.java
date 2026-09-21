package com.hda.proveedor.usecase.liberarproveedor;

import com.hda.proveedor.model.proveedor.gateways.ProveedorEventPublisher;
import com.hda.proveedor.model.proveedor.gateways.ProveedorRepository;
import reactor.core.publisher.Mono;

public class LiberarProveedorUseCase {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorEventPublisher eventPublisher;

    public LiberarProveedorUseCase(ProveedorRepository proveedorRepository, ProveedorEventPublisher eventPublisher) {
        this.proveedorRepository = proveedorRepository;
        this.eventPublisher = eventPublisher;
    }

    public Mono<Void> ejecutar(LiberarProveedorCommand comando) {
        return proveedorRepository.buscarPorId(comando.proveedorId())
                .flatMap(proveedor -> {
                    proveedor.liberar(comando.sagaId());
                    return proveedorRepository.guardar(proveedor);
                })
                .flatMap(guardado -> eventPublisher.publicarTodos(guardado.eventosDeDominio())
                        .doOnSuccess(v -> guardado.limpiarEventos()));
    }
}
