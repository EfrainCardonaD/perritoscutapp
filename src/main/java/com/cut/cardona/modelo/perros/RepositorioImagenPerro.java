package com.cut.cardona.modelo.perros;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepositorioImagenPerro extends JpaRepository<ImagenPerro, String> {
    List<ImagenPerro> findByPerro_Id(String perroId);
}

