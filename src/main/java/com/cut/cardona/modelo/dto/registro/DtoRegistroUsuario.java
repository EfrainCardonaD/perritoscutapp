package com.cut.cardona.modelo.dto.registro;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record DtoRegistroUsuario(
        @NotBlank(message = "{nombre.obligatorio}")
        String userName,
        String email,
        String confirmEmail,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String confirmPassword,
        Boolean terms
) {
}

