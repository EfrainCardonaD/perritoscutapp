package com.cut.cardona.api.controller.adopcion;

import com.cut.cardona.modelo.dto.adopcion.DtoSolicitudAdopcion;
import com.cut.cardona.service.adopcion.AdminReviewerAdopcionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/adopciones/solicitudes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
public class AdminReviewerAdopcionController {

    private final AdminReviewerAdopcionService service;

    @GetMapping("/pendientes")
    public ResponseEntity<List<DtoSolicitudAdopcion>> pendientes() {
        return ResponseEntity.ok(service.pendientesRevision());
    }

    @GetMapping
    public ResponseEntity<List<DtoSolicitudAdopcion>> buscar(
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "perroId", required = false) String perroId,
            @RequestParam(value = "solicitanteId", required = false) String solicitanteId) {
        return ResponseEntity.ok(service.buscar(estado, perroId, solicitanteId));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<DtoSolicitudAdopcion> actualizarEstado(@PathVariable("id") String id, @RequestParam("estado") String estado) {
        return ResponseEntity.ok(service.actualizarEstado(id, estado));
    }

    @PostMapping("/{id}/revertir")
    public ResponseEntity<DtoSolicitudAdopcion> revertir(@PathVariable("id") String id) {
        return ResponseEntity.ok(service.revertirAdopcion(id));
    }

    @GetMapping("/{id}/documentos")
    public ResponseEntity<List<DtoSolicitudAdopcion.Documento>> listarDocumentos(@PathVariable("id") String id) {
        return ResponseEntity.ok(service.listarDocumentos(id));
    }

    @GetMapping("/{id}/documentos/{docId}")
    public ResponseEntity<DtoSolicitudAdopcion.Documento> obtenerDocumento(@PathVariable("id") String id, @PathVariable("docId") String docId) {
        return ResponseEntity.ok(service.obtenerDocumento(id, docId));
    }
}
