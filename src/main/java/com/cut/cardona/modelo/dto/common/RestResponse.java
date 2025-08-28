package com.cut.cardona.modelo.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO estándar para respuestas de la API REST
 * Proporciona un formato consistente para todas las respuestas
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RestResponse<T>(
    boolean success,
    String mensaje,
    T data,
    LocalDateTime timestamp,
    Map<String, Object> metadata
) {

    /**
     * Crear respuesta exitosa con datos
     */
    public static <T> RestResponse<T> success(String mensaje, T data) {
        return RestResponse.<T>builder()
                .success(true)
                .mensaje(mensaje)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crear respuesta exitosa sin datos
     */
    public static RestResponse<Void> success(String mensaje) {
        return RestResponse.<Void>builder()
                .success(true)
                .mensaje(mensaje)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crear respuesta de error
     */
    public static RestResponse<Void> error(String mensaje) {
        return RestResponse.<Void>builder()
                .success(false)
                .mensaje(mensaje)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crear respuesta de error con datos adicionales
     */
    public static <T> RestResponse<T> error(String mensaje, T data) {
        return RestResponse.<T>builder()
                .success(false)
                .mensaje(mensaje)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Crear respuesta con metadata
     */
    public static <T> RestResponse<T> withMetadata(String mensaje, T data, Map<String, Object> metadata) {
        return RestResponse.<T>builder()
                .success(true)
                .mensaje(mensaje)
                .data(data)
                .metadata(metadata)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
