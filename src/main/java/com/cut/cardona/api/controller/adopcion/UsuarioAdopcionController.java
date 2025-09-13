package com.cut.cardona.api.controller.adopcion;

import com.cut.cardona.modelo.dto.adopcion.DtoSolicitudAdopcion;
import com.cut.cardona.service.adopcion.UsuarioAdopcionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/usuario/adopciones/solicitudes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class UsuarioAdopcionController {

    private final UsuarioAdopcionService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DtoSolicitudAdopcion> crear(
            @RequestParam("perroId") String perroId,
            @RequestParam(value = "mensaje", required = false) String mensaje,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "tipoDocumento", required = false) String tipoDocumento) throws Exception {
        return ResponseEntity.ok(service.crearSolicitud(perroId, mensaje, tipoDocumento, file));
    }

    @GetMapping("/mis")
    public ResponseEntity<List<DtoSolicitudAdopcion>> mis() {
        return ResponseEntity.ok(service.misSolicitudes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DtoSolicitudAdopcion> detalle(@PathVariable("id") String id) {
        return ResponseEntity.ok(service.obtenerSolicitud(id));
    }

    @PatchMapping("/{id}/mensaje")
    public ResponseEntity<DtoSolicitudAdopcion> actualizarMensaje(@PathVariable("id") String id, @Valid @RequestBody DtoSolicitudAdopcion req) {
        return ResponseEntity.ok(service.actualizarMensaje(id, req));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<DtoSolicitudAdopcion> cancelar(@PathVariable("id") String id) {
        return ResponseEntity.ok(service.cancelarSolicitud(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") String id) {
        service.eliminarSolicitud(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/documentos")
    public ResponseEntity<List<DtoSolicitudAdopcion.Documento>> listarDocumentos(@PathVariable("id") String solicitudId) {
        return ResponseEntity.ok(service.listarDocumentos(solicitudId));
    }

    @GetMapping("/{id}/documentos/{docId}")
    public ResponseEntity<DtoSolicitudAdopcion.Documento> obtenerDocumento(@PathVariable("id") String solicitudId, @PathVariable("docId") String docId) {
        return ResponseEntity.ok(service.obtenerDocumento(solicitudId, docId));
    }
}
