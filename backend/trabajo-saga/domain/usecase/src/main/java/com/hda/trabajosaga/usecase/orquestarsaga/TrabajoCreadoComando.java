package com.hda.trabajosaga.usecase.orquestarsaga;

import java.util.UUID;

public record TrabajoCreadoComando(UUID trabajoId, UUID clienteId, String categoriaServicio, String ciudad) {
}
