package com.cut.cardona.modelo.dto.usuarios;

import com.cut.cardona.modelo.usuarios.Usuario;
import java.sql.Timestamp;

/**
 * DTO con la información (casi) completa del usuario.
 * NOTA: No expone password ni token por razones de seguridad.
 */
public record DtoUsuarioDetalle(
        String id,
        String userName,
        String email,
        String rol,
        Boolean activo,
        Boolean emailVerificado,
        Timestamp fechaCreacion,
        Timestamp ultimoAcceso
) {
    public static DtoUsuarioDetalle from(Usuario u) {
        if (u == null) return null;
        return new DtoUsuarioDetalle(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getRol() != null ? u.getRol().name().replaceFirst("^ROLE_", "") : null,
                u.getActivo(),
                u.getEmailVerificado(),
                u.getFechaCreacion(),
                u.getUltimoAcceso()
        );
    }
}

