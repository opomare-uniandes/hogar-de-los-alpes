# Evidencia de presentación y despliegue

La plataforma elegida para la demostración académica es **GitHub Codespaces**. Dentro
del Codespace se ejecuta el sistema completo con Docker Compose y se publica únicamente
el puerto `8090` del BFF. Esta decisión evita costos para el equipo y mantiene el mismo
límite externo definido para nube: ningún servicio de dominio se expone directamente.

La URL es temporal y responde mientras el Codespace permanezca encendido. La guía de
operación está en [`../../deploy/codespaces`](../../deploy/codespaces/README.md).

## Artefactos entregados

- BFF: `backend/bff/application`.
- Contrato: `docs/api/openapi.yaml`.
- Colección y ambientes: `docs/postman/`.
- Despliegue demostrable sin costo: `.devcontainer` y `deploy/codespaces`.
- Alternativa con infraestructura administrada: `deploy/terraform` y `deploy/k8s-cloud`
  (no requerida para la demostración y no desplegada para evitar costos).

## URL pública

> Completar al encender el Codespace y verificar la colección. No se debe versionar una
> URL antigua como si fuera permanentemente disponible.

```text
Base URL: https://REEMPLAZAR-CODESPACE-8090.app.github.dev
Health:   https://REEMPLAZAR-CODESPACE-8090.app.github.dev/actuator/health
```

No se debe reemplazar este marcador hasta verificar la URL mediante `curl` y Postman.

## Lista de evidencia para el video

- [ ] Mostrar los siete contenedores de aplicación disponibles con
      `docker compose ps` (seis servicios de negocio y el BFF).
- [ ] Mostrar que solo el BFF tiene una ruta pública.
- [ ] Ejecutar `GET /actuator/health` contra la URL pública de Codespaces.
- [ ] Ejecutar la carpeta de saga exitosa en Postman.
- [ ] Consultar el Saga Log y sus pasos.
- [ ] Ejecutar el camino compensado y mostrar `CANCELADA`.
- [ ] Mostrar que Postman y el navegador solo conocen la URL del BFF.
- [ ] Exportar nuevamente el ambiente Postman con la URL definitiva.

## Criterio de cierre

El despliegue se considera demostrado únicamente cuando la colección pasa contra la URL
pública. Tener scripts o contenedores creados, sin una interacción exitosa por el BFF, no
es evidencia suficiente para la rúbrica. Al terminar la demostración se detiene el
Codespace para conservar la cuota gratuita.
