package com.cut.cardona.errores;

import com.cut.cardona.modelo.dto.common.RestResponse;
import com.cut.cardona.security.ratelimit.RateLimitExceededException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<RestResponse<Void>> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(RestResponse.error("Recurso no encontrado"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Map<String, String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Validation errors found: {}", ex.getBindingResult().getFieldErrorCount());

        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            log.debug("Field error: {} = {}", error.getField(), error.getDefaultMessage());
            errores.put(error.getField(), error.getDefaultMessage());
        });

        // Tomar siempre el primer mensaje de error capturado
        String mensajePrincipal = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Error de validación"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.error(mensajePrincipal, errores));
    }

    @ExceptionHandler(ValidacionDeIntegridad.class)
    public ResponseEntity<RestResponse<Void>> handleValidacionDeIntegridad(ValidacionDeIntegridad ex) {
        log.warn("Validation integrity error: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)  // Cambiado de UNAUTHORIZED a BAD_REQUEST
                .body(RestResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<RestResponse<Void>> handleValidationException(ValidationException ex) {
        log.warn("Validation exception: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DomainConflictException.class)
    public ResponseEntity<RestResponse<Void>> handleDomainConflict(DomainConflictException ex) {
        log.warn("Domain conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(RestResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<RestResponse<Void>> handleUnprocessable(UnprocessableEntityException ex) {
        log.warn("Unprocessable entity: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(RestResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RestResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage());

        String errorMessage = "Error de integridad de datos";
        String specificCause = ex.getMostSpecificCause().getMessage().toLowerCase();

        // Mensajes personalizados según el tipo de violación
        if (specificCause.contains("email")) {
            errorMessage = "Este email ya está registrado en el sistema";
        } else if (specificCause.contains("user_name") || specificCause.contains("username")) {
            errorMessage = "Este nombre de usuario ya está en uso";
        } else if (specificCause.contains("telefono") || specificCause.contains("phone")) {
            errorMessage = "Este número de teléfono ya está registrado";
        } else if (specificCause.contains("documento")) {
            errorMessage = "Este documento ya está registrado en el sistema";
        } else if (specificCause.contains("unique") || specificCause.contains("duplicate")) {
            errorMessage = "Ya existe un registro con estos datos";
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(RestResponse.error(errorMessage));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<RestResponse<Void>> handleRateLimitExceeded(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(RestResponse.error("Demasiadas solicitudes. Por favor, intenta más tarde."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RestResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<RestResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RestResponse.error("Error interno del servidor. Por favor, intenta más tarde."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RestResponse.error("Ha ocurrido un error inesperado. Por favor, contacta al soporte técnico."));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<RestResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(RestResponse.error("Credenciales inválidas"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RestResponse<Map<String, Object>>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON: {}", ex.getMessage());
        String detalle = ex.getMostSpecificCause().getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.error("Formato JSON inválido", Map.of("detalle", detalle)));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<RestResponse<Map<String, Object>>> handleMissingHeader(MissingRequestHeaderException ex) {
        log.warn("Missing header: {}", ex.getHeaderName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(RestResponse.error("Header requerido ausente: " + ex.getHeaderName(), Map.of("header", ex.getHeaderName())));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<RestResponse<Map<String, Object>>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getContentType());
        var data = Map.of(
                "contentTypeRecibido", String.valueOf(ex.getContentType()),
                "contentTypesSoportados", ex.getSupportedMediaTypes().stream().map(Object::toString).toList()
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(RestResponse.error("Content-Type no soportado", data));
    }
}
