package com.cut.cardona.modelo.dto.perfil;

import com.cut.cardona.modelo.usuarios.Roles;
import com.cut.cardona.modelo.usuarios.Usuario;
import com.cut.cardona.service.perfil.PerfilUsuarioService;

import java.time.LocalDate;
import java.sql.Timestamp;


public record DtoPerfilCompleto(
    // Datos básicos del usuario
    String id,
    String userName,
    String email,
    Boolean emailVerificado,
    Roles rol,
    Boolean activo,
    Timestamp fechaCreacion,
    Timestamp ultimoAcceso,

    // Datos del perfil extendido
    String perfilId,
    String nombreReal,
    String telefono,
    String idioma,
    String zonaHoraria,
    LocalDate fechaNacimiento,
    Boolean esMayorDeEdad,

    // Datos de la imagen de perfil
    String fotoPerfilId,
    String fotoPerfilUrl,
    String nombreArchivoFoto,
    String tipoMimeFoto,
    Long tamañoBytesFoto,

    // Metadatos
    Timestamp fechaCreacionPerfil,
    Timestamp fechaActualizacionPerfil
) {
    /**
     * Construye un DTO con solo la información básica del usuario.
     * Los campos de perfil e imagen se dejan en null.
     */
    public static DtoPerfilCompleto minimal(Usuario u) {
        if (u == null) return null;
        return new DtoPerfilCompleto(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getEmailVerificado(),
                u.getRol(),
                u.getActivo(),
                u.getFechaCreacion(),
                u.getUltimoAcceso(),
                // Perfil (nulls)
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                // Imagen (nulls)
                null,
                null,
                null,
                null,
                null,
                // Metadatos perfil (nulls)
                null,
                null
        );
    }

    /**
     * Compatibilidad: crea un DTO mínimo a partir del usuario.
     * Para incluir datos de perfil e imagen, use from(Usuario, PerfilUsuarioService).
     */
    public static DtoPerfilCompleto from(Usuario u) {
        return minimal(u);
    }

    /**
     * Usa PerfilUsuarioService para construir el DTO completo. Si no existe perfil o imagen,
     * retorna el DTO mínimo con datos de usuario.
     */
    public static DtoPerfilCompleto from(Usuario u, PerfilUsuarioService perfilUsuarioService) {
        if (u == null) return null;
        if (perfilUsuarioService == null) return minimal(u);
        return perfilUsuarioService.obtenerPerfilCompleto(u.getId()).orElseGet(() -> minimal(u));
    }
}
