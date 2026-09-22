package com.hda.proveedor.usecase.liberarproveedor;

import com.hda.proveedor.model.proveedor.gateways.ProveedorRepository;
import reactor.core.publisher.Mono;

public class LiberarProveedorUseCase {

    private final ProveedorRepository proveedorRepository;

    public LiberarProveedorUseCase(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    public Mono<Void> ejecutar(LiberarProveedorCommand comando) {
        // La publicacion del evento ya no ocurre aqui: ProveedorRepositoryAdapter.guardar() lo
        // inserta en outbox_evento en la misma transaccion del agregado; OutboxRelay lo envia
        // a Pulsar despues (ver patron Transactional Outbox).
        return proveedorRepository.buscarPorId(comando.proveedorId())
                .flatMap(proveedor -> {
                    proveedor.liberar(comando.sagaId());
                    return proveedorRepository.guardar(proveedor);
                })
                .doOnSuccess(guardado -> guardado.limpiarEventos())
                .then();
    }
}
