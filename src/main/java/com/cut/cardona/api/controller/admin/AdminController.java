package com.cut.cardona.api.controller.admin;

import com.cut.cardona.modelo.dto.common.RestResponse;
import com.cut.cardona.modelo.dto.perfil.DtoPerfilCompleto;
import com.cut.cardona.modelo.dto.perros.DtoPerro;
import com.cut.cardona.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // --- Usuarios ---
    @GetMapping("/usuarios")
    public ResponseEntity<RestResponse<List<DtoPerfilCompleto>>> listarUsuarios() {
        List<DtoPerfilCompleto> data = adminService.listarUsuarios();
        return ResponseEntity.ok(RestResponse.success("Usuarios cargados", data));
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<RestResponse<DtoPerfilCompleto>> obtenerUsuario(@PathVariable("id") String id) {
        return adminService.obtenerUsuario(id)
                .map(dto -> ResponseEntity.ok(RestResponse.success("Detalle de usuario", dto)))
                .orElseGet(() -> ResponseEntity.status(404).body(RestResponse.<DtoPerfilCompleto>error("Usuario no encontrado", null)));
    }

    @PatchMapping("/usuarios/{id}/rol")
    public ResponseEntity<RestResponse<DtoPerfilCompleto>> cambiarRol(@PathVariable("id") String id,
                                                                      @RequestParam("rol") String rol) {
        DtoPerfilCompleto dto = adminService.cambiarRol(id, rol);
        return ResponseEntity.ok(RestResponse.success("Rol actualizado", dto));
    }

    @PostMapping("/usuarios/{id}/desactivar")
    public ResponseEntity<RestResponse<Void>> desactivar(@PathVariable("id") String id) {
        boolean desactivado = adminService.desactivarUsuario(id);
        String mensaje = desactivado ? "Usuario desactivado" : "Usuario ya estaba inactivo";
        return ResponseEntity.ok(RestResponse.success(mensaje));
    }

    @PostMapping("/usuarios/{id}/activar")
    public ResponseEntity<RestResponse<DtoPerfilCompleto>> activar(@PathVariable("id") String id) {
        DtoPerfilCompleto dto = adminService.activarUsuario(id);
        return ResponseEntity.ok(RestResponse.success("Usuario activado", dto));
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<RestResponse<Void>> eliminarUsuario(@PathVariable("id") String id) {
        adminService.eliminarUsuarioFisico(id);
        return ResponseEntity.ok(RestResponse.success("Usuario eliminado"));
    }

    // --- Perros ---
    @GetMapping("/perros")
    public ResponseEntity<RestResponse<List<DtoPerro>>> listarPerros(
            @RequestParam(value = "revision", required = false) String revision) {
        List<DtoPerro> data = adminService.buscarPerrosPorRevision(revision);
        String msg = (revision == null || revision.isBlank()) ? "Perros cargados" : ("Perros en revisión: " + revision);
        return ResponseEntity.ok(RestResponse.success(msg, data));
    }

    @DeleteMapping("/perros/{id}")
    public ResponseEntity<RestResponse<Void>> eliminarPerro(@PathVariable("id") String id) {
        adminService.eliminarPerro(id);
        return ResponseEntity.ok(RestResponse.success("Perro eliminado"));
    }


}
