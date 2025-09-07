package com.cut.cardona.modelo.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositorioResetToken extends JpaRepository<ResetToken, String> {
    Optional<ResetToken> findByToken(String token);
}

