package com.cut.cardona.controllers.api;

import com.cut.cardona.controllers.service.PerfilUsuarioService;
import com.cut.cardona.controllers.service.RegistroService;
import com.cut.cardona.errores.ErrorHandler;
import com.cut.cardona.modelo.dto.common.RestResponse;
import com.cut.cardona.modelo.dto.perfil.DtoPerfilCompleto;
import com.cut.cardona.modelo.dto.registro.DtoRegistroCompletoRequest;
import com.cut.cardona.modelo.dto.registro.DtoRegistroUsuario;
import com.cut.cardona.modelo.dto.registro.DtoValidacionPaso1;
import com.cut.cardona.modelo.dto.registro.DtoValidacionPaso2;
import com.cut.cardona.modelo.usuarios.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import java.security.Principal;
import java.util.Map;

/**
 * ✅ CONTROLADOR REFACTORIZADO siguiendo mejores prácticas:
 * - Uso de DTO con Bean Validation
 * - Manejo centralizado de errores
 * - Separación de responsabilidades
 * - Código limpio y mantenible
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Registro y Perfil", description = "Gestión completa de registro de usuarios y perfiles")
@Slf4j
public class RegistroController {

    private final PerfilUsuarioService perfilUsuarioService;
    private final RegistroService registroService;

    // =====================================
    // FORMULARIOS WEB TRADICIONALES
    // =====================================

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("registroUsuario", new DtoRegistroUsuario("", "", "", "", "", false));
        return "registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute("registroUsuario") DtoRegistroUsuario registroUsuario,
                                   BindingResult result, RedirectAttributes redirectAttributes) {
        validarDTO(registroUsuario, result);
        if (result.hasErrors()) {
            return "registro";
        }

        try {
            Usuario usuario = registroService.crearUsuarioBasico(registroUsuario);
            String successfulMessage = "Registro exitoso.<br>Ahora puedes iniciar sesión.<br>" + usuario.getUsername();
            redirectAttributes.addFlashAttribute("successfulRegistroUsuario", successfulMessage);
        } catch (Exception e) {
            ErrorHandler.handleGenericException(e, redirectAttributes);
            return "redirect:/registro";
        }

        return "redirect:/login";
    }

    // =====================================
    // APIs REST - REGISTRO (REFACTORIZADAS)
    // =====================================

    /**
     * ✅ REGISTRO BÁSICO - Usando DTO con validaciones Bean Validation
     */
    @Operation(
            summary = "Registro básico de usuario (JSON)",
            description = "Registra un nuevo usuario con datos básicos enviados como JSON desde el frontend"
    )
    @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o usuario ya existe")
    @PostMapping(value = "/registro", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<RestResponse<Map<String, Object>>> registroBasico(
            @Valid @RequestBody DtoRegistroUsuario registroUsuario) {

        log.info("Iniciando registro básico para usuario: {}", registroUsuario.userName());

        Map<String, Object> userData = registroService.registroBasico(registroUsuario);

        log.info("Usuario registrado exitosamente: {} (pendiente de verificación)", userData.get("userName"));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestResponse.success(
                        "Usuario registrado. Revisa tu correo para verificar la cuenta.",
                        userData
                ));
    }

    /**
     * ✅ REGISTRO COMPLETO - Usando DTO con validaciones Bean Validation
     */
    @Operation(
            summary = "Registro completo de usuario con perfil",
            description = "Registra un nuevo usuario con toda la información básica y de perfil en una sola operación"
    )
    @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o usuario ya existe")
    @PostMapping(value = "/registro-completo", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<RestResponse<Map<String, Object>>> registroCompleto(
            @Valid @RequestBody DtoRegistroCompletoRequest request) {

        log.info("Iniciando registro completo para usuario: {}", request.userName());

        DtoPerfilCompleto perfilCompleto = registroService.registroCompleto(request);

        Map<String, Object> userData = Map.of(
                "userName", perfilCompleto.userName(),
                "email", perfilCompleto.email(),
                "nombreReal", perfilCompleto.nombreReal(),
                "telefono", perfilCompleto.telefono(),
                "tieneImagenPerfil", perfilCompleto.fotoPerfilUrl() != null,
                "fotoPerfilUrl", perfilCompleto.fotoPerfilUrl() != null ? perfilCompleto.fotoPerfilUrl() : ""
        );

        log.info("Usuario con perfil completo registrado (pendiente de verificación): {}", perfilCompleto.userName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestResponse.success(
                        "Usuario registrado con perfil. Revisa tu correo para verificar la cuenta.",
                        userData
                ));
    }

    /**
     * ✅ REGISTRO COMPLETO CON IMAGEN - Multipart/form-data
     */
    @Operation(
            summary = "Registro completo con imagen de perfil",
            description = "Registra un nuevo usuario con perfil completo incluyendo imagen"
    )
    @PostMapping(value = "/registro-completo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<RestResponse<Map<String, Object>>> registroCompletoConImagen(
            @Valid @ModelAttribute DtoRegistroCompletoRequest request,
            @RequestParam(value = "fotoPerfil", required = false) MultipartFile fotoPerfil) {

        log.info("Iniciando registro completo con imagen para usuario: {}", request.userName());

        DtoPerfilCompleto perfilCompleto = registroService.registroCompletoConImagen(request, fotoPerfil);

        Map<String, Object> userData = Map.of(
                "userName", perfilCompleto.userName(),
                "email", perfilCompleto.email(),
                "nombreReal", perfilCompleto.nombreReal(),
                "telefono", perfilCompleto.telefono(),
                "tieneImagenPerfil", perfilCompleto.fotoPerfilUrl() != null,
                "fotoPerfilUrl", perfilCompleto.fotoPerfilUrl() != null ? perfilCompleto.fotoPerfilUrl() : ""
        );

        log.info("Usuario con perfil completo e imagen registrado (pendiente de verificación): {}", perfilCompleto.userName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestResponse.success(
                        "Usuario registrado con perfil e imagen. Revisa tu correo para verificar la cuenta.",
                        userData
                ));
    }

    /**
     * ✅ VALIDACIÓN PASO 1 - Usando DTO con Bean Validation
     */
    @Operation(
            summary = "Validar datos del paso 1",
            description = "Valida que el usuario y email no existan en la base de datos antes de proceder al paso 2"
    )
    @PostMapping(value = "/validar-paso1", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<RestResponse<Void>> validarPaso1(
            @Valid @RequestBody DtoValidacionPaso1 request) {

        log.debug("Validando paso 1 para usuario: {}", request.userName());

        registroService.validarPaso1(request);

        return ResponseEntity.ok(
                RestResponse.success("Datos válidos, puede proceder al paso 2")
        );
    }

    /**
     * ✅ VALIDACIÓN PASO 2 - Usando DTO con Bean Validation
     */
    @Operation(
            summary = "Validar datos del paso 2",
            description = "Valida que los datos de perfil sean correctos y únicos"
    )
    @PostMapping(value = "/validar-paso2", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<RestResponse<Void>> validarPaso2(
            @Valid @RequestBody DtoValidacionPaso2 request) {

        log.debug("Validando paso 2 para teléfono: {}", request.telefono());

        registroService.validarPaso2(request);

        return ResponseEntity.ok(
                RestResponse.success("Datos de perfil válidos")
        );
    }

    // =====================================
    // MÉTODOS PRIVADOS DE UTILIDAD
    // =====================================

    /**
     * ✅ Validación para formularios web tradicionales
     */
    private static void validarDTO(DtoRegistroUsuario registroUsuario, BindingResult result) {
        if (!registroUsuario.userName().matches("^[a-zA-Z0-9._-]+$") || registroUsuario.userName().isEmpty()) {
            result.rejectValue("userName", "error.userName", "El nombre de usuario solo puede contener letras, números, puntos, guiones bajos y guiones medios");
        }
        if (!registroUsuario.email().equals(registroUsuario.confirmEmail())) {
            result.rejectValue("confirmEmail", "error.confirmEmail", "Los correos electrónicos no coinciden");
        }
        if (!registroUsuario.password().equals(registroUsuario.confirmPassword())) {
            result.rejectValue("confirmPassword", "error.confirmPassword", "Las contraseñas no coinciden");
        }
    }

    // =====================================
    // APIs REST - PERFIL (SIN CAMBIOS)
    // =====================================

    @Operation(
            summary = "Obtener perfil completo",
            description = "Obtiene toda la información del perfil del usuario autenticado"
    )
    @GetMapping("/perfil/completo")
    @ResponseBody
    public ResponseEntity<?> obtenerPerfilCompleto(Principal principal) {
        try {
            String usuarioId = principal.getName();
            return perfilUsuarioService.obtenerPerfilCompleto(usuarioId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener el perfil"));
        }
    }

    @Operation(
            summary = "Actualizar imagen de perfil",
            description = "Actualiza o establece la imagen de perfil del usuario autenticado"
    )
    @PostMapping(value = "/perfil/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<?> actualizarImagenPerfil(
            @RequestParam("imagen") MultipartFile imagen,
            Principal principal) {
        try {
            String usuarioId = principal.getName();
            String urlImagen = perfilUsuarioService.actualizarImagenPerfil(usuarioId, imagen);
            return ResponseEntity.ok(Map.of(
                    "mensaje", "Imagen de perfil actualizada exitosamente",
                    "urlImagen", urlImagen
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar la imagen"));
        }
    }

    @Operation(
            summary = "Obtener perfil por ID de usuario",
            description = "Obtiene el perfil de un usuario específico (solo para administradores)"
    )
    @GetMapping("/perfil/usuario/{usuarioId}")
    @ResponseBody
    public ResponseEntity<?> obtenerPerfilPorUsuarioId(@PathVariable String usuarioId) {
        try {
            return perfilUsuarioService.obtenerPerfilCompleto(usuarioId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener el perfil"));
        }
    }
}
