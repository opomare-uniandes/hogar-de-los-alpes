package com.hda.notificaciones.usuarioscliente;

import com.hda.notificaciones.model.usuario.ContactoUsuario;
import com.hda.notificaciones.model.usuario.gateways.ConsultaUsuarioGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Implementa ConsultaUsuarioGateway con una llamada HTTP a usuarios-service. Es el
 * unico punto sincrono de todo el sistema (ver README): una consulta de solo lectura,
 * no un comando - el resto de la comunicacion entre servicios sigue siendo 100% por
 * eventos sobre Pulsar.
 *
 * ContactoUsuarioDTO de usuarios-service (correo/celular/notificarPorEmail/
 * notificarPorWhatsapp) tiene exactamente los mismos campos que ContactoUsuario aqui,
 * asi que se deserializa directo a el sin un DTO intermedio.
 */
@Component
public class UsuarioHttpClientAdapter implements ConsultaUsuarioGateway {

    private final WebClient webClient;

    public UsuarioHttpClientAdapter(WebClient.Builder webClientBuilder,
                                     @Value("${hda.usuarios-service.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Mono<ContactoUsuario> obtenerContacto(String clienteId) {
        return webClient.get()
                .uri("/usuarios/{clienteId}/contacto", clienteId)
                .retrieve()
                .bodyToMono(ContactoUsuario.class);
    }
}
