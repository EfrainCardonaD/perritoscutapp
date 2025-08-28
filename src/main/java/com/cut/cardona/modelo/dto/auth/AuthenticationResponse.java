package com.cut.cardona.modelo.dto.auth;
import com.cut.cardona.modelo.dto.usuarios.DtoUsuario;

public record AuthenticationResponse(
        String token,
        String refreshToken,
        DtoUsuario usuario,
        long expiresIn,
        boolean success,
        String mensaje
) {
    // Constructor para login exitoso
    public AuthenticationResponse(String token, String refreshToken, DtoUsuario usuario, long expiresIn) {
        this(token, refreshToken, usuario, expiresIn, true, "Inicio de sesión exitoso");
    }

    // Constructor para refresh exitoso
    public static AuthenticationResponse refreshSuccess(String token, String refreshToken, DtoUsuario usuario, long expiresIn) {
        return new AuthenticationResponse(token, refreshToken, usuario, expiresIn, true, "Sesión renovada exitosamente");
    }
}
