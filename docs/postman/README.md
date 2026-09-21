# Colección Postman del BFF

Esta carpeta contiene la colección evaluable para interactuar con Hogar de los Alpes
exclusivamente por medio del BFF. No es necesario conocer las direcciones internas de
los seis servicios de negocio.

## Importación

1. Importe `HogarDeLosAlpes-BFF.postman_collection.json` en Postman.
2. Importe `Local.postman_environment.json` o `Cloud.postman_environment.json`.
3. El ambiente Cloud ya contiene la URL del Codespace verificado. Si se crea otro
   Codespace, reemplace `baseUrl` con la nueva URL pública; no agregue `/` al final.
4. Ejecute primero `00 - Disponibilidad` y después las carpetas numeradas.

## Demostración de la saga

- **Camino exitoso:** `plomeria` en `Bogota` coincide con un proveedor semilla. El estado
  terminal esperado es `COMPLETADA` y el trabajo termina `ASIGNADO`.
- **Camino compensado:** `plomeria` en `Medellin` no tiene proveedor semilla. El estado
  terminal esperado es `CANCELADA` y el trabajo termina `CANCELADO`.

La comunicación de la saga es asíncrona. Si la primera consulta devuelve
`SAGA_PENDIENTE` o un estado intermedio, espere entre 3 y 5 segundos y repita únicamente
la petición de seguimiento. Esto no representa un error: demuestra consistencia eventual.

## Ejecución por línea de comandos

Con Newman instalado:

```bash
newman run docs/postman/HogarDeLosAlpes-BFF.postman_collection.json \
  -e docs/postman/Local.postman_environment.json
```

El contrato completo está en [`../api/openapi.yaml`](../api/openapi.yaml).
