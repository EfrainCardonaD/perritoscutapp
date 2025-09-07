package com.cut.cardona.modelo.convert;

import com.cut.cardona.modelo.perros.enums.PerroEstadoRevision;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PerroEstadoRevisionConverter implements AttributeConverter<PerroEstadoRevision, String> {
    @Override
    public String convertToDatabaseColumn(PerroEstadoRevision attribute) {
        return attribute == null ? null : attribute.getLabel();
    }

    @Override
    public PerroEstadoRevision convertToEntityAttribute(String dbData) {
        return dbData == null ? null : PerroEstadoRevision.fromLabel(dbData);
    }
}

