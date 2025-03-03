package com.cut.cardona.modelo.imagenes;

import java.sql.Timestamp;

public record DatosHistograma(
        String url,
        String titulo,
        String descripcion,
        Timestamp fechaSubida) {

}
