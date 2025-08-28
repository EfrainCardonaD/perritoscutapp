package com.cut.cardona.validaciones;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class MayorDe15Validator implements ConstraintValidator<MayorDe15, LocalDate> {

    @Override
    public void initialize(MayorDe15 constraintAnnotation) {
        // Inicialización si es necesaria
    }

    @Override
    public boolean isValid(LocalDate fechaNacimiento, ConstraintValidatorContext context) {
        if (fechaNacimiento == null) {
            return false; // La fecha es obligatoria
        }

        LocalDate fechaMinima = LocalDate.now().minusYears(15);
        return fechaNacimiento.isBefore(fechaMinima) || fechaNacimiento.isEqual(fechaMinima);
    }
}
