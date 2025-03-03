package com.cut.cardona.modelo.imagenes;

public record DatosMostrarHistograma(
        String url,
        String titulo,
        String descripcion,
        String fechaSubida

) {
    public DatosMostrarHistograma(Histograma histograma) {
        this(histograma.getUrl(), histograma.getTitulo(), histograma.getDescripcion(), histograma.getFechaSubida().toString());
    }

}
