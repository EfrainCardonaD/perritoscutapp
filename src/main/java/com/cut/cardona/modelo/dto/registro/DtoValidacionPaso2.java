package com.cut.cardona.modelo.dto.registro;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO para validar el paso 2 del registro (datos de perfil)
 */
public record DtoValidacionPaso2(

    @NotBlank(message = "El nombre completo es requerido")
    @Size(min = 2, max = 50, message = "El nombre completo debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\u00f1\\u00d1\\s]+$", message = "El nombre completo solo puede contener letras y espacios")
    String nombreReal,

    @NotBlank(message = "El teléfono es requerido")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe tener exactamente 10 dígitos")
    String telefono

) {
}
