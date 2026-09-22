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

## URL pública verificada

```text
Base URL: https://hda-entrega5-bff-5gr56g469x67h45wp-8090.app.github.dev
Health:   https://hda-entrega5-bff-5gr56g469x67h45wp-8090.app.github.dev/actuator/health
```

La URL fue verificada el **21 de septiembre de 2026** mediante `curl` y Newman:
`8` solicitudes de la colección más `3` sondeos de seguimiento, `14` aserciones y
`0` fallos. Las aserciones esperan estados terminales: el camino exitoso dejó el trabajo
`ASIGNADO` y registró los tres pasos de la saga; el camino alterno terminó
`CANCELADA` con la compensación `COMPENSAR_CANCELAR_TRABAJO=OK`.

> Codespaces es un entorno académico temporal. El enlace responde mientras el Codespace
> `hda-entrega5-bff-5gr56g469x67h45wp` permanezca encendido. Si GitHub lo detuvo por
> inactividad, inícielo nuevamente antes de la evaluación; la URL se conserva mientras
> exista el mismo Codespace.
> El proveedor de Bogotá queda reservado después de una saga exitosa. Para repetir la
> colección, reinicialice solamente los datos de prueba de `hda-codespaces` siguiendo
> [la guía de Codespaces](../../deploy/codespaces/README.md).

## Lista de evidencia para el video

- [x] Mostrar los siete contenedores de aplicación disponibles con
      `docker compose ps` (seis servicios de negocio y el BFF).
- [x] Mostrar que solo el BFF tiene una ruta pública.
- [x] Ejecutar `GET /actuator/health` contra la URL pública de Codespaces.
- [x] Ejecutar la carpeta de saga exitosa en Postman/Newman.
- [x] Consultar el Saga Log y sus pasos.
- [x] Ejecutar el camino compensado y mostrar `CANCELADA`.
- [x] Mostrar que Postman y el navegador solo conocen la URL del BFF.
- [x] Exportar nuevamente el ambiente Postman con la URL definitiva.

## Criterio de cierre

El despliegue se considera demostrado únicamente cuando la colección pasa contra la URL
pública. Tener scripts o contenedores creados, sin una interacción exitosa por el BFF, no
es evidencia suficiente para la rúbrica. Al terminar la demostración se detiene el
Codespace para conservar la cuota gratuita.
