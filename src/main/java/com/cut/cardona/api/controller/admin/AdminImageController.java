package com.cut.cardona.api.controller.admin;

import com.cut.cardona.service.infra.storage.ImageStorageService;
import com.cut.cardona.modelo.perros.RepositorioImagenPerro;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminImageController {

    private final RepositorioImagenPerro repositorioImagenPerro;
    private final ImageStorageService imageStorageService;

    /**
     * Endpoint para limpiar imágenes huérfanas en Cloudinary.
     * Solo accesible por administradores.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cleanup-orphans")
    public ResponseEntity<String> cleanupOrphanImages() {
        int eliminadas = imageStorageService.cleanupOrphanImages(repositorioImagenPerro);
        if (eliminadas < 0) {
            return ResponseEntity.internalServerError().body("Error al limpiar imágenes huérfanas en Cloudinary");
        }
        return ResponseEntity.ok("Imágenes huérfanas eliminadas: " + eliminadas);
    }
}
