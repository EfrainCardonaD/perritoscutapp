package com.cut.cardona.controllers.api;

import com.cut.cardona.controllers.service.PerroService;
import com.cut.cardona.modelo.dto.perros.CrearPerroRequest;
import com.cut.cardona.modelo.dto.perros.DtoPerro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<DtoPerro>> catalogo(
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
        return ResponseEntity.ok(perroService.catalogoPublico(sexo, tamano, ubicacion, page, size));

    }

    @GetMapping("/perros/mis")
    public ResponseEntity<List<DtoPerro>> misPerros() {
        return ResponseEntity.ok(perroService.perrosDelUsuarioActual());
    }

    @PostMapping("/perros")
    public ResponseEntity<DtoPerro> crear(@Valid @RequestBody CrearPerroRequest req) {
        return ResponseEntity.ok(perroService.crearPerro(req));
    }

    @PostMapping("/admin/perros/{id}/aprobar")
    public ResponseEntity<DtoPerro> aprobar(@PathVariable("id") String id) {
        return ResponseEntity.ok(perroService.aprobarPerro(id));
    }

    @PostMapping("/admin/perros/{id}/rechazar")
    public ResponseEntity<DtoPerro> rechazar(@PathVariable("id") String id) {
        return ResponseEntity.ok(perroService.rechazarPerro(id));
    }

    @PatchMapping("/admin/perros/{id}/estado")
    public ResponseEntity<DtoPerro> cambiarEstado(@PathVariable("id") String id, @RequestParam("estado") String estado) {
        return ResponseEntity.ok(perroService.cambiarEstadoAdopcion(id, estado));
    }
}
