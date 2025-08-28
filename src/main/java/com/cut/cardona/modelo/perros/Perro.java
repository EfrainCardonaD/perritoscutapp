package com.cut.cardona.modelo.perros;

import com.cut.cardona.modelo.usuarios.Usuario;
import jakarta.persistence.*;
import lombok.*;
import com.cut.cardona.modelo.perros.enums.PerroEstadoAdopcion;
import com.cut.cardona.modelo.perros.enums.PerroEstadoRevision;
import com.cut.cardona.modelo.convert.PerroEstadoAdopcionConverter;
import com.cut.cardona.modelo.convert.PerroEstadoRevisionConverter;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "perros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Perro {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(nullable = false, length = 100)
    private String nombre;

    private Integer edad;

    // Almacenar como String por compatibilidad con ENUMs con acentos en BD
    @Column(length = 10)
    private String sexo; // 'Macho' | 'Hembra'

    @Column(name = "tamano", length = 10)
    private String tamano; // 'Pequeño' | 'Mediano' | 'Grande'

    @Column(length = 100)
    private String raza;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 255)
    private String ubicacion;

    @Column(name = "fecha_publicacion")
    private Timestamp fechaPublicacion;

    @Convert(converter = PerroEstadoAdopcionConverter.class)
    @Column(name = "estado_adopcion", length = 20)
    private PerroEstadoAdopcion estadoAdopcion; // 'Disponible','Adoptado','Pendiente','No disponible'

    @Convert(converter = PerroEstadoRevisionConverter.class)
    @Column(name = "estado_revision", length = 10)
    private PerroEstadoRevision estadoRevision; // 'Pendiente','Aprobado','Rechazado'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por")
    private Usuario revisadoPor;

    @Column(name = "fecha_revision")
    private Timestamp fechaRevision;

    @OneToMany(mappedBy = "perro", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ImagenPerro> imagenes = new ArrayList<>();

    @Version
    private Long version;
}
