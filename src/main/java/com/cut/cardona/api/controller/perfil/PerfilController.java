package com.cut.cardona.api.controller.perfil;

import com.cut.cardona.service.perfil.PerfilService;
import com.cut.cardona.modelo.dto.common.RestResponse;
import com.cut.cardona.modelo.dto.usuarios.DtoUsuarioResumen;
import com.cut.cardona.modelo.dto.perfil.DtoPerfilCompleto;
import com.cut.cardona.modelo.dto.perfil.DtoActualizarPerfilRequest;
import com.cut.cardona.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'REVIEWER')")
public class PerfilController {

    private final PerfilService perfilService;

    /**
     * Devuelve un resumen ligero del usuario autenticado (userName, email, fotoPerfilUrl)
     */
    @GetMapping("/me/resumen")
    public ResponseEntity<RestResponse<DtoUsuarioResumen>> obtenerMiResumen(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401)
                    .body(RestResponse.error("No autenticado", (DtoUsuarioResumen) null));
        }
        var usuario = userDetails.getUsuario();
        var dto = perfilService.toResumen(usuario);
        log.debug("Resumen solicitado para usuario {}", usuario.getUsername());
        return ResponseEntity.ok(RestResponse.success("Resumen de usuario", dto));
    }

    @GetMapping("/me")
    public ResponseEntity<RestResponse<DtoPerfilCompleto>> obtenerMiPerfil(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(RestResponse.error("No autenticado", null));
        }
        var usuario = userDetails.getUsuario();
        return perfilService.obtenerPerfilCompleto(usuario.getId())
                .map(dto -> ResponseEntity.ok(RestResponse.success("Perfil completo", dto)))
                .orElseGet(() -> ResponseEntity.status(404).body(RestResponse.error("Perfil no encontrado", null)));
    }

    @PatchMapping("/me")
    public ResponseEntity<RestResponse<DtoPerfilCompleto>> actualizarMiPerfil(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody DtoActualizarPerfilRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(RestResponse.error("No autenticado", null));
        }
        var usuario = userDetails.getUsuario();
        try {
            DtoPerfilCompleto actualizado = perfilService.actualizarPerfilCampos(usuario.getId(), request);
            return ResponseEntity.ok(RestResponse.success("Perfil actualizado", actualizado));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(RestResponse.error(ex.getMessage(), null));
        }
    }

    @PostMapping("/me/foto")
    public ResponseEntity<RestResponse<Map<String, String>>> actualizarFotoPerfil(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("archivo") MultipartFile archivo) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(RestResponse.error("No autenticado", null));
        }
        var usuario = userDetails.getUsuario();
        try {
            String url = perfilService.actualizarFotoPerfil(usuario.getId(), archivo);
            return ResponseEntity.ok(RestResponse.success("Foto de perfil actualizada", Map.of("url", url)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(RestResponse.error("Error al actualizar foto de perfil", null));
        }
    }

    /**
     * Obtiene perfil completo por id. ADMIN puede acceder a cualquiera. USER/REVIEWER sólo al suyo (controlado por SpEL).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.usuario.id")
    public ResponseEntity<RestResponse<DtoPerfilCompleto>> obtenerPerfilPorId(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(RestResponse.error("No autenticado", null));
        }
        return perfilService.obtenerPerfilCompleto(id)
                .map(dto -> ResponseEntity.ok(RestResponse.success("Perfil completo", dto)))
                .orElseGet(() -> ResponseEntity.status(404).body(RestResponse.error("Perfil no encontrado", null)));
    }

    /**
     * Actualiza perfil de cualquier usuario (solo ADMIN)
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<DtoPerfilCompleto>> actualizarPerfilDeUsuario(
            @PathVariable String id,
            @Valid @RequestBody DtoActualizarPerfilRequest request) {
        try {
            DtoPerfilCompleto actualizado = perfilService.actualizarPerfilCampos(id, request);
            return ResponseEntity.ok(RestResponse.success("Perfil actualizado", actualizado));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(RestResponse.error(ex.getMessage(), null));
        }
    }

    /**
     * Desactiva lógicamente un usuario por el ADMIN
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<Void>> desactivarUsuario(@PathVariable String id) {
        try {
            boolean cambiado = perfilService.desactivarUsuario(id);
            String msg = cambiado ? "Usuario desactivado" : "Usuario ya estaba inactivo";
            return ResponseEntity.ok(RestResponse.success(msg, (Void) null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(RestResponse.error("Usuario no encontrado", (Void) null));
        }
    }
}
