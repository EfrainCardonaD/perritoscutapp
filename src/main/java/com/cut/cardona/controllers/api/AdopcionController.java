package com.cut.cardona.controllers.api;

import com.cut.cardona.controllers.service.AdopcionService;
import com.cut.cardona.modelo.dto.adopcion.CrearSolicitudRequest;
import com.cut.cardona.modelo.dto.adopcion.DocumentoRequest;
import com.cut.cardona.modelo.dto.adopcion.DtoSolicitud;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdopcionController {

    private final AdopcionService adopcionService;

    @PostMapping("/solicitudes")
    public ResponseEntity<DtoSolicitud> crearSolicitud(@Valid @RequestBody CrearSolicitudRequest req) {
        return ResponseEntity.ok(adopcionService.crearSolicitud(req));
    }

    @PostMapping("/solicitudes/{id}/documentos")
    public ResponseEntity<Void> subirDocumento(@PathVariable("id") String solicitudId, @Valid @RequestBody DocumentoRequest req) {
        adopcionService.subirDocumento(solicitudId, req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/solicitudes/mis")
    public ResponseEntity<List<DtoSolicitud>> misSolicitudes() {
        return ResponseEntity.ok(adopcionService.misSolicitudes());
    }

    @GetMapping("/admin/solicitudes/pendientes")
    public ResponseEntity<List<DtoSolicitud>> pendientes() {
        return ResponseEntity.ok(adopcionService.pendientesRevision());
    }

    @PatchMapping("/admin/solicitudes/{id}/estado")
    public ResponseEntity<DtoSolicitud> actualizarEstado(@PathVariable("id") String id, @RequestParam("estado") String estado) {
        return ResponseEntity.ok(adopcionService.actualizarEstado(id, estado));
    }

    @GetMapping("/solicitudes/{id}")
    public ResponseEntity<DtoSolicitud> obtenerSolicitud(@PathVariable("id") String id) {
        return ResponseEntity.ok(adopcionService.obtenerSolicitud(id));
    }

    @DeleteMapping("/solicitudes/{id}")
    public ResponseEntity<Void> eliminarSolicitud(@PathVariable("id") String id) {
        adopcionService.eliminarSolicitud(id);
        return ResponseEntity.ok().build();
    }
}
