package com.cut.cardona.modelo.convert;

import com.cut.cardona.modelo.perros.enums.PerroEstadoAdopcion;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PerroEstadoAdopcionConverter implements AttributeConverter<PerroEstadoAdopcion, String> {
    @Override
    public String convertToDatabaseColumn(PerroEstadoAdopcion attribute) {
        return attribute == null ? null : attribute.getLabel();
    }

    @Override
    public PerroEstadoAdopcion convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PerroEstadoAdopcion.fromLabel(dbData);
    }
}

