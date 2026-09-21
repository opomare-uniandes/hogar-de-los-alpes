package com.hda.trabajos.usecase.asignartrabajo;

import java.util.UUID;

public record AsignarTrabajoCommand(UUID trabajoId, UUID sagaId) {
}
