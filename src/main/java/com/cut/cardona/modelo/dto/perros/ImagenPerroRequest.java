package com.cut.cardona.modelo.dto.perros;

import jakarta.validation.constraints.*;

public record ImagenPerroRequest(
        @NotBlank(message = "El id de la imagen es obligatorio")
        @Size(min = 36, max = 36, message = "El id debe ser un UUID de 36 caracteres")
        String id,

        @Size(max = 255, message = "La descripción no debe superar 255 caracteres")
        String descripcion,

        Boolean principal
) {}
