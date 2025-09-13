package com.cut.cardona.api.controller;

import com.cut.cardona.service.infra.storage.ImageStorageService;
import com.cut.cardona.service.infra.storage.StorageConfig;
import com.cut.cardona.service.perros.PerroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import com.cut.cardona.modelo.dto.common.RestResponse;
import com.cut.cardona.modelo.imagenes.RepositorioImagenPerfil;
import com.cut.cardona.modelo.imagenes.ImagenPerfil;
import com.cut.cardona.modelo.perros.RepositorioImagenPerro;
import com.cut.cardona.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.nio.file.*;
import com.cut.cardona.modelo.perros.ImagenPerro;


@RestController
@RequestMapping("/api/imagenes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Imágenes", description = "Gestión de imágenes del sistema")
@Import(StorageConfig.class)
public class ImagenController {

    private final ImageStorageService imageStorageService;
    private final RepositorioImagenPerfil repositorioImagenPerfil;
    private final RepositorioImagenPerro repositorioImagenPerro;
    private final PerroService perroService;

    @Value("${app.storage.perros-dir:uploads/perritos/}")
    private String perrosDir;

    private static final Map<String, String> MIME_TYPES = createMimeTypesMap();

    private static Map<String, String> createMimeTypesMap() {
        Map<String, String> mimeTypes = new HashMap<>();
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("jpeg", "image/jpeg");
        mimeTypes.put("png", "image/png");
        mimeTypes.put("gif", "image/gif");
        mimeTypes.put("webp", "image/webp");
        return mimeTypes;
    }

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

    // Endpoint antiguo BLOQUEADO para evitar huérfanas
    @Operation(summary = "[Deprecado] Subir imagen de perro sin id", description = "No permitido: se requiere perroId")
    @PostMapping(value = "/perritos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImagenPerroDeprecado() {
        return ResponseEntity.status(422).body(RestResponse.error("Debes subir la imagen a /api/imagenes/perritos/{perroId}"));
    }

    // ====== IMÁGENES DE PERROS (local o proxy a CDN) ======

