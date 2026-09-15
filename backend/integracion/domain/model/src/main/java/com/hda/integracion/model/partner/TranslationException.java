package com.hda.integracion.model.partner;

/** Error semantico de un contrato externo que la ACL no puede traducir. */
public class TranslationException extends IllegalArgumentException {
    public TranslationException(String message) {
        super(message);
    }
}
