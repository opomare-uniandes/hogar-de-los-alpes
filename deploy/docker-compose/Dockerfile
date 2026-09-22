# Dockerfile compartido por los servicios de Hogar de los Alpes y su BFF.
#
# El build context es backend/ (ver docker-compose.yml). Cada servicio es su propio
# modulo Gradle bootable (p. ej. :trabajos-application, en trabajos/application/),
# con su propio classpath -- construir uno no compila ni empaqueta los otros tres.
# SERVICE selecciona cual (bff | trabajos | integracion | notificaciones | usuarios |
# proveedor | trabajo-saga).
#
# Patron de 3 etapas inspirado en poc-pulsar-scaling:
#   1) build Gradle    2) extraccion por capas    3) runtime con AOT cache.
ARG SERVICE=trabajos

FROM bellsoft/liberica-openjdk-debian:25 AS gradle-builder
WORKDIR /build
COPY . .
ARG SERVICE
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && \
    ./gradlew --no-daemon ":${SERVICE}-application:bootJar"

FROM bellsoft/liberica-openjre-debian:25-cds AS builder
ARG SERVICE
WORKDIR /builder
COPY --from=gradle-builder /build/${SERVICE}/application/build/libs/${SERVICE}-service.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM bellsoft/liberica-openjre-debian:25-cds
WORKDIR /application
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
ENTRYPOINT ["java", "-jar", "application.jar"]
