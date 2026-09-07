package com.hda.trabajos.api.dto;

import com.hda.trabajos.model.trabajo.Trabajo;

import java.time.Instant;
import java.util.UUID;

/** DTO de lectura expuesto por la API. Nunca se expone la entidad de dominio directamente. */
public record TrabajoDTO(
        UUID id,
        UUID clienteId,
        String categoriaServicio,
        String urgencia,
        String ciudad,
        String origen,
        UUID partnerId,
        String moneda,
        String estado,
        Instant fechaCreacion
) {
    public static TrabajoDTO desde(Trabajo trabajo) {
        return new TrabajoDTO(
                trabajo.getId(),
                trabajo.getClienteId(),
                trabajo.getCategoriaServicio().nombre(),
                trabajo.getUrgencia().name(),
                trabajo.getCiudad(),
                trabajo.getOrigen().name(),
                trabajo.getPartnerId(),
                trabajo.getMoneda().codigoIso4217(),
                trabajo.getEstado().name(),
                trabajo.getFechaCreacion()
        );
    }
}