    @Operation(summary = "Obtener imagen de perro", description = "Devuelve la imagen de perro por id; local lee de disco, cloud hace proxy a CDN")
    @ApiResponse(responseCode = "200", description = "Imagen devuelta")
    @ApiResponse(responseCode = "404", description = "Imagen no encontrada")
    @GetMapping("/perritos/{id}")
    public ResponseEntity<byte[]> getImagenPerro(@PathVariable String id) {
        log.debug("[Imagenes] GET perritos id={}", id);
        if (!imageStorageService.isCloudProvider()) {
            try {
                LocalFile lf = findLocalDogFile(id);
                if (lf == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                byte[] body = Files.readAllBytes(lf.path());
                MediaType mt = MediaType.parseMediaType(lf.contentType());
                return ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                        .contentType(mt)
                        .contentLength(body.length)
                        .body(body);
            } catch (Exception ex) {
                log.warn("Error leyendo imagen local {}: {}", id, ex.toString());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        }
        String url = imageStorageService.resolveDogImagePublicUrl(id);
        try {
            ProxyResult pr = fetchBinary(url, "GET");
            if (pr == null || pr.body == null || pr.body.length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            MediaType mt = pr.mediaType != null ? MediaType.parseMediaType(pr.mediaType) : MediaType.IMAGE_JPEG;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                    .contentType(mt)
                    .contentLength(pr.length >= 0 ? pr.length : pr.body.length)
                    .body(pr.body);
        } catch (Exception ex) {
            log.warn("No se pudo obtener imagen proxy {}: {}", id, ex.toString());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @Operation(summary = "HEAD imagen de perro", description = "Verifica si existe la imagen por id; local revisa disco, cloud hace proxy HEAD")
    @ApiResponse(responseCode = "200", description = "Existe")
    @RequestMapping(value = "/perritos/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headImagenPerro(@PathVariable String id) {
        log.debug("[Imagenes] HEAD perritos id={}", id);
        if (!imageStorageService.isCloudProvider()) {
            try {
                LocalFile lf = findLocalDogFile(id);
                if (lf == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                return ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                        .contentType(MediaType.parseMediaType(lf.contentType()))
                        .contentLength(Files.size(lf.path()))
                        .build();
            } catch (Exception ex) {
                log.warn("Error HEAD local {}: {}", id, ex.toString());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        }
        String url = imageStorageService.resolveDogImagePublicUrl(id);
        try {
            ProxyResult pr = fetchBinary(url, "HEAD");
            if (pr == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            MediaType mt = pr.mediaType != null ? MediaType.parseMediaType(pr.mediaType) : MediaType.IMAGE_JPEG;
            ResponseEntity.BodyBuilder bb = ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                    .contentType(mt);
            if (pr.length >= 0) bb.contentLength(pr.length);
            return bb.build();
        } catch (Exception ex) {
            log.warn("No se pudo hacer HEAD proxy {}: {}", id, ex.toString());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    // ====== IMÁGENES DE PERFIL (compat: filename -> id) ======

    @Operation(summary = "Obtener imagen de perfil", description = "Redirige a la URL CDN de la imagen de perfil por nombre de archivo (requiere ser dueño o admin)")
    @ApiResponse(responseCode = "302", description = "Redirección a la URL pública de la imagen")
    @ApiResponse(responseCode = "400", description = "Nombre de archivo inválido")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @ApiResponse(responseCode = "403", description = "No autorizado")
    @ApiResponse(responseCode = "404", description = "No encontrada")
    @GetMapping("/perfil/{filename:.+}")
    public ResponseEntity<Void> obtenerImagenPerfil(
            @Parameter(description = "Nombre del archivo de imagen", required = true) @PathVariable String filename,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String id = extraerId(filename);
        if (id == null) return ResponseEntity.badRequest().build();
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var imagenOpt = repositorioImagenPerfil.findByIdWithUsuario(id);
        if (imagenOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        ImagenPerfil imagen = imagenOpt.get();
        if (!estaAutorizadoImagenPerfil(userDetails, imagen)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String url = imageStorageService.resolveProfileImagePublicUrl(id);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }

    @Operation(summary = "Verificar disponibilidad de imagen", description = "HEAD de imagen de perfil (requiere ser dueño o admin)")
    @ApiResponse(responseCode = "302", description = "Redirección a la URL pública de la imagen")
    @ApiResponse(responseCode = "400", description = "Nombre de archivo inválido")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    @ApiResponse(responseCode = "403", description = "No autorizado")
    @ApiResponse(responseCode = "404", description = "No encontrada")
    @RequestMapping(value = "/perfil/{filename:.+}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> verificarImagenPerfil(
            @PathVariable String filename,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String id = extraerId(filename);
        if (id == null) return ResponseEntity.badRequest().build();
        if (userDetails == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        var imagenOpt = repositorioImagenPerfil.findByIdWithUsuario(id);
        if (imagenOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        ImagenPerfil imagen = imagenOpt.get();
        if (!estaAutorizadoImagenPerfil(userDetails, imagen)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String url = imageStorageService.resolveProfileImagePublicUrl(id);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }

    // ====== ELIMINACIÓN DE IMÁGENES ======

    @Operation(summary = "Eliminar imagen de perro", description = "Elimina una imagen por id. Si está asociada a un perro, requiere ser dueño o rol privilegiado; elimina asociación y archivo")
    @ApiResponse(responseCode = "200", description = "Imagen eliminada")
    @ApiResponse(responseCode = "404", description = "Imagen no encontrada en almacenamiento")
    @DeleteMapping("/perritos/{id}")
    public ResponseEntity<RestResponse<Void>> eliminarImagenPerro(@PathVariable String id, @AuthenticationPrincipal CustomUserDetails user) {
        try { java.util.UUID.fromString(id); } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(RestResponse.error("ID inválido", null));
        }
        var asociadaOpt = repositorioImagenPerro.findById(id);
        if (asociadaOpt.isPresent()) {
            ImagenPerro img = asociadaOpt.get();
            if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(RestResponse.error("No autenticado", null));
            boolean esDueno = img.getPerro() != null && img.getPerro().getUsuario() != null && img.getPerro().getUsuario().getId().equals(user.getUsuario().getId());
            boolean esAdmin = user.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_REVIEWER".equals(a.getAuthority()));
            if (!esDueno && !esAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(RestResponse.error("No autorizado", null));
            }
            try {
                repositorioImagenPerro.delete(img);
            } catch (Exception e) {
                log.warn("No se pudo borrar entidad ImagenPerro {}: {}", id, e.getMessage());
            }
        }
        try {
            if (imageStorageService.isCloudProvider()) {
                imageStorageService.deleteDogImage(id);
            } else {
                deleteLocalDogFiles(id);
            }
        } catch (Exception e) {
            log.warn("Error eliminando imagen perro {}: {}", id, e.getMessage());
        }
        return ResponseEntity.ok(RestResponse.success("Imagen eliminada (o ya inexistente)", null));
    }

    private void deleteLocalDogFiles(String id) {
        try {
            java.nio.file.Path base = java.nio.file.Paths.get(perrosDir);
            String[] exts = {"jpg", "jpeg", "png", "gif", "webp"};
            for (String ext : exts) {
                java.nio.file.Path p = base.resolve(id + "." + ext);
                if (java.nio.file.Files.exists(p)) {
                    try { java.nio.file.Files.delete(p); } catch (Exception ex) { log.warn("No se pudo borrar archivo {}: {}", p, ex.getMessage()); }
                }
            }
        } catch (Exception ex) {
            log.debug("deleteLocalDogFiles fallo silencioso id={}: {}", id, ex.getMessage());
        }
    }

    // ====== Helpers ======

    private String extraerId(String filename) {
        if (filename == null || filename.trim().isEmpty()) return null;
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) return null;
        int dot = filename.lastIndexOf('.');
        String base = dot > 0 ? filename.substring(0, dot) : filename;
        return base.isBlank() ? null : base;
    }

    private boolean estaAutorizadoImagenPerfil(CustomUserDetails userDetails, ImagenPerfil imagen) {
        if (userDetails == null || imagen == null) return false;
        String ownerId = imagen.getPerfilUsuario().getUsuario().getId();
        String requesterId = userDetails.getUsuario().getId();
        if (ownerId != null && ownerId.equals(requesterId)) return true;
        return userDetails.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private record ProxyResult(byte[] body, String mediaType, long length) {}
    private record LocalFile(Path path, String contentType) {}

    private LocalFile findLocalDogFile(String id) {
        Path base = Paths.get(perrosDir);
        String[] exts = {"jpg", "jpeg", "png", "gif", "webp"};
        try {
            for (String ext : exts) {
                Path p = base.resolve(id + "." + ext);
                if (Files.exists(p)) {
                    String ct = MIME_TYPES.getOrDefault(ext, "image/jpeg");
                    return new LocalFile(p, ct);
                }
            }
            if (Files.exists(base) && Files.isDirectory(base)) {
                try (var stream = Files.list(base)) {
                    Optional<Path> found = stream
                            .filter(Files::isRegularFile)
                            .filter(p -> {
                                String fn = p.getFileName().toString();
                                return fn.startsWith(id + ".") || fn.equals(id);
                            })
                            .findFirst();
                    if (found.isPresent()) {
                        Path p = found.get();
                        String fn = p.getFileName().toString();
                        int dot = fn.lastIndexOf('.');
                        String ext = dot > 0 ? fn.substring(dot + 1).toLowerCase() : "jpg";
                        String ct = MIME_TYPES.getOrDefault(ext, "image/jpeg");
                        return new LocalFile(p, ct);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("findLocalDogFile fallo id={}: {}", id, ex.getMessage());
        }
        return null;
    }

    private ProxyResult fetchBinary(String url, String method) throws java.io.IOException, InterruptedException {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();
        java.net.http.HttpRequest.Builder rb = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("User-Agent", "perritoscutapp/1.0");
        if ("HEAD".equalsIgnoreCase(method)) {
            rb.method("HEAD", java.net.http.HttpRequest.BodyPublishers.noBody());
        } else {
            rb.GET();
        }
        java.net.http.HttpResponse<byte[]> resp = client.send(rb.build(), java.net.http.HttpResponse.BodyHandlers.ofByteArray());
        int code = resp.statusCode();
        if (code >= 400) return null;
        String contentType = resp.headers().firstValue("content-type").map(v -> v.split(";", 2)[0]).orElse(null);
        long len = -1;
        try {
            var clOpt = resp.headers().firstValue("content-length");
            if (clOpt.isPresent()) len = Long.parseLong(clOpt.get());
        } catch (Exception ignored) {}
        byte[] body = "HEAD".equalsIgnoreCase(method) ? new byte[0] : resp.body();
        return new ProxyResult(body, contentType, len);
    }
}
