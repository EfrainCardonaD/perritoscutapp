package com.cut.cardona.modelo.perros;

import com.cut.cardona.modelo.perros.enums.PerroEstadoAdopcion;
import com.cut.cardona.modelo.perros.enums.PerroEstadoRevision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositorioPerro extends JpaRepository<Perro, String> {

    @Query("SELECT p FROM Perro p WHERE p.estadoRevision = com.cut.cardona.modelo.perros.enums.PerroEstadoRevision.APROBADO AND p.estadoAdopcion = com.cut.cardona.modelo.perros.enums.PerroEstadoAdopcion.DISPONIBLE")
    List<Perro> findCatalogoPublico();

    @Query(value = "SELECT p FROM Perro p WHERE p.estadoRevision = com.cut.cardona.modelo.perros.enums.PerroEstadoRevision.APROBADO AND p.estadoAdopcion = com.cut.cardona.modelo.perros.enums.PerroEstadoAdopcion.DISPONIBLE AND (:sexo IS NULL OR p.sexo = :sexo) AND (:tamano IS NULL OR p.tamano = :tamano) AND (:ubicacion IS NULL OR p.ubicacion LIKE CONCAT('%', :ubicacion, '%')) ORDER BY p.fechaPublicacion DESC",
           countQuery = "SELECT COUNT(p) FROM Perro p WHERE p.estadoRevision = com.cut.cardona.modelo.perros.enums.PerroEstadoRevision.APROBADO AND p.estadoAdopcion = com.cut.cardona.modelo.perros.enums.PerroEstadoAdopcion.DISPONIBLE AND (:sexo IS NULL OR p.sexo = :sexo) AND (:tamano IS NULL OR p.tamano = :tamano) AND (:ubicacion IS NULL OR p.ubicacion LIKE CONCAT('%', :ubicacion, '%'))")
    Page<Perro> findCatalogoPublicoFiltrado(@Param("sexo") String sexo,
                                            @Param("tamano") String tamano,
                                            @Param("ubicacion") String ubicacion,
                                            Pageable pageable);

    @Query("SELECT p FROM Perro p WHERE p.usuario.id = :usuarioId")
    List<Perro> findByUsuarioId(@Param("usuarioId") String usuarioId);

    @Query("SELECT p FROM Perro p WHERE p.estadoRevision = com.cut.cardona.modelo.perros.enums.PerroEstadoRevision.PENDIENTE")
    List<Perro> findPendientesRevision();

    @Query("SELECT p FROM Perro p WHERE p.estadoAdopcion = :estado")
    List<Perro> findByEstadoAdopcion(@Param("estado") PerroEstadoAdopcion estado);

    // Nuevo: búsqueda por estado de revisión (derivable pero lo declaramos explícito por consistencia)
    List<Perro> findByEstadoRevision(PerroEstadoRevision estado);
}
