# Despliegue demostrable sin costo con GitHub Codespaces

Esta alternativa ejecuta PostgreSQL, Redis, Apache Pulsar, los seis servicios de
negocio y el BFF dentro de un Codespace. El puerto público `8090` es la única entrada
externa; los servicios de dominio continúan comunicándose dentro de la red de Docker.

## Alcance y costo

Las cuentas personales de GitHub incluyen una cuota mensual de Codespaces. Una
máquina de 2 cores consume aproximadamente dos core-hours por cada hora encendida.
Cuando una cuenta sin método de pago agota su cuota, GitHub bloquea nuevo consumo en
lugar de facturarlo. Confirme siempre la cuota disponible en **Settings > Billing**.

Esta ruta está pensada para demostración académica: la URL deja de responder cuando
el Codespace se detiene. No representa un despliegue productivo de alta disponibilidad.

## Puesta en marcha

1. Abra el repositorio en GitHub.
2. Seleccione **Code > Codespaces > Create codespace** sobre la rama de la entrega.
3. Cuando VS Code web termine de preparar el contenedor, ejecute:

   ```bash
   ./deploy/codespaces/start.sh
   ```

4. El script construye el sistema, espera la salud del BFF, publica el puerto y muestra
   una URL similar a:

   ```text
   https://<codespace>-8090.app.github.dev
   ```

   El despliegue usa el proyecto Docker Compose `hda-codespaces` y volúmenes propios,
   por lo que no modifica los datos de otro entorno local del repositorio.

5. Abra `<URL>/actuator/health` y espere la respuesta `UP`.
6. Ejecute la colección de `docs/postman` asignando esa URL a `baseUrl`.

Si GitHub mantiene el puerto privado, abra la pestaña **Ports**, haga clic derecho
sobre `8090`, seleccione **Port Visibility > Public** y vuelva a copiar la URL.

## Detener el consumo

```bash
./deploy/codespaces/stop.sh
```

Después, use **Codespaces: Stop Current Codespace** o deténgalo desde GitHub. Cerrar
la pestaña del navegador no necesariamente detiene la máquina.

## Límites deliberados

- El archivo `docker-compose.codespaces.yml` limita la memoria de cada JVM y de Pulsar
  para caber en el tamaño gratuito.
- Solo el BFF se publica. PostgreSQL, Redis, Pulsar y los servicios de dominio quedan
  internos.
- La evidencia debe registrar la URL, fecha de ejecución y resultado de Postman, pero
  no prometer disponibilidad permanente.
