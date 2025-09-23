package com.cut.cardona.api.controller.perros;

import com.cut.cardona.modelo.dto.common.RestResponse;
import com.cut.cardona.modelo.dto.perros.DtoPerro;
import com.cut.cardona.service.admin.AdminService;
import com.cut.cardona.service.perros.PerroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/moderacion/perros")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
public class AdminReviewerPerrosController {

    private final AdminService adminService;
    private final PerroService perroService;

    /**
     * Lista perros filtrando opcionalmente por estado de revisión (APROBADO|PENDIENTE|RECHAZADO)
     */
    @GetMapping
    public ResponseEntity<RestResponse<List<DtoPerro>>> listar(@RequestParam(value = "revision", required = false) String revision) {
        List<DtoPerro> data = adminService.buscarPerrosPorRevision(revision);
        String msg = (revision == null || revision.isBlank()) ? "Perros cargados" : ("Perros en revisión: " + revision);
        return ResponseEntity.ok(RestResponse.success(msg, data));
    }

    /**
     * Lista únicamente los pendientes de revisión.
     */
    @GetMapping("/pendientes")
    public ResponseEntity<RestResponse<List<DtoPerro>>> pendientes() {
        List<DtoPerro> data = perroService.pendientesRevision();
        return ResponseEntity.ok(RestResponse.success("Perros pendientes de revisión", data));
    }
}

