# Dockerfile compartido por los 4 servicios de Hogar de los Alpes.
#
# El build context es backend/ (ver docker-compose.yml). El modulo :applications
# produce los 4 jars (trabajos/integracion/notificaciones/usuarios) en un solo
# directorio; se selecciona cual queda como application.jar via el arg SERVICE_JAR.
#
# Patron de 3 etapas inspirado en poc-pulsar-scaling:
#   1) build Gradle    2) extraccion por capas    3) runtime con AOT cache.
ARG SERVICE_JAR=trabajos-service.jar

FROM bellsoft/liberica-openjdk-debian:25 AS gradle-builder
WORKDIR /build
COPY . .
RUN chmod +x gradlew && \
    ./gradlew --no-daemon \
      :applications:bootJarTrabajos \
      :applications:bootJarIntegracion \
      :applications:bootJarNotificaciones \
      :applications:bootJarUsuarios

FROM bellsoft/liberica-openjre-debian:25-cds AS builder
ARG SERVICE_JAR
WORKDIR /builder
COPY --from=gradle-builder /build/applications/build/libs/${SERVICE_JAR} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM bellsoft/liberica-openjre-debian:25-cds
WORKDIR /application
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
ENTRYPOINT ["java", "-jar", "application.jar"]
