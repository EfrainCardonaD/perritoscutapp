package com.cut.cardona.modelo.imagenes;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "imagenes")
@Entity(name = "Histograma")
public class Histograma {
    @Getter
    @Setter
    @Id
    private Long id;
    private String url;
    private String titulo;
    private String descripcion;
    @Column(name = "fecha_subida")
    private Timestamp fechaSubida;

    public Histograma(DatosHistograma datosImagenes) {
        this.url = datosImagenes.url();
        this.titulo = datosImagenes.titulo();
        this.descripcion = datosImagenes.descripcion();
        this.fechaSubida = datosImagenes.fechaSubida();
    }

}
