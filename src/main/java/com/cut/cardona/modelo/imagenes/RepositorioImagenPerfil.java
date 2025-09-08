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

    // Nueva: traer imagen + perfil + usuario para autorización
    @Query("SELECT i FROM ImagenPerfil i JOIN FETCH i.perfilUsuario pu JOIN FETCH pu.usuario u WHERE i.id = :id")
    Optional<ImagenPerfil> findByIdWithUsuario(@Param("id") String id);

    void deleteByPerfilUsuarioId(String perfilUsuarioId);
}
