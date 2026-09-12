package com.hda.notificaciones.usuarioscliente;

import com.hda.notificaciones.model.usuario.ContactoUsuario;
import com.hda.notificaciones.model.usuario.gateways.ConsultaUsuarioGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
