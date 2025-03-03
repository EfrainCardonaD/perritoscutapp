package com.cut.cardona.errores;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public class ErrorHandler {

    public static void handleDataIntegrityViolationException(DataIntegrityViolationException e, RedirectAttributes redirectAttributes) {
        Throwable cause = e.getCause();
        if (cause instanceof ConstraintViolationException) {
            String constraintName = ((ConstraintViolationException) cause).getConstraintName();
            String errorMessage;
            if (constraintName.contains("user_name")) {
                errorMessage = "El nombre de usuario ya está en uso";
            } else if (constraintName.contains("email")) {
                errorMessage = "El correo electrónico ya está en uso";
            } else {
                errorMessage = "No se ha podido hacer el registro, por favor intente de nuevo";
            }
            redirectAttributes.addFlashAttribute("error", errorMessage);
        } else {
            String errorMessage = "No se ha podido hacer el registro, por favor intente de nuevo";
            redirectAttributes.addFlashAttribute("error", errorMessage);
        }
    }

    public static void handleGenericException(Exception e, RedirectAttributes redirectAttributes) {
        String errorMessage = "Ha ocurrido un error, por favor intente de nuevo";
        redirectAttributes.addFlashAttribute("error", errorMessage);
    }
}