package com.cut.cardona.api.controller.perros;

import com.cut.cardona.service.perros.PerroService;
import com.cut.cardona.modelo.dto.common.RestResponse;
import com.cut.cardona.modelo.dto.perros.ActualizarPerroRequest;
import com.cut.cardona.modelo.dto.perros.CrearPerroRequest;
import com.cut.cardona.modelo.dto.perros.DtoPerro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class PerroController {

    private final PerroService perroService;

    @GetMapping("/perros/catalogo")
    public ResponseEntity<RestResponse<List<DtoPerro>>> catalogo(
            @Pattern(regexp = "Macho|Hembra", message = "Sexo debe ser 'Macho' o 'Hembra'")
            @RequestParam(value = "sexo", required = false) String sexo,
            @Pattern(regexp = "Pequeño|Mediano|Grande", message = "Tamaño debe ser 'Pequeño','Mediano' o 'Grande'")
            @RequestParam(value = "tamano", required = false) String tamano,
            @Size(max = 255, message = "La ubicación no debe superar 255 caracteres")
            @RequestParam(value = "ubicacion", required = false) String ubicacion,
            @Min(value = 0, message = "page debe ser >= 0")
            @RequestParam(value = "page", required = false) Integer page,
            @Min(value = 1, message = "size debe ser >= 1") @Max(value = 100, message = "size no debe superar 100")
            @RequestParam(value = "size", required = false) Integer size) {
        List<DtoPerro> data = perroService.catalogoPublico(sexo, tamano, ubicacion, page, size);
        return ResponseEntity.ok(RestResponse.success("Catálogo cargado", data));
    }

    @GetMapping("/perros/mis")
    public ResponseEntity<RestResponse<List<DtoPerro>>> misPerros() {
        List<DtoPerro> data = perroService.perrosDelUsuarioActual();
        return ResponseEntity.ok(RestResponse.success("Perros del usuario", data));
    }

    @PostMapping("/perros")
    public ResponseEntity<RestResponse<DtoPerro>> crear(@Valid @RequestBody CrearPerroRequest req) {
        DtoPerro creado = perroService.crearPerro(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestResponse.success("Perro creado correctamente", creado));
    }

    @PostMapping("/admin/perros/{id}/aprobar")
    @PreAuthorize("hasRole('ADMIN, REVIEWER')")
    public ResponseEntity<RestResponse<DtoPerro>> aprobar(@PathVariable("id") String id) {
        DtoPerro dto = perroService.aprobarPerro(id);
        return ResponseEntity.ok(RestResponse.success("Perro aprobado", dto));
    }

    @PreAuthorize("hasRole('ADMIN, REVIEWER')")
    @PostMapping("/admin/perros/{id}/rechazar")
    public ResponseEntity<RestResponse<DtoPerro>> rechazar(@PathVariable("id") String id) {
        DtoPerro dto = perroService.rechazarPerro(id);
        return ResponseEntity.ok(RestResponse.success("Perro rechazado", dto));
    }

    @PreAuthorize("hasRole('ADMIN, REVIEWER')")
    @PatchMapping("/admin/perros/{id}/estado")
    public ResponseEntity<RestResponse<DtoPerro>> cambiarEstado(@PathVariable("id") String id, @RequestParam("estado") String estado) {
        DtoPerro dto = perroService.cambiarEstadoAdopcion(id, estado);
        return ResponseEntity.ok(RestResponse.success("Estado de adopción actualizado", dto));
    }


    @GetMapping("/perros/{id}")
    public ResponseEntity<RestResponse<DtoPerro>> detalle(@PathVariable("id") String id) {
        // TODO(Fase3): añadir validaciones adicionales (ej. visibilidad si se introducen estados privados)
        DtoPerro dto = perroService.obtenerPerro(id);
        return ResponseEntity.ok(RestResponse.success("Detalle perro", dto));
    }


    @PatchMapping("/perros/{id}")
    public ResponseEntity<RestResponse<DtoPerro>> actualizar(@PathVariable("id") String id,
                                                             @Valid @RequestBody ActualizarPerroRequest req) {
        DtoPerro dto = perroService.actualizarPerro(id, req);
        return ResponseEntity.ok(RestResponse.success("Perro actualizado", dto));
    }

    @DeleteMapping("/perros/{id}")
    public ResponseEntity<RestResponse<Void>> eliminar(@PathVariable("id") String id) {
        perroService.eliminarPerro(id);
        return ResponseEntity.ok(RestResponse.success("Perro eliminado", null));
    }
}
