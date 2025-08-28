package com.cut.cardona.modelo.dto.adopcion;

import com.cut.cardona.modelo.adopcion.SolicitudAdopcion;

import java.sql.Timestamp;

public record DtoSolicitud(
        String id,
        String perroId,
        String solicitanteId,
        String estado,
        String mensaje,
        Timestamp fechaSolicitud,
        Timestamp fechaRespuesta
) {
    public DtoSolicitud(SolicitudAdopcion s) {
        this(
                s.getId(),
                s.getPerro() != null ? s.getPerro().getId() : null,
                s.getSolicitante() != null ? s.getSolicitante().getId() : null,
                s.getEstado() != null ? s.getEstado().getLabel() : null,
                s.getMensaje(),
                s.getFechaSolicitud(),
                s.getFechaRespuesta()
        );
    }
}
