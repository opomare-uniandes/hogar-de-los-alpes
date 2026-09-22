package com.hda.bff.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CrearTrabajoRequest(
        @NotNull UUID clienteId,
        @NotBlank String categoriaServicio,
        @NotBlank String urgencia,
        @NotBlank String ciudad,
        @NotBlank String origen,
        UUID partnerId,
        @NotBlank String moneda
) {
}
