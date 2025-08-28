package com.cut.cardona.modelo.adopcion;

import com.cut.cardona.modelo.adopcion.enums.SolicitudEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositorioSolicitudAdopcion extends JpaRepository<SolicitudAdopcion, String> {

    @Query("SELECT s FROM SolicitudAdopcion s WHERE s.solicitante.id = :solicitanteId")
    List<SolicitudAdopcion> findBySolicitanteId(@Param("solicitanteId") String solicitanteId);

    @Query("SELECT s FROM SolicitudAdopcion s WHERE s.perro.id = :perroId")
    List<SolicitudAdopcion> findByPerroId(@Param("perroId") String perroId);

    @Query("SELECT s FROM SolicitudAdopcion s WHERE s.estado = :estado")
    List<SolicitudAdopcion> findByEstado(@Param("estado") SolicitudEstado estado);

    @Query("SELECT COUNT(s) FROM SolicitudAdopcion s WHERE s.perro.id = :perroId AND s.estado = com.cut.cardona.modelo.adopcion.enums.SolicitudEstado.ACEPTADA")
    long countAceptadasByPerro(@Param("perroId") String perroId);
}
