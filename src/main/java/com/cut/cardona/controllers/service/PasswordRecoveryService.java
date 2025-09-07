package com.cut.cardona.controllers.service;

import com.cut.cardona.errores.ValidacionDeIntegridad;
import com.cut.cardona.modelo.usuarios.RepositorioResetToken;
import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.ResetToken;
import com.cut.cardona.modelo.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordRecoveryService {

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioResetToken repositorioResetToken;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    private static final Duration RESET_TTL = Duration.ofHours(1);

    @Transactional
    public void solicitarRecuperacion(String email) {
        if (email == null || email.isBlank()) return;
        String normalized = email.trim().toLowerCase();
        Optional<Usuario> opt = repositorioUsuario.findByEmail(normalized);
        if (opt.isEmpty()) {
            log.info("[FORGOT] Solicitud para email inexistente: {}", normalized);
            return;
        }
        Usuario usuario = opt.get();
        if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            log.info("[FORGOT] Email no verificado: {}", normalized);
            return;
        }
        String token = UUID.randomUUID().toString();
        ResetToken rt = new ResetToken();
        rt.setUsuario(usuario);
        rt.setToken(token);
        rt.setExpiraEn(Timestamp.from(Instant.now().plus(RESET_TTL)));
        repositorioResetToken.save(rt);
        mailService.sendPasswordResetLink(normalized, token);
    }

    @Transactional
    public void resetearConToken(String token, String nuevaPassword) {
        ResetToken rt = repositorioResetToken.findByToken(token)
                .orElseThrow(() -> new ValidacionDeIntegridad("Token inválido o expirado"));
        if (rt.getUsadoEn() != null) {
            throw new ValidacionDeIntegridad("Token ya utilizado");
        }
        if (rt.getExpiraEn() != null && rt.getExpiraEn().before(new Timestamp(System.currentTimeMillis()))) {
            throw new ValidacionDeIntegridad("Token expirado");
        }
        Usuario usuario = rt.getUsuario();
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        rt.setUsadoEn(new Timestamp(System.currentTimeMillis()));
        repositorioUsuario.save(usuario);
        repositorioResetToken.save(rt);
    }
}

