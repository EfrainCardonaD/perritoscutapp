package com.cut.cardona.modelo.adopcion;

import com.cut.cardona.modelo.perros.Perro;
import com.cut.cardona.modelo.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.*;
import com.cut.cardona.modelo.adopcion.enums.SolicitudEstado;
import com.cut.cardona.modelo.convert.SolicitudEstadoConverter;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "solicitudes_adopcion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudAdopcion {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perro_id", nullable = false)
    private Perro perro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @Convert(converter = SolicitudEstadoConverter.class)
    @Column(length = 20)
    private SolicitudEstado estado; // 'Pendiente','En revisión','Aceptada','Rechazada','Cancelada'

    @Column(columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "fecha_solicitud")
    private Timestamp fechaSolicitud;

    @Column(name = "fecha_respuesta")
    private Timestamp fechaRespuesta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por")
    private Usuario revisadoPor;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentoSolicitud> documentos = new ArrayList<>();

    @Version
    private Long version;
}
