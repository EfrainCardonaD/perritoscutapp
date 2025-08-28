package com.cut.cardona.modelo.perros;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Table(name = "imagenes_perros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenPerro {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perro_id", nullable = false)
    private Perro perro;

    // URL remota opcional: ahora se sirve por id desde almacenamiento local
    @Column(nullable = true, length = 500)
    private String url;

    @Column(length = 255)
    private String descripcion;

    @Column
    private Boolean principal;

    @Column(name = "fecha_subida")
    private Timestamp fechaSubida;
}
