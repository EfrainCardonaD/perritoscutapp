package com.cut.cardona.modelo.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthenticationRequest(

        @NotBlank(message = "El nombre de usuario es obligatorio")
        String userName,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String password

) {}
