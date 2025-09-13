package com.cut.cardona.controllers.api;

import com.cut.cardona.controllers.service.UserService;
import com.cut.cardona.modelo.dto.common.RestResponse;
import com.cut.cardona.modelo.dto.usuarios.DtoUsuarioResumen;
import com.cut.cardona.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Slf4j
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuariosController {

    private final UserService userService;

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
        var dto = userService.toResumen(usuario);
        log.debug("Resumen solicitado para usuario {}", usuario.getUsername());
        return ResponseEntity.ok(RestResponse.success("Resumen de usuario", dto));
    }

    /**
     * Desactiva lógicamente un usuario por el ADMIN
     */
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RestResponse<Void>> desactivarUsuario(@PathVariable String id) {
        try {
            boolean cambiado = userService.desactivarUsuario(id);
            String msg = cambiado ? "Usuario desactivado" : "Usuario ya estaba inactivo";
            return ResponseEntity.ok(RestResponse.success(msg, (Void) null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(RestResponse.error("Usuario no encontrado", (Void) null));
        }
    }
}
