package com.cut.cardona.modelo.dto.perros;

import jakarta.validation.constraints.*;
import java.util.List;

public record CrearPerroRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
        String nombre,

        @Min(value = 0, message = "La edad no puede ser negativa")
        @Max(value = 25, message = "La edad no debe superar 25 años")
        Integer edad,

        @Pattern(regexp = "Macho|Hembra", message = "Sexo debe ser 'Macho' o 'Hembra'")
        String sexo,

        @Pattern(regexp = "Pequeño|Mediano|Grande", message = "Tamaño debe ser 'Pequeño','Mediano' o 'Grande'")
        String tamano,

        @Size(max = 100, message = "La raza no debe superar 100 caracteres")
        String raza,

        @Size(max = 5000, message = "La descripción es demasiado larga")
        String descripcion,

        @Size(max = 255, message = "La ubicación no debe superar 255 caracteres")
        String ubicacion,

        @NotEmpty(message = "Debe incluir al menos una imagen")
        List<ImagenPerroRequest> imagenes


) {}
