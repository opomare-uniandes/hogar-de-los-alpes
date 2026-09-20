package com.hda.proveedor.r2dbc;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Repositorio tecnico de Spring Data R2DBC. Solo lo usa el adaptador (ProveedorRepositoryAdapter). */
public interface ProveedorR2dbcRepository extends ReactiveCrudRepository<ProveedorEntity, UUID> {

    Mono<ProveedorEntity> findBySagaIdReserva(UUID sagaIdReserva);

    /**
     * Reserva atomica: UPDATE ... WHERE disponible = TRUE ... RETURNING, en una sola sentencia.
     * Tambien resuelve, de paso, lo que se buscaba con la sección 7: la comparacion es
     * insensible a mayusculas/minusculas y a tildes (TRANSLATE, funcion nativa de Postgres, sin
     * extension unaccent) - un trabajo creado con "Bogotá" (con tilde) encuentra igual el
     * proveedor sembrado como "Bogota".
     * Postgres re-evalua el WHERE contra el estado ya comprometido de la fila al tomar el lock
     * de escritura, asi que si dos ejecuciones concurrentes intentan reservar el mismo proveedor,
     * la que pierde la carrera encuentra disponible = FALSE y no actualiza ninguna fila (Mono
     * vacio) - a diferencia de "SELECT disponible, despues UPDATE" en dos pasos separados, donde
     * ambas ejecuciones podian leer "disponible" antes de que cualquiera escribiera.
     * Sin LIMIT: valido mientras el seed garantice a lo sumo un proveedor por categoria+ciudad
     * (seccion 5.2 del plan) - Postgres no soporta LIMIT en UPDATE directamente.
     */
    @Query("""
            UPDATE proveedor
            SET disponible = FALSE, saga_id_reserva = :sagaId
            WHERE LOWER(TRANSLATE(categoria_servicio, 'ÁÉÍÓÚÑáéíóúñ', 'AEIOUNaeioun'))
                  = LOWER(TRANSLATE(:categoriaServicio, 'ÁÉÍÓÚÑáéíóúñ', 'AEIOUNaeioun'))
              AND LOWER(TRANSLATE(ciudad, 'ÁÉÍÓÚÑáéíóúñ', 'AEIOUNaeioun'))
                  = LOWER(TRANSLATE(:ciudad, 'ÁÉÍÓÚÑáéíóúñ', 'AEIOUNaeioun'))
              AND disponible = TRUE
            RETURNING *
            """)
    Mono<ProveedorEntity> reservarSiDisponible(@Param("categoriaServicio") String categoriaServicio,
                                                @Param("ciudad") String ciudad,
                                                @Param("sagaId") UUID sagaId);
}
