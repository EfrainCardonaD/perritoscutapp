package com.cut.cardona.modelo.dto.perfil;

import com.cut.cardona.modelo.usuarios.Roles;
import java.time.LocalDate;
import java.sql.Timestamp;

public record DtoPerfilCompleto(
    // Datos básicos del usuario
    String id,
    String userName,
    String email,
    Roles rol,
    Boolean activo,
    Timestamp fechaCreacion,

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
) {}
