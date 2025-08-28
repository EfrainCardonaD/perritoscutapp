package com.cut.cardona.modelo.adopcion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioDocumentoSolicitud extends JpaRepository<DocumentoSolicitud, String> {

    @Query("SELECT COUNT(DISTINCT d.tipoDocumento) FROM DocumentoSolicitud d WHERE d.solicitud.id = :solicitudId")
    long contarTiposPorSolicitud(@Param("solicitudId") String solicitudId);
}

