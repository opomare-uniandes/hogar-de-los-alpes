package com.hda.trabajos.usecase.cancelartrabajo;

import java.util.UUID;

public record CancelarTrabajoCommand(UUID trabajoId, UUID sagaId, String motivo) {
}
