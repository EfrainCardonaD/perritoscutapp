package com.cut.cardona.service.admin;

import com.cut.cardona.modelo.dto.perfil.DtoPerfilCompleto;
import com.cut.cardona.modelo.dto.perros.DtoPerro;
import com.cut.cardona.modelo.imagenes.ImagenPerfil;
import com.cut.cardona.modelo.imagenes.RepositorioImagenPerfil;
import com.cut.cardona.modelo.perfil.PerfilUsuario;
import com.cut.cardona.modelo.perfil.RepositorioPerfilUsuario;
import com.cut.cardona.modelo.perros.Perro;
import com.cut.cardona.modelo.perros.RepositorioPerro;
import com.cut.cardona.modelo.usuarios.*;
import com.cut.cardona.modelo.adopcion.RepositorioSolicitudAdopcion;
import com.cut.cardona.service.infra.storage.ImageStorageService;
import com.cut.cardona.service.perfil.PerfilService;
import com.cut.cardona.service.perros.PerroService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioPerro repositorioPerro;
    private final RepositorioSolicitudAdopcion repositorioSolicitudAdopcion;
    private final RepositorioPerfilUsuario repositorioPerfilUsuario;
    private final RepositorioImagenPerfil repositorioImagenPerfil;
    private final RepositorioVerificationToken repositorioVerificationToken;
    private final RepositorioResetToken repositorioResetToken;
    private final PerfilService perfilService;
    private final PerroService perroService;
    private final ImageStorageService imageStorageService;

    // --- Usuarios ---

    @Transactional(readOnly = true)
    public List<DtoPerfilCompleto> listarUsuarios() {
        return repositorioUsuario.findAll().stream()
                .map(u -> perfilService.obtenerPerfilCompleto(u.getId()).orElseGet(() -> DtoPerfilCompleto.minimal(u)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<DtoPerfilCompleto> obtenerUsuario(String id) {
        Optional<DtoPerfilCompleto> dto = perfilService.obtenerPerfilCompleto(id);
        if (dto.isPresent()) return dto;
        return repositorioUsuario.findById(id).map(DtoPerfilCompleto::from);
    }

    public DtoPerfilCompleto cambiarRol(String usuarioId, String nuevoRolPlano) {
        Usuario u = repositorioUsuario.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (nuevoRolPlano == null || nuevoRolPlano.isBlank()) {
            throw new IllegalArgumentException("Rol requerido");
        }
        String upper = nuevoRolPlano.trim().toUpperCase(Locale.ROOT);
        // Aceptar ADMIN/USER/REVIEWER o con prefijo ROLE_
        if (!upper.startsWith("ROLE_")) upper = "ROLE_" + upper;
        Roles rol;
        try { rol = Roles.valueOf(upper); } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Rol inválido: " + nuevoRolPlano);
        }
        u.setRol(rol);
        repositorioUsuario.save(u);
        return perfilService.obtenerPerfilCompleto(usuarioId).orElseGet(() -> DtoPerfilCompleto.minimal(u));
    }

    public boolean desactivarUsuario(String id) {
        // Borrado lógico: reutilizamos servicio de perfil
        return perfilService.desactivarUsuario(id);
    }

    public DtoPerfilCompleto activarUsuario(String id) {
        Usuario u = repositorioUsuario.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        if (Boolean.TRUE.equals(u.getActivo())) {
            // ya estaba activo; retornamos DTO sin cambios
            return perfilService.obtenerPerfilCompleto(id).orElseGet(() -> DtoPerfilCompleto.from(u));
        }
        u.setActivo(true);
        repositorioUsuario.save(u);
        return perfilService.obtenerPerfilCompleto(id).orElseGet(() -> DtoPerfilCompleto.minimal(u));
    }

    public void eliminarUsuarioFisico(String usuarioId) {
        Usuario usuario = repositorioUsuario.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // 1) Eliminar solicitudes del usuario como solicitante
        var solicitudesByUser = repositorioSolicitudAdopcion.findBySolicitanteId(usuarioId);
        if (!solicitudesByUser.isEmpty()) {
            repositorioSolicitudAdopcion.deleteAll(solicitudesByUser);
        }

        // 2) Perros del usuario: eliminar solicitudes por perro y luego el perro (usa servicio para borrar imágenes)
        var perros = repositorioPerro.findByUsuarioId(usuarioId);
        for (Perro p : perros) {
            var solicitudesDelPerro = repositorioSolicitudAdopcion.findByPerroId(p.getId());
            if (!solicitudesDelPerro.isEmpty()) {
                repositorioSolicitudAdopcion.deleteAll(solicitudesDelPerro);
            }
            // Intentar borrado estándar (respeta reglas); si falla por estado, forzar borrado
            try {
                perroService.eliminarPerro(p.getId());
            } catch (Exception ex) {
                // Forzar borrado: limpiar imágenes en storage y borrar entidad
                var imagenes = new ArrayList<>(p.getImagenes());
                if (!imagenes.isEmpty()) {
                    imagenes.forEach(img -> {
                        try { imageStorageService.deleteDogImage(img.getId()); } catch (Exception ignored) {}
                    });
                }
                repositorioPerro.delete(p);
            }
        }

        // 3) Imagen y perfil del usuario (cargar perfil con imágenes)
        repositorioPerfilUsuario.findByUsuarioIdWithImagenes(usuarioId).ifPresent(perfil -> {
            limpiarImagenesPerfil(perfil);
            repositorioPerfilUsuario.delete(perfil);
        });

        // 4) Tokens (verificación y reset)
        var tokensVerif = repositorioVerificationToken.findAll().stream()
                .filter(t -> t.getUsuario() != null && usuarioId.equals(t.getUsuario().getId()))
                .toList();
        if (!tokensVerif.isEmpty()) {
            repositorioVerificationToken.deleteAll(tokensVerif);
        }
        var resetTokens = repositorioResetToken.findAll().stream()
                .filter(t -> t.getUsuario() != null && usuarioId.equals(t.getUsuario().getId()))
                .toList();
        if (!resetTokens.isEmpty()) {
            repositorioResetToken.deleteAll(resetTokens);
        }

        // 5) Finalmente, eliminar usuario
        repositorioUsuario.delete(usuario);
    }

    private void limpiarImagenesPerfil(PerfilUsuario perfil) {
        // Borrado best-effort de imágenes de perfil en storage
        List<ImagenPerfil> imgs = perfil.getImagenesPerfil();
        if (imgs != null) {
            for (ImagenPerfil img : imgs) {
                String nombreArchivo = img.getNombreArchivo();
                if (nombreArchivo != null && !nombreArchivo.isBlank()) {
                    String publicId;
                    int dot = nombreArchivo.lastIndexOf('.');
                    if (dot > 0) publicId = nombreArchivo.substring(0, dot); else publicId = nombreArchivo;
                    try { imageStorageService.deleteProfileImage(publicId); } catch (Exception ignored) {}
                }
            }
        }
        repositorioImagenPerfil.deleteByPerfilUsuarioId(perfil.getId());
    }

    // --- Perros ---
    @Transactional(readOnly = true)
    public List<DtoPerro> listarPerros() {
        return repositorioPerro.findAll().stream().map(DtoPerro::new).toList();
    }

    @Transactional(readOnly = true)
    public List<DtoPerro> buscarPerrosPorRevision(String revision) {
        if (revision == null || revision.isBlank()) {
            return listarPerros();
        }
        String rv = revision.trim();
        // Atajo: si piden "pendientes"
        if (rv.equalsIgnoreCase("PENDIENTE") || rv.equalsIgnoreCase("PENDIENTES")) {
            return repositorioPerro.findPendientesRevision().stream().map(DtoPerro::new).toList();
        }
        // Intentar mapear contra enum por label (usa método del servicio)
        com.cut.cardona.modelo.perros.enums.PerroEstadoRevision estado = com.cut.cardona.modelo.perros.enums.PerroEstadoRevision.fromLabel(rv);
        return repositorioPerro.findByEstadoRevision(estado).stream().map(DtoPerro::new).toList();
    }

    public void eliminarPerro(String perroId) {
        perroService.eliminarPerro(perroId);
    }
}
