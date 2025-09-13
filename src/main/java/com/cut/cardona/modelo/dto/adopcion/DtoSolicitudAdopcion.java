package com.cut.cardona.modelo.dto.adopcion;

import com.cut.cardona.modelo.adopcion.DocumentoSolicitud;
import com.cut.cardona.modelo.adopcion.SolicitudAdopcion;
import com.cut.cardona.modelo.adopcion.enums.SolicitudEstado;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * DTO unificado para exponer la información completa (o resumida) de una Solicitud de Adopción
 * incluyendo, opcionalmente, la lista de documentos asociados. Sustituirá los antiguos
 * DtoSolicitud, DtoSolicitudCreada y DtoDocumentoSolicitud en el refactor.
 */
public record DtoSolicitudAdopcion(
        // Datos de la solicitud
        String id,
        String perroId,
        String solicitanteId,
        String estado,            // label legible del enum
        String estadoCodigo,       // nombre del enum para lógica front si se requiere
        String mensaje,
        Timestamp fechaSolicitud,
        Timestamp fechaRespuesta,
        String revisadoPorId,
        // Documentos (puede ser null o lista vacía según modo de construcción)
        List<Documento> documentos
) {
    /** Representación ligera de un documento asociado a la solicitud */
    public record Documento(
            String id,
            String tipoDocumento,
            String urlDocumento,
            String nombreArchivo,
            String tipoMime,
            Long tamanoBytes,
            Timestamp fechaSubida
    ) {
        public static Documento of(DocumentoSolicitud d) {
            if (d == null) return null;
            return new Documento(
                    d.getId(),
                    d.getTipoDocumento(),
                    d.getUrlDocumento(),
                    d.getNombreArchivo(),
                    d.getTipoMime(),
                    d.getTamanoBytes(),
                    d.getFechaSubida()
            );
        }
    }

    /** Crea un DTO sin la colección de documentos (modo listado / resumen). */
    public static DtoSolicitudAdopcion from(SolicitudAdopcion s) { return of(s, false); }

    /** Crea un DTO incluyendo la colección de documentos (modo detalle). */
    public static DtoSolicitudAdopcion fromWithDocumentos(SolicitudAdopcion s) { return of(s, true); }

    /** Factoría central parametrizada. */
    public static DtoSolicitudAdopcion of(SolicitudAdopcion s, boolean incluirDocumentos) {
        if (s == null) return null;
        SolicitudEstado est = s.getEstado();
        List<Documento> docs = null;
        if (incluirDocumentos) {
            docs = s.getDocumentos() == null ? Collections.emptyList() : s.getDocumentos().stream()
                    .filter(Objects::nonNull)
                    .map(Documento::of)
                    .collect(Collectors.toList());
        }
        return new DtoSolicitudAdopcion(
                s.getId(),
                s.getPerro() != null ? s.getPerro().getId() : null,
                s.getSolicitante() != null ? s.getSolicitante().getId() : null,
                est != null ? est.getLabel() : null,
                est != null ? est.name() : null,
                s.getMensaje(),
                s.getFechaSolicitud(),
                s.getFechaRespuesta(),
                s.getRevisadoPor() != null ? s.getRevisadoPor().getId() : null,
                docs
        );
    }
}

