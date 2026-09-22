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

La comunicación de la saga es asíncrona. Al ejecutar la colección completa, las peticiones
de seguimiento consultan de nuevo hasta alcanzar un estado terminal (máximo 12 intentos)
y verifican tanto el estado de la saga como el del trabajo. Un estado intermedio no se
cuenta como éxito. Al enviar una petición individual en Postman, espere y repita la
consulta de seguimiento manualmente.

El proveedor semilla de `plomeria` en `Bogota` queda reservado después de la primera
saga exitosa. Por eso la colección completa está diseñada para ejecutarse sobre datos
de demostración limpios. Si ya ejecutó el caso exitoso, siga la reinicialización
acotada del entorno de prueba indicada en [`../../deploy/codespaces/README.md`](../../deploy/codespaces/README.md)
antes de repetirlo; de lo contrario, el camino exitoso fallará correctamente.

## Ejecución por línea de comandos

Con Newman instalado:

```bash
newman run docs/postman/HogarDeLosAlpes-BFF.postman_collection.json \
  -e docs/postman/Local.postman_environment.json
```

El contrato completo está en [`../api/openapi.yaml`](../api/openapi.yaml).
