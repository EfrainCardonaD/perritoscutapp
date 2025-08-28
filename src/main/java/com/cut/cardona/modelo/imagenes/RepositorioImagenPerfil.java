package com.cut.cardona.modelo.imagenes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositorioImagenPerfil extends JpaRepository<ImagenPerfil, String> {

    Optional<ImagenPerfil> findByPerfilUsuarioId(String perfilUsuarioId);

    Optional<ImagenPerfil> findByPerfilUsuarioIdAndActivaTrue(String perfilUsuarioId);

    // Consulta corregida usando la relación JPA correcta
    @Query("SELECT i FROM ImagenPerfil i WHERE i.perfilUsuario.usuario.id = :usuarioId AND i.activa = true")
    Optional<ImagenPerfil> findActivaByUsuarioId(@Param("usuarioId") String usuarioId);

    void deleteByPerfilUsuarioId(String perfilUsuarioId);
}
