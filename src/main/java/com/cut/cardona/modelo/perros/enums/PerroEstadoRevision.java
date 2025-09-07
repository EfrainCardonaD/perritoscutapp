package com.cut.cardona.modelo.perros.enums;

public enum PerroEstadoRevision {
    PENDIENTE("Pendiente"),
    APROBADO("Aprobado"),
    RECHAZADO("Rechazado");

    private final String label;

    PerroEstadoRevision(String label) { this.label = label; }
    public String getLabel() { return label; }

    public static PerroEstadoRevision fromLabel(String label) {
        if (label == null) return null;
        for (PerroEstadoRevision e : values()) {
            if (e.label.equalsIgnoreCase(label)) return e;
        }
        throw new IllegalArgumentException("Estado revisión desconocido: " + label);
    }
}

