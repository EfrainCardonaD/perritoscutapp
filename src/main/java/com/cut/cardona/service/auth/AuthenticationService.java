package com.cut.cardona.service.auth;
import com.cut.cardona.service.perfil.PerfilService;
import com.cut.cardona.modelo.dto.auth.AuthenticationRequest;
import com.cut.cardona.modelo.dto.auth.AuthenticationResponse;
import com.cut.cardona.modelo.dto.usuarios.DtoUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.security.CustomUserDetails;
import com.cut.cardona.security.TokenService;
import com.cut.cardona.errores.ValidacionDeIntegridad;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final RepositorioUsuario repositorioUsuario;
    private final PerfilService perfilService;

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    request.userName(),
                    request.password()
            );

            Authentication authentication = authenticationManager.authenticate(authToken);
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Usuario usuario = userDetails.getUsuario();

            String jwtToken = tokenService.generarToken(usuario);
            String refreshToken = tokenService.generarRefreshToken(usuario);

            updateLastAccess(usuario);

            DtoUsuario userDto = perfilService.toDto(usuario);

            // ✅ Usar constructor que incluye mensaje de éxito
            return new AuthenticationResponse(
                    jwtToken,
                    refreshToken,
                    userDto,
                    tokenService.getExpirationTime(),
                    true,
                    "¡Bienvenido " + usuario.getUsername() + "! Has iniciado sesión exitosamente."
            );


        } catch (DisabledException e) {
            log.warn("Authentication blocked (disabled/unverified) for user: {}", request.userName());
            throw new ValidacionDeIntegridad(" Verifica tu correo para iniciar sesión");
        } catch (BadCredentialsException e) {
            log.warn("Failed authentication attempt for user: {}", request.userName());
            throw new ValidacionDeIntegridad(" Las credenciales proporcionadas son incorrectas. Verifica tu usuario y contraseña.");
        }
    }

    public AuthenticationResponse refreshToken(String refreshToken) {
        try {
            // Validar el refresh token
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                throw new ValidacionDeIntegridad("Refresh token requerido");
            }

            // Extraer el username del token - PUEDE FALLAR SI TOKEN ES INVÁLIDO
            String username;
            try {
                username = tokenService.getSubject(refreshToken);
                if (username == null || username.trim().isEmpty()) {
                    throw new ValidacionDeIntegridad("Token inválido o expirado");
                }
            } catch (RuntimeException e) {
                // Token malformado o expirado
                log.warn("Invalid refresh token format: {}", e.getMessage());
                throw new ValidacionDeIntegridad("Refresh token inválido o expirado");
            }

            // Buscar el usuario
            Usuario usuario = repositorioUsuario.findByUserName(username)
                    .orElseThrow(() -> new ValidacionDeIntegridad("Usuario no encontrado"));

            // Verificar que el usuario esté activo y verificado
            if (!Boolean.TRUE.equals(usuario.getActivo()) || !Boolean.TRUE.equals(usuario.getEmailVerificado())) {
                throw new ValidacionDeIntegridad("Verifica tu correo para iniciar sesión");
            }

            // ✅ NUEVA VALIDACIÓN: Verificar que no haya intentos fallidos recientes
            // Si el refresh token es el mismo que está almacenado en BD, significa que no ha habido login exitoso reciente
            if (refreshToken.equals(usuario.getToken())) {
                log.warn("Refresh token attempt with potentially stale token for user: {}", username);
                throw new ValidacionDeIntegridad("Sesión expirada, inicie sesión nuevamente");
            }

            // Generar nuevo token de acceso
            String newAccessToken = tokenService.generarToken(usuario);

            // Actualizar último acceso
            updateLastAccess(usuario);

            // Obtener DTO del usuario
            DtoUsuario userDto = perfilService.toDto(usuario);

            return new AuthenticationResponse(
                    newAccessToken,
                    refreshToken,
                    userDto,
                    tokenService.getExpirationTime()
            );

        } catch (ValidacionDeIntegridad e) {
            // Re-lanzar las validaciones específicas sin cambiar el mensaje
            log.warn("Refresh token validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // Solo para errores inesperados del sistema
            log.error("Unexpected error during refresh token processing: {}", e.getMessage(), e);
            throw new ValidacionDeIntegridad("Error interno del servidor");
        }
    }

    public void logout(String token) {
        // Implementar lógica de logout (blacklist del token)
        tokenService.revokeToken(token);
    }

    private void updateLastAccess(Usuario usuario) {
        usuario.setUltimoAcceso(
                Timestamp.from(ZonedDateTime.now(ZoneId.of("America/Mexico_City")).toInstant())
        );
        repositorioUsuario.save(usuario);
    }
}