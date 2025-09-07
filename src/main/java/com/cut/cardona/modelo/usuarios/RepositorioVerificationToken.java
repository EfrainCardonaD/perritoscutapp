package com.cut.cardona.modelo.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositorioVerificationToken extends JpaRepository<VerificationToken, String> {
    Optional<VerificationToken> findByToken(String token);
    long deleteByUsuario_IdAndCanalAndUsadoEnIsNull(String usuarioId, VerificationChannel canal);
}
