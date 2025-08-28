package com.cut.cardona.modelo.dto.perros;

import com.cut.cardona.modelo.perros.Perro;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public record DtoPerro(
        String id,
        String nombre,
        Integer edad,
        String sexo,
        String tamano,
        String raza,
        String descripcion,
        String ubicacion,
        String estadoAdopcion,
        String estadoRevision,
        String usuarioId,
        String imagenPrincipalId,
        List<String> imagenIds
) {
    public DtoPerro(Perro p) {
        this(
                p.getId(),
                p.getNombre(),
                p.getEdad(),
                p.getSexo(),
                p.getTamano(),
                p.getRaza(),
                p.getDescripcion(),
                p.getUbicacion(),
                p.getEstadoAdopcion() != null ? p.getEstadoAdopcion().getLabel() : null,
                p.getEstadoRevision() != null ? p.getEstadoRevision().getLabel() : null,
                p.getUsuario() != null ? p.getUsuario().getId() : null,
                p.getImagenes() == null ? null : p.getImagenes().stream()
                        .filter(i -> Boolean.TRUE.equals(i.getPrincipal()))
                        .map(i -> i.getId())
                        .findFirst().orElse(null),
                p.getImagenes() == null ? List.of() : p.getImagenes().stream()
                        .map(i -> i.getId())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );
    }
}
