package com.cut.cardona.modelo.adopcion.enums;

import lombok.Getter;

@Getter
public enum SolicitudEstado {
    PENDIENTE("Pendiente"),
    EN_REVISION("En revisión"),
    ACEPTADA("Aceptada"),
    RECHAZADA("Rechazada"),
    CANCELADA("Cancelada");

    private final String label;

    SolicitudEstado(String label) { this.label = label; }

    public static SolicitudEstado fromLabel(String label) {
        if (label == null) return null;
        for (SolicitudEstado e : values()) {
            if (e.label.equalsIgnoreCase(label)) return e;
        }
        throw new IllegalArgumentException("Estado solicitud desconocido: " + label);
    }
}

