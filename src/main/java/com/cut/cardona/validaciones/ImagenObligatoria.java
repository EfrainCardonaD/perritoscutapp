package com.cut.cardona.validaciones;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ImagenObligatoriaValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ImagenObligatoria {
    String message() default "La imagen de perfil es obligatoria";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
