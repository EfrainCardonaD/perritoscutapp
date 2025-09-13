package com.cut.cardona.api.controller;

import com.cut.cardona.service.infra.storage.ImageStorageService;
import com.cut.cardona.service.infra.storage.StorageConfig;
import com.cut.cardona.service.perros.PerroService;
import com.cut.cardona.service.imagenes.ImagenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import com.cut.cardona.modelo.dto.common.RestResponse;
import com.cut.cardona.modelo.perros.ImagenPerro;
import com.cut.cardona.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/imagenes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Imágenes", description = "Gestión de imágenes del sistema")
@Import(StorageConfig.class)
public class ImagenController {

    private final ImageStorageService imageStorageService; // solo para resolver URL post-subida
    private final PerroService perroService;
    private final ImagenService imagenService; // nueva capa con la lógica extraída

    // ====== SUBIDA DE IMÁGENES (requiere perroId) ======
    @Operation(summary = "Subir imagen de perro (requiere perroId)", description = "Sube y asocia una imagen a un perro existente, validando límites y permisos")
    @ApiResponse(responseCode = "200", description = "Imagen subida y asociada")
    @PostMapping(value = "/perritos/{perroId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImagenPerroVinculada(
            @PathVariable("perroId") String perroId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "principal", required = false, defaultValue = "false") Boolean principal) {
        try {
            ImagenPerro img = perroService.agregarImagen(perroId, file, descripcion, principal);
            Map<String, Object> data = new HashMap<>();
            data.put("id", img.getId());
            data.put("descripcion", img.getDescripcion());
            data.put("principal", img.getPrincipal());
            data.put("perroId", perroId);
            data.put("url", imageStorageService.resolveDogImagePublicUrl(img.getId()));
            return ResponseEntity.ok(RestResponse.success("Imagen subida", data));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(400).body(RestResponse.error(ex.getMessage()));
        } catch (com.cut.cardona.errores.UnprocessableEntityException ex) {
            return ResponseEntity.status(422).body(RestResponse.error(ex.getMessage()));
        } catch (SecurityException ex) {
            return ResponseEntity.status(403).body(RestResponse.error(ex.getMessage()));
        } catch (Exception e) {
            log.error("Error subiendo imagen de perro y asociando", e);
            String raw = e.getMessage() != null ? e.getMessage() : "Error interno al guardar la imagen";
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RestResponse.error("No se pudo guardar la imagen: " + raw));
        }
    }

    @Operation(summary = "[Deprecado] Subir imagen de perro sin id", description = "No permitido: se requiere perroId")
    @PostMapping(value = "/perritos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImagenPerroDeprecado() {
        return ResponseEntity.status(422).body(RestResponse.error("Debes subir la imagen a /api/imagenes/perritos/{perroId}"));
    }

    // ====== IMÁGENES DE PERROS (GET / HEAD) ======
    @Operation(summary = "Obtener imagen de perro", description = "Devuelve la imagen de perro por id; local lee de disco, cloud proxy a CDN")
    @ApiResponse(responseCode = "200", description = "Imagen devuelta")
    @ApiResponse(responseCode = "404", description = "Imagen no encontrada")
    @GetMapping("/perritos/{id}")
    public ResponseEntity<byte[]> getImagenPerro(@PathVariable String id) {
        log.debug("[Imagenes] GET perritos id={}", id);
        var r = imagenService.obtenerContenidoImagenPerro(id);
        if (r.status() == HttpStatus.OK) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                    .contentType(MediaType.parseMediaType(r.mediaType()))
                    .contentLength(r.length())
                    .body(r.body());
        }
        if (r.status() == HttpStatus.NOT_FOUND) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }

    @Operation(summary = "HEAD imagen de perro", description = "Verifica si existe la imagen por id")
    @ApiResponse(responseCode = "200", description = "Existe")
    @RequestMapping(value = "/perritos/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headImagenPerro(@PathVariable String id) {
        log.debug("[Imagenes] HEAD perritos id={}", id);
        var r = imagenService.obtenerHeadImagenPerro(id);
        if (r.status() == HttpStatus.OK) {
            ResponseEntity.BodyBuilder bb = ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                    .contentType(MediaType.parseMediaType(r.mediaType()));
            if (r.length() >= 0) bb.contentLength(r.length());
            return bb.build();
        }
        if (r.status() == HttpStatus.NOT_FOUND) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }

    // ====== IMÁGENES DE PERFIL (redirect) ======
    @Operation(summary = "Obtener imagen de perfil", description = "Redirige a la URL pública si autorizado (dueño o admin)")
    @ApiResponse(responseCode = "302", description = "Redirección a la URL pública")
    @GetMapping("/perfil/{filename:.+}")
    public ResponseEntity<Void> obtenerImagenPerfil(
            @Parameter(description = "Nombre del archivo", required = true) @PathVariable String filename,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var r = imagenService.obtenerUrlImagenPerfilAutorizada(filename, userDetails);
        if (r.status() == HttpStatus.FOUND) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, r.redirectUrl())
                    .build();
        }
        return ResponseEntity.status(r.status()).build();
    }

    @Operation(summary = "Verificar disponibilidad imagen perfil", description = "HEAD simulando redirección si autorizado")
    @RequestMapping(value = "/perfil/{filename:.+}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> verificarImagenPerfil(
            @PathVariable String filename,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        var r = imagenService.obtenerUrlImagenPerfilAutorizada(filename, userDetails);
        if (r.status() == HttpStatus.FOUND) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, r.redirectUrl())
                    .build();
        }
        return ResponseEntity.status(r.status()).build();
    }

    // ====== ELIMINACIÓN DE IMAGEN PERRO ======
    @Operation(summary = "Eliminar imagen de perro", description = "Elimina la imagen (requiere dueño o privilegio si asociada)")
    @DeleteMapping("/perritos/{id}")
    public ResponseEntity<RestResponse<Void>> eliminarImagenPerro(@PathVariable String id, @AuthenticationPrincipal CustomUserDetails user) {
        var r = imagenService.eliminarImagenPerro(id, user);
        if (r.status() == HttpStatus.OK) return ResponseEntity.ok(RestResponse.success(r.mensaje(), null));
        if (r.status() == HttpStatus.BAD_REQUEST) return ResponseEntity.badRequest().body(RestResponse.error(r.mensaje(), null));
        if (r.status() == HttpStatus.UNAUTHORIZED) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(RestResponse.error(r.mensaje(), null));
        if (r.status() == HttpStatus.FORBIDDEN) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(RestResponse.error(r.mensaje(), null));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RestResponse.error("Error inesperado", null));
    }
}
