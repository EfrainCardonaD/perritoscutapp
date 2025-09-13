package com.cut.cardona.errores;

import java.time.Instant;

/**
 * Respuesta estándar de error para la API.
 */
public record ErrorResponse(
        String mensaje,
        String error,
        int status,
        Instant timestamp,
        String path
) {}

