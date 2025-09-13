package com.cut.cardona.controllers.service;
import com.cut.cardona.modelo.dto.usuarios.DtoUsuario;
import com.cut.cardona.modelo.dto.usuarios.DtoUsuarioDetalle;
import com.cut.cardona.modelo.dto.usuarios.DtoUsuarioResumen;
import com.cut.cardona.modelo.imagenes.ImagenPerfil;
import com.cut.cardona.modelo.imagenes.RepositorioImagenPerfil;
import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioImagenPerfil repositorioImagenPerfil;

    // Conversión existente usada por AuthenticationService
    public DtoUsuario toDto(Usuario usuario) {
        return new DtoUsuario(usuario);
    }

    // --- DTO Resumen (userName, email, fotoPerfilUrl) ---
    public DtoUsuarioResumen toResumen(Usuario usuario) {
        if (usuario == null) return null;
        String foto = obtenerUrlFotoPerfil(usuario.getId());
        return DtoUsuarioResumen.of(usuario, foto);
    }

    public Optional<DtoUsuarioResumen> obtenerResumenPorId(String id) {
        return repositorioUsuario.findById(id).map(this::toResumen);
    }

    public Optional<DtoUsuarioResumen> obtenerResumenPorUserNameOEmail(String userOrEmail) {
        return repositorioUsuario.findByUserNameOrEmail(userOrEmail, userOrEmail).map(this::toResumen);
    }

    // --- DTO Detalle (más campos del usuario) ---
    public Optional<DtoUsuarioDetalle> obtenerDetallePorId(String id) {
        return repositorioUsuario.findById(id).map(DtoUsuarioDetalle::from);
    }

    public Optional<DtoUsuarioDetalle> obtenerDetallePorUserName(String userName) {
        return repositorioUsuario.findByUserName(userName).map(DtoUsuarioDetalle::from);
    }

    public Optional<DtoUsuarioDetalle> obtenerDetallePorEmail(String email) {
        return repositorioUsuario.findByEmail(email).map(DtoUsuarioDetalle::from);
    }

    // Método auxiliar para recuperar la URL pública de la imagen de perfil activa
    private String obtenerUrlFotoPerfil(String usuarioId) {
        return repositorioImagenPerfil.findActivaByUsuarioId(usuarioId)
                .map(ImagenPerfil::getUrlPublica)
                .orElse(null); // El front puede manejar null como "sin foto"
    }

    // Posibles métodos adicionales futuros (crear/actualizar usuario) podrían ir aquí

    public boolean desactivarUsuario(String id) {
        return repositorioUsuario.findById(id).map(u -> {
            if (Boolean.FALSE.equals(u.getActivo())) {
                return false; // ya inactivo
            }
            u.setActivo(false);
            // Opcional: invalidar token / email verificado
            u.setToken(null);
            u.setFechaExpiracionToken(null);
            repositorioUsuario.save(u);
            return true;
        }).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }
}