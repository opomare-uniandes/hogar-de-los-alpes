# Docker Compose — entorno completo local

Levanta PostgreSQL, Pulsar, Redis, los seis servicios de negocio y `bff-service`.
El BFF es el único punto de entrada HTTP usado por el frontend y por Postman.

## Ejecución

```bash
cd deploy/docker-compose
cp .env.example .env
docker compose up -d --build
```

Docker Compose inicializa las cuatro bases separadas, crea el tenant y los namespaces
de Pulsar y construye los seis servicios de negocio más el BFF con Java 25. El servicio
`postgres-bootstrap` crea únicamente las bases ausentes, incluso con un volumen antiguo.

## Acceso

| Componente | Dirección desde el host | Uso |
| --- | --- | --- |
| BFF | `http://localhost:8090` | Único API público |
| Pulsar Admin | `http://localhost:8087` | Administración del broker; el puerto se puede cambiar con `PULSAR_ADMIN_PORT` |
| Trabajos | `http://localhost:8081` | Diagnóstico interno |
| Integración | `http://localhost:8082` | Consumidor interno |
| Notificaciones | `http://localhost:8083` | Consumidor interno |
| Usuarios | `http://localhost:8084` | Consulta interna |
| Proveedor | `http://localhost:8085` | Participante interno de saga |
| Trabajo Saga | `http://localhost:8086` | Saga Log interno |

Aunque los puertos internos se publican para facilitar la demostración local, clientes y
frontend deben usar únicamente `http://localhost:8090`.

## Prueba rápida

```bash
curl http://localhost:8090/actuator/health

curl -X POST http://localhost:8090/api/v1/trabajos \
  -H 'Content-Type: application/json' \
  -d '{"clienteId":"11111111-1111-1111-1111-111111111113","categoriaServicio":"plomeria","urgencia":"MEDIA","ciudad":"Bogota","origen":"MARKETPLACE","partnerId":null,"moneda":"COP"}'
```

Para probar los caminos exitoso y compensado completos, importe la colección de
[`../../docs/postman`](../../docs/postman/README.md).

## Reinicializar datos

Los scripts de `postgres-init/` solo se ejecutan al crear el volumen. Para volver a las
semillas deterministas:

```bash
docker compose down -v
docker compose up -d --build
```

Este comando borra únicamente los volúmenes locales de esta composición.
