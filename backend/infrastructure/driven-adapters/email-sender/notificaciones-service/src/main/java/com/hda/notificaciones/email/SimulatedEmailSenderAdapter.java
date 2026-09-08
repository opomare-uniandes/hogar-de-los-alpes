package com.hda.notificaciones.email;

import com.hda.notificaciones.model.notificacion.gateways.EmailSender;
import com.hda.notificaciones.model.trabajocreado.TrabajoCreadoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SimulatedEmailSenderAdapter implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SimulatedEmailSenderAdapter.class);

    @Override
    public Mono<String> enviar(TrabajoCreadoEvento evento) {
        String destinatario = "cliente-" + evento.clienteId() + "@hda-trabajos.test";

        log.info("[EMAIL SIMULADO] Para: {} | Asunto: Tu trabajo {} fue creado | "
                        + "Categoria: {} | Ciudad: {} | Urgencia: {}",
                destinatario, evento.trabajoId(), evento.categoriaServicio(),
                evento.ciudad(), evento.urgencia());

        return Mono.just(destinatario);
    }
}
