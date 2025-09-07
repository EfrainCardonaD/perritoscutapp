package com.cut.cardona.modelo.convert;

import com.cut.cardona.modelo.adopcion.enums.SolicitudEstado;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SolicitudEstadoConverter implements AttributeConverter<SolicitudEstado, String> {
    @Override
    public String convertToDatabaseColumn(SolicitudEstado attribute) {
        return attribute == null ? null : attribute.getLabel();
    }

    @Override
    public SolicitudEstado convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SolicitudEstado.fromLabel(dbData);
    }
}

