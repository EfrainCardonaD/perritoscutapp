package com.cut.cardona.modelo.adopcion;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Table(name = "documentos_solicitud")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoSolicitud {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudAdopcion solicitud;

    @Column(name = "tipo_documento", length = 30, nullable = false)
    private String tipoDocumento; // 'Identificacion' | 'CartaResponsiva'

    @Column(name = "url_documento", length = 500, nullable = false)
    private String urlDocumento;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "tipo_mime", length = 100)
    private String tipoMime;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "fecha_subida")
    private Timestamp fechaSubida;
}

