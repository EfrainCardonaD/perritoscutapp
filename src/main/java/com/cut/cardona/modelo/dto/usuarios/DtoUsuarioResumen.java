package com.cut.cardona.modelo.dto.usuarios;

import com.cut.cardona.modelo.usuarios.Usuario;

/**
 * DTO ligero para exponer en el front: nombre de usuario, email y foto de perfil.
 */
public record DtoUsuarioResumen(
        String userName,
        String email,
        String fotoPerfilUrl
) {
    public static DtoUsuarioResumen of(Usuario u, String fotoPerfilUrl) {
        if (u == null) return null;
        return new DtoUsuarioResumen(u.getUsername(), u.getEmail(), fotoPerfilUrl);
    }
}

