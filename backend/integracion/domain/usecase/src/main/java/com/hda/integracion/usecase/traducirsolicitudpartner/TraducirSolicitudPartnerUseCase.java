package com.hda.integracion.usecase.traducirsolicitudpartner;

import com.hda.integracion.model.partner.SolicitudTrabajoPartner;
import com.hda.integracion.model.partner.TranslationException;
import com.hda.integracion.model.trabajo.CrearTrabajoIntegradoCommand;
import com.hda.integracion.model.trabajo.gateways.CrearTrabajoCommandPublisher;
import reactor.core.publisher.Mono;

import java.util.Map;

/** Anti-corruption layer: traduce V1/V2 del partner al comando del contexto Trabajos. */
public class TraducirSolicitudPartnerUseCase {

    private static final Map<String, String> CATEGORIAS = Map.of(
            "PLUMBING", "PLOMERIA",
            "ELECTRICAL", "ELECTRICIDAD",
            "LOCKSMITH", "CERRAJERIA"
    );
    private static final Map<String, String> URGENCIAS = Map.of(
            "P4", "BAJA", "P3", "MEDIA", "P2", "ALTA", "P1", "CRITICA"
    );
    private static final Map<String, String> CIUDADES = Map.of(
            "BOG", "BOGOTA", "MDE", "MEDELLIN", "CLO", "CALI", "MEX", "CIUDAD_DE_MEXICO"
    );

    private final CrearTrabajoCommandPublisher publisher;

    public TraducirSolicitudPartnerUseCase(CrearTrabajoCommandPublisher publisher) {
        this.publisher = publisher;
    }

    public Mono<Void> ejecutar(SolicitudTrabajoPartner solicitud) {
        return publisher.publicar(traducir(solicitud));
    }

    public CrearTrabajoIntegradoCommand traducir(SolicitudTrabajoPartner solicitud) {
        return new CrearTrabajoIntegradoCommand(
                solicitud.id(),
                solicitud.correlationId(),
                solicitud.partnerId(),
                solicitud.externalRequestId(),
                solicitud.clienteId(),
                buscar(CATEGORIAS, solicitud.codigoAsistencia(), "codigo de asistencia"),
                buscar(URGENCIAS, solicitud.codigoPrioridad(), "codigo de prioridad"),
                buscar(CIUDADES, solicitud.codigoCiudad(), "codigo de ciudad"),
                solicitud.pais(),
                solicitud.moneda(),
                solicitud.fechaSolicitud()
        );
    }

    private String buscar(Map<String, String> equivalencias, String valor, String campo) {
        String traducido = equivalencias.get(valor == null ? "" : valor.trim().toUpperCase());
        if (traducido == null) {
            throw new TranslationException(campo + " no soportado: " + valor);
        }
        return traducido;
    }
}
