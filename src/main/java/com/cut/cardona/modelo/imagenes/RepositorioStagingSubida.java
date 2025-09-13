package com.cut.cardona.modelo.imagenes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RepositorioStagingSubida extends JpaRepository<StagingSubida, String> {
    // Imágenes huérfanas: no asociadas y con antigüedad mayor a X horas
    @Query("SELECT s FROM StagingSubida s WHERE s.asociado = false AND s.createdAt < :umbral")
    List<StagingSubida> findOrphans(@Param("umbral") Instant umbral);
}

