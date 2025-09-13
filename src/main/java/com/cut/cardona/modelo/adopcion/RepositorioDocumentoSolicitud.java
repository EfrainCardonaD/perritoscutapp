package com.cut.cardona.modelo.adopcion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositorioDocumentoSolicitud extends JpaRepository<DocumentoSolicitud, String> {

    @Query("SELECT COUNT(DISTINCT d.tipoDocumento) FROM DocumentoSolicitud d WHERE d.solicitud.id = :solicitudId")
    long contarTiposPorSolicitud(@Param("solicitudId") String solicitudId);

    List<DocumentoSolicitud> findBySolicitud_Id(String solicitudId);

    @Query("SELECT COUNT(d) FROM DocumentoSolicitud d WHERE d.solicitud.id = :solicitudId AND LOWER(d.tipoDocumento) = LOWER(:tipo)")
    long existeTipoEnSolicitud(@Param("solicitudId") String solicitudId, @Param("tipo") String tipo);
}
