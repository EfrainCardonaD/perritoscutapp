package com.cut.cardona.modelo.dto.usuarios;
import com.cut.cardona.modelo.usuarios.Usuario;

public record DtoUsuario (
        String id,
        String userName,
        String email,
        String rol
){
    public DtoUsuario(Usuario usuario) {
        this(
            usuario.getId(),
            usuario.getUsername(),
            usuario.getEmail(),
            usuario.getRol() != null ? usuario.getRol().name().replaceFirst("^ROLE_", "") : null
        );
    }
}
