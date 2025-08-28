package com.cut.cardona.validaciones;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

public class ImagenObligatoriaValidator implements ConstraintValidator<ImagenObligatoria, MultipartFile> {

    @Override
    public void initialize(ImagenObligatoria constraintAnnotation) {
        // Inicialización si es necesaria
    }

    @Override
    public boolean isValid(MultipartFile imagen, ConstraintValidatorContext context) {
        if (imagen == null || imagen.isEmpty()) {
            return false;
        }

        // Verificar que sea una imagen válida
        String contentType = imagen.getContentType();
        return contentType != null &&
               (contentType.startsWith("image/jpeg") ||
                contentType.startsWith("image/png") ||
                contentType.startsWith("image/gif") ||
                contentType.startsWith("image/webp"));
    }
}
