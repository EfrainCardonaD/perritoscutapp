package com.cut.cardona.controllers.service;

import com.cut.cardona.errores.ValidacionDeIntegridad;
import com.cut.cardona.modelo.dto.perfil.DtoPerfilCompleto;
import com.cut.cardona.modelo.dto.registro.DtoRegistroCompletoRequest;
import com.cut.cardona.modelo.dto.registro.DtoRegistroUsuario;
import com.cut.cardona.modelo.dto.registro.DtoValidacionPaso1;
import com.cut.cardona.modelo.dto.registro.DtoValidacionPaso2;
import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Roles;
import com.cut.cardona.modelo.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistroService {

    private final RepositorioUsuario repositorioUsuario;
    private final PasswordEncoder passwordEncoder;
    private final PerfilUsuarioService perfilUsuarioService;
    private final VerificationService verificationService;

    public Map<String, Object> registroBasico(DtoRegistroUsuario registroUsuario) {
        String normalizedEmail = normalizeEmail(registroUsuario.email());
        validarUnicos(registroUsuario.userName(), normalizedEmail, null);
        Usuario usuario = new Usuario();
        usuario.setUserName(registroUsuario.userName());
        usuario.setEmail(normalizedEmail);
        usuario.setPassword(passwordEncoder.encode(registroUsuario.password()));
        usuario.setRol(Roles.ROLE_USER);
        usuario.setActivo(false);
        usuario.setEmailVerificado(false);
        repositorioUsuario.save(usuario);
        // Disparar verificación de email
        verificationService.requestEmailVerification(normalizedEmail);
        return Map.of(
                "userName", usuario.getUsername(),
                "email", usuario.getEmail(),
                "estado", "PENDIENTE_VERIFICACION"
        );
    }

    public DtoPerfilCompleto registroCompleto(DtoRegistroCompletoRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        validarUnicos(request.userName(), normalizedEmail, request.telefono());
        // Usar directamente el DTO unificado
        DtoPerfilCompleto perfil = perfilUsuarioService.registrarUsuarioExtendido(
                new DtoRegistroCompletoRequest(
                        request.userName(),
                        normalizedEmail,
                        request.password(),
                        request.nombreReal(),
                        request.telefono(),
                        request.fechaNacimiento(),
                        request.idioma(),
                        request.zonaHoraria()
                )
        );
        // Disparar verificación de email
        verificationService.requestEmailVerification(normalizedEmail);
        return perfil;
    }

    public DtoPerfilCompleto registroCompletoConImagen(DtoRegistroCompletoRequest request, MultipartFile fotoPerfil) {
        String normalizedEmail = normalizeEmail(request.email());
        validarUnicos(request.userName(), normalizedEmail, request.telefono());
        // Usar directamente el DTO unificado con imagen
        DtoPerfilCompleto perfil = perfilUsuarioService.registrarUsuarioExtendido(
                new DtoRegistroCompletoRequest(
                        request.userName(),
                        normalizedEmail,
                        request.password(),
                        request.nombreReal(),
                        request.telefono(),
                        request.fechaNacimiento(),
                        request.idioma(),
                        request.zonaHoraria()
                ),
                fotoPerfil
        );
        verificationService.requestEmailVerification(normalizedEmail);
        return perfil;
    }

    public void validarPaso1(DtoValidacionPaso1 request) {
        validarUnicos(request.userName(), normalizeEmail(request.email()), null);
    }

    public void validarPaso2(DtoValidacionPaso2 request) {
        if (perfilUsuarioService.existeTelefono(request.telefono())) {
            throw new ValidacionDeIntegridad("Este número de teléfono ya está registrado");
        }
    }

    public Usuario crearUsuarioBasico(DtoRegistroUsuario registroUsuario) {
        String normalizedEmail = normalizeEmail(registroUsuario.email());
        validarUnicos(registroUsuario.userName(), normalizedEmail, null);
        Usuario usuario = new Usuario();
        usuario.setUserName(registroUsuario.userName());
        usuario.setEmail(normalizedEmail);
        usuario.setPassword(passwordEncoder.encode(registroUsuario.password()));
        usuario.setRol(Roles.ROLE_USER);
        usuario.setActivo(false);
        usuario.setEmailVerificado(false);
        usuario.setFechaCreacion(new Timestamp(System.currentTimeMillis()));
        usuario = repositorioUsuario.save(usuario);
        verificationService.requestEmailVerification(normalizedEmail);
        return usuario;
    }

    private void validarUnicos(String userName, String email, String telefono) {
        if (repositorioUsuario.findByUserName(userName).isPresent()) {
            throw new ValidacionDeIntegridad("El nombre de usuario ya está en uso");
        }
        if (repositorioUsuario.findByEmail(email).isPresent()) {
            throw new ValidacionDeIntegridad("El email ya está registrado");
        }
        if (telefono != null && perfilUsuarioService.existeTelefono(telefono)) {
            throw new ValidacionDeIntegridad("Este número de teléfono ya está registrado");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
