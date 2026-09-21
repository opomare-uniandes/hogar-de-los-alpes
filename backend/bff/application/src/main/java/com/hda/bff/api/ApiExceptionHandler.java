package com.hda.bff.api;

import com.hda.bff.client.UpstreamException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.netty.http.client.PrematureCloseException;

import java.time.Instant;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UpstreamException.class)
    ResponseEntity<ApiError> upstream(UpstreamException exception) {
        HttpStatus status = exception.status().value() == 404
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), status.getReasonPhrase(), exception.getMessage(), exception.service()));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    ResponseEntity<ApiError> validation(WebExchangeBindException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Solicitud inválida");
        return ResponseEntity.badRequest().body(new ApiError(
                Instant.now(), 400, "Bad Request", detail, "bff-service"));
    }

    @ExceptionHandler(ServerWebInputException.class)
    ResponseEntity<ApiError> malformedRequest(ServerWebInputException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                Instant.now(), 400, "Bad Request", "El cuerpo o uno de sus valores no es válido", "bff-service"));
    }

    @ExceptionHandler({WebClientRequestException.class, TimeoutException.class, PrematureCloseException.class})
    ResponseEntity<ApiError> unavailable(Exception exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(
                Instant.now(), 503, "Service Unavailable",
                "Un servicio interno no está disponible temporalmente", "bff-service"));
    }

    public record ApiError(Instant timestamp, int status, String error, String detail, String source) {
    }
}
