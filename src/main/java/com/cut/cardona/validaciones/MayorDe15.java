package com.cut.cardona.validaciones;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MayorDe15Validator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MayorDe15 {
    String message() default "Debe ser mayor de 15 años";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
