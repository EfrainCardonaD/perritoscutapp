package com.cut.cardona.modelo.perros.enums;

public enum PerroEstadoAdopcion {
    DISPONIBLE("Disponible"),
    ADOPTADO("Adoptado"),
    PENDIENTE("Pendiente"),
    NO_DISPONIBLE("No disponible");

    private final String label;

    PerroEstadoAdopcion(String label) { this.label = label; }
    public String getLabel() { return label; }

    public static PerroEstadoAdopcion fromLabel(String label) {
        if (label == null) return null;
        for (PerroEstadoAdopcion e : values()) {
            if (e.label.equalsIgnoreCase(label)) return e;
        }
        throw new IllegalArgumentException("Estado adopción desconocido: " + label);
    }
}

