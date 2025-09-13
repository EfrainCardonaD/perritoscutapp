package com.cut.cardona.modelo.perros;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface RepositorioImagenPerro extends JpaRepository<ImagenPerro, String> {
    List<ImagenPerro> findByPerro_Id(String perroId);

    @Modifying
    @Query("update ImagenPerro i set i.principal=false where i.perro.id=:perroId and i.principal=true")
    int clearPrincipal(@Param("perroId") String perroId);
}
