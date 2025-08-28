package com.cut.cardona.modelo.dto.adopcion;

import jakarta.validation.constraints.*;

public record CrearSolicitudRequest(
        @NotBlank(message = "El perroId es obligatorio")
        @Size(max = 36, message = "El perroId debe ser un UUID de 36 caracteres")
        String perroId,

        @Size(max = 2000, message = "El mensaje no debe superar 2000 caracteres")
        String mensaje
) {}
