package com.cut.cardona.modelo.dto.adopcion;

import jakarta.validation.constraints.*;

public record DocumentoRequest(
        @NotBlank(message = "El tipo de documento es obligatorio")
        @Size(max = 30, message = "El tipo de documento no debe superar 30 caracteres")
        String tipoDocumento,

        @NotBlank(message = "La URL del documento es obligatoria")
        @Size(max = 500, message = "La URL no debe superar 500 caracteres")
        String urlDocumento,

        @Size(max = 255, message = "El nombre de archivo no debe superar 255 caracteres")
        String nombreArchivo,

        @Size(max = 100, message = "El tipo MIME no debe superar 100 caracteres")
        String tipoMime,

        @PositiveOrZero(message = "El tamaño debe ser positivo")
        Long tamanoBytes
) {}
