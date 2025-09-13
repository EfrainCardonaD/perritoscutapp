package com.cut.cardona.modelo.dto.perfil;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record DtoActualizarPerfilRequest(
        @Size(max = 100, message = "Nombre real demasiado largo")
        String nombreReal,
        @Pattern(regexp = "^$|^[+0-9()\\s-]{5,20}$", message = "Formato de teléfono inválido")
        String telefono,
        @Size(max = 5, message = "Idioma inválido")
        String idioma,
        @Size(max = 50, message = "Zona horaria inválida")
        String zonaHoraria,
        LocalDate fechaNacimiento
) {}
