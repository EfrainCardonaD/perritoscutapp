package com.cut.cardona.controllers.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Value("${app.mail.from:}")
    private String mailFrom;

    public void sendEmailVerificationLink(String toEmail, String token) {
        String url = buildUrl("/verify-email", token);
        String subject = "Verifica tu correo";
        String body = "Hola,\n\n" +
                "Para verificar tu correo, haz clic en el siguiente enlace (válido por 1 hora):\n" +
                url + "\n\n" +
                "Si no solicitaste esto, ignora este mensaje.";
        send(toEmail, subject, body, token, "Verificación de email");
    }

    public void sendPasswordResetLink(String toEmail, String token) {
        String url = buildUrl("/reset", token);
        String subject = "Restablecer contraseña";
        String body = "Hola,\n\n" +
                "Para restablecer tu contraseña, usa el siguiente enlace (válido por 1 hora):\n" +
                url + "\n\n" +
                "Si no solicitaste esto, ignora este mensaje.";
        send(toEmail, subject, body, token, "Reset de contraseña");
    }

    private void send(String toEmail, String subject, String body, String token, String logPrefix) {
        if (!mailEnabled) {
            log.info("[MAIL][DEV-OFF] {} para {} -> token: {}", logPrefix, toEmail, token);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            if (mailFrom != null && !mailFrom.isBlank()) {
                msg.setFrom(mailFrom);
            }
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("[MAIL] {} enviado a {}", logPrefix, toEmail);
        } catch (Exception e) {
            log.error("[MAIL][ERROR] No se pudo enviar {} a {}: {}", logPrefix, toEmail, e.getMessage(), e);
        }
    }


    private String buildUrl(String path, String token) {
        String url = "http://192.168.100.2:3000";
        String base = trimTrailingSlash(url);
        if (!path.startsWith("/")) path = "/" + path;
        return base + path + "?token=" + token;
    }

    private String trimTrailingSlash(String s) {
        if (s == null || s.isBlank()) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
