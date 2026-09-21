package com.hda.bff.client;

import org.springframework.http.HttpStatusCode;

public class UpstreamException extends RuntimeException {

    private final String service;
    private final HttpStatusCode status;

    public UpstreamException(String service, HttpStatusCode status, String detail) {
        super(detail == null || detail.isBlank() ? "El servicio no pudo procesar la solicitud" : detail);
        this.service = service;
        this.status = status;
    }

    public String service() {
        return service;
    }

    public HttpStatusCode status() {
        return status;
    }
}
