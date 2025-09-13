package com.cut.cardona.service.auth;

import com.cut.cardona.errores.ValidacionDeIntegridad;
import com.cut.cardona.service.infra.MailService;
import com.cut.cardona.modelo.usuarios.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class VerificationService {

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioVerificationToken repositorioVerificationToken;
    private final MailService mailService;

    private static final Duration EMAIL_TOKEN_TTL = Duration.ofHours(1);

    @Transactional
    public void requestEmailVerification(String email) {
        if (email == null || email.isBlank()) return;
        String normalized = email.trim().toLowerCase();
        Optional<Usuario> optUser = repositorioUsuario.findByEmail(normalized);
        if (optUser.isEmpty()) {
            // Respuesta genérica: no revelar existencia
            log.info("[VERIFY] Solicitud verificación para email inexistente: {}", normalized);
            return;
        }
        Usuario usuario = optUser.get();
        if (Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            log.info("[VERIFY] Email ya verificado: {}", normalized);
            return;
        }
        // invalidar previos no usados
        repositorioVerificationToken.deleteByUsuario_IdAndCanalAndUsadoEnIsNull(usuario.getId(), VerificationChannel.EMAIL);
        // generar nuevo token y enviar
        String token = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken();
        vt.setUsuario(usuario);
        vt.setCanal(VerificationChannel.EMAIL);
        vt.setToken(token);
        vt.setExpiraEn(Timestamp.from(Instant.now().plus(EMAIL_TOKEN_TTL)));
        repositorioVerificationToken.save(vt);
        mailService.sendEmailVerificationLink(normalized, token);
    }

    @Transactional
    public void confirmEmail(String token) {
        VerificationToken vt = repositorioVerificationToken.findByToken(token)
                .orElseThrow(() -> new ValidacionDeIntegridad("Token inválido o expirado"));
        if (vt.getUsadoEn() != null) {
            throw new ValidacionDeIntegridad("Token ya utilizado");
        }
        if (vt.getExpiraEn() != null && vt.getExpiraEn().before(new Timestamp(System.currentTimeMillis()))) {
            throw new ValidacionDeIntegridad("Token expirado");
        }
        Usuario usuario = vt.getUsuario();
        usuario.setEmailVerificado(true);
        usuario.setActivo(true); // Política: con email verificado, activar
        vt.setUsadoEn(new Timestamp(System.currentTimeMillis()));
        repositorioUsuario.save(usuario);
        repositorioVerificationToken.save(vt);
    }

    @Transactional
    public void resendEmail(String email) {
        requestEmailVerification(email);
    }
}

