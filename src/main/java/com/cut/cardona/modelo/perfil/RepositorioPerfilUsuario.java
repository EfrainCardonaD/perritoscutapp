package com.cut.cardona.modelo.perfil;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositorioPerfilUsuario extends JpaRepository<PerfilUsuario, String> {

    Optional<PerfilUsuario> findByUsuarioId(String usuarioId);

    Optional<PerfilUsuario> findByTelefono(String telefono);

    @Query("SELECT p FROM PerfilUsuario p JOIN FETCH p.usuario WHERE p.usuario.id = :usuarioId")
    Optional<PerfilUsuario> findByUsuarioIdWithUsuario(@Param("usuarioId") String usuarioId);

    @Query("SELECT p FROM PerfilUsuario p LEFT JOIN FETCH p.imagenesPerfil WHERE p.usuario.id = :usuarioId")
    Optional<PerfilUsuario> findByUsuarioIdWithImagenes(@Param("usuarioId") String usuarioId);

    boolean existsByTelefono(String telefono);
}
