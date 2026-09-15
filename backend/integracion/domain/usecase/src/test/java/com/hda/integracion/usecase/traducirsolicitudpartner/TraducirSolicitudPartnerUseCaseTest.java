package com.hda.integracion.usecase.traducirsolicitudpartner;

import com.hda.integracion.model.partner.SolicitudTrabajoPartner;
import com.hda.integracion.model.partner.TranslationException;
import com.hda.integracion.model.trabajo.CrearTrabajoIntegradoCommand;
import com.hda.integracion.model.trabajo.gateways.CrearTrabajoCommandPublisher;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TraducirSolicitudPartnerUseCaseTest {

    private final TraducirSolicitudPartnerUseCase useCase = new TraducirSolicitudPartnerUseCase(new NoOpPublisher());

    @Test
    void v1Yv2ConElMismoSignificadoProducenElMismoComandoCanonico() {
        CrearTrabajoIntegradoCommand desdeV1 = useCase.traducir(solicitud("v1"));
        CrearTrabajoIntegradoCommand desdeV2 = useCase.traducir(solicitud("v2"));

        assertEquals(desdeV1, desdeV2);
        assertEquals("PLOMERIA", desdeV1.categoriaServicio());
        assertEquals("CRITICA", desdeV1.urgencia());
        assertEquals("BOGOTA", desdeV1.ciudad());
    }

    @Test
    void rechazaUnCodigoExternoQueNoTieneEquivalenciaDeDominio() {
        SolicitudTrabajoPartner invalida = new SolicitudTrabajoPartner(
                "evento-1", "correlacion-1", "v1", "partner-1", "request-1", "cliente-1",
                "UNKNOWN", "P1", "BOG", "CO", "COP", Instant.parse("2026-01-01T00:00:00Z"));

        assertThrows(TranslationException.class, () -> useCase.traducir(invalida));
    }

    private SolicitudTrabajoPartner solicitud(String version) {
        return new SolicitudTrabajoPartner(
                "evento-1", "correlacion-1", version, "partner-1", "request-1", "cliente-1",
                "PLUMBING", "P1", "BOG", "CO", "COP", Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static class NoOpPublisher implements CrearTrabajoCommandPublisher {
        @Override
        public Mono<Void> publicar(CrearTrabajoIntegradoCommand comando) {
            return Mono.empty();
        }
    }
}
