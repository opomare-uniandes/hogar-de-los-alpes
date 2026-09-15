package com.hda.trabajos.model.trabajo;

import com.hda.trabajos.model.seedwork.ValueObject;

import java.util.Set;

public record Moneda(String codigoIso4217) implements ValueObject {

    private static final Set<String> SOPORTADAS = Set.of("COP", "MXN", "BRL", "ARS");

    public Moneda {
        if (codigoIso4217 == null || !SOPORTADAS.contains(codigoIso4217)) {
            throw new IllegalArgumentException("Moneda no soportada: " + codigoIso4217);
        }
    }

    public static Moneda cop() {
        return new Moneda("COP");
    }
}
