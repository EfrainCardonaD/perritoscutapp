package com.cut.cardona.security;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor // ✅ Constructor injection automático
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    // ✅ Constructor injection - inmutable y testeable
    private final RepositorioUsuario usuarioRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        Usuario usuario = usuarioRepository.findByUserNameOrEmail(username, username).orElse(null);
        if (usuario != null) {
            usuario.setUltimoAcceso(Timestamp.from(ZonedDateTime.now(ZoneId.of("America/Mexico_City")).toInstant()));
            usuarioRepository.save(usuario);
        }
        response.sendRedirect("/index");
    }
}
