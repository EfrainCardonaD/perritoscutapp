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
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // --- Usuarios ---
    @GetMapping("/usuarios")
    public ResponseEntity<RestResponse<List<DtoPerfilCompleto>>> listarUsuarios(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size
    ) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        // cap para evitar requests excesivos
        final int MAX_SIZE = 200;
        if (size > MAX_SIZE) size = MAX_SIZE;

        List<DtoPerfilCompleto> all = adminService.listarUsuarios();
        int total = all == null ? 0 : all.size();
        int totalPages = total == 0 ? 0 : ((total + size - 1) / size);
        int from = page * size;
        List<DtoPerfilCompleto> pageContent;
        if (all == null || from >= total) {
            pageContent = Collections.emptyList();
        } else {
            int to = Math.min(from + size, total);
            pageContent = all.subList(from, to);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("page", page);
        metadata.put("size", size);
        metadata.put("total", total);
        metadata.put("totalPages", totalPages);

        return ResponseEntity.ok(RestResponse.withMetadata("Usuarios cargados", pageContent, metadata));
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
            @RequestParam(value = "revision", required = false) String revision,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "20") int size
    ) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        final int MAX_SIZE = 200;
        if (size > MAX_SIZE) size = MAX_SIZE;

        List<DtoPerro> all = adminService.buscarPerrosPorRevision(revision);
        int total = all == null ? 0 : all.size();
        int totalPages = total == 0 ? 0 : ((total + size - 1) / size);
        int from = page * size;
        List<DtoPerro> pageContent;
        if (all == null || from >= total) {
            pageContent = Collections.emptyList();
        } else {
            int to = Math.min(from + size, total);
            pageContent = all.subList(from, to);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("page", page);
        metadata.put("size", size);
        metadata.put("total", total);
        metadata.put("totalPages", totalPages);

        String msg = (revision == null || revision.isBlank()) ? "Perros cargados" : ("Perros en revisión: " + revision);
        return ResponseEntity.ok(RestResponse.withMetadata(msg, pageContent, metadata));
    }

    @DeleteMapping("/perros/{id}")
    public ResponseEntity<RestResponse<Void>> eliminarPerro(@PathVariable("id") String id) {
        adminService.eliminarPerro(id);
        return ResponseEntity.ok(RestResponse.success("Perro eliminado"));
    }


}
