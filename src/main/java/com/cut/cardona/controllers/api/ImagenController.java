package com.cut.cardona.controllers.api;

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
import com.cut.cardona.infra.storage.ImageStorageService;
import com.cut.cardona.infra.storage.UploadResult;
import com.cut.cardona.infra.storage.StorageConfig;
import com.cut.cardona.modelo.dto.common.RestResponse;
import com.cut.cardona.modelo.imagenes.RepositorioImagenPerfil;
import com.cut.cardona.modelo.imagenes.ImagenPerfil;
import com.cut.cardona.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.nio.file.*;

@RestController
@RequestMapping("/api/imagenes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Imágenes", description = "Gestión de imágenes del sistema")
@Import(StorageConfig.class)
public class ImagenController {

    private static final int MAX_DOG_IMAGES = 5;

    private final ImageStorageService imageStorageService;
    private final RepositorioImagenPerfil repositorioImagenPerfil; // NUEVO

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

    // ====== SUBIDA DE IMÁGENES ======

    @Operation(summary = "Subir imagen de perro", description = "Sube una imagen y devuelve su id (UUID) para asociarla con un perro")
    @ApiResponse(responseCode = "200", description = "Imagen subida")
    @PostMapping(value = "/perritos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImagenPerro(
            @RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Dog-Images-Count", required = false) Integer countHdr,
            @RequestParam(value = "count", required = false) Integer countParam) {
        Integer count = countHdr != null ? countHdr : countParam;
        if (count != null && count >= MAX_DOG_IMAGES) {
            return ResponseEntity.status(422).body(RestResponse.error("Solo se permiten " + MAX_DOG_IMAGES + " imágenes por perro"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(RestResponse.error("Archivo vacío"));
        }
        // Volver a 15MB de entrada; el servicio comprimirá a ~9MB antes de subir
        if (file.getSize() > 15L * 1024 * 1024) {
            return ResponseEntity.status(422).body(RestResponse.error("El archivo supera el tamaño máximo de 15MB"));
        }
        try {
            UploadResult result = imageStorageService.uploadDogImage(file);
            Map<String, Object> resp = new HashMap<>();
            resp.put("id", result.getId());
            resp.put("filename", result.getFilename());
            resp.put("contentType", result.getContentType());
            resp.put("size", result.getSize());
            resp.put("url", result.getUrl());
            return ResponseEntity.ok(RestResponse.success("Imagen subida", resp));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(422).body(RestResponse.error(ex.getMessage()));
        } catch (Exception e) {
            log.error("Error subiendo imagen de perro", e);
            String raw = e.getMessage() != null ? e.getMessage() : "Error interno al guardar la imagen";
            if (raw.contains("File size too large")) {
                return ResponseEntity.status(422).body(RestResponse.error("El archivo supera el límite del proveedor tras el preprocesado"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RestResponse.error("No se pudo guardar la imagen: " + raw));
        }
    }

    // ====== IMÁGENES DE PERROS (local o proxy a CDN) ======

    @Operation(summary = "Obtener imagen de perro", description = "Devuelve la imagen de perro por id; local lee de disco, cloud hace proxy a CDN")
    @ApiResponse(responseCode = "200", description = "Imagen devuelta")
    @ApiResponse(responseCode = "404", description = "Imagen no encontrada")
    @GetMapping("/perritos/{id}")
    public ResponseEntity<byte[]> getImagenPerro(@PathVariable String id) {
        log.debug("[Imagenes] GET perritos id={}", id);
        if (!imageStorageService.isCloudProvider()) {
            // Leer desde almacenamiento local buscando por extensión conocida
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

    // Pequeño helper de proxy
    private record ProxyResult(byte[] body, String mediaType, long length) {}

    private record LocalFile(Path path, String contentType) {}

    private LocalFile findLocalDogFile(String id) {
        Path base = Paths.get(perrosDir);
        String[] exts = {"jpg", "jpeg", "png", "gif", "webp"};
        for (String ext : exts) {
            Path p = base.resolve(id + "." + ext);
            if (Files.exists(p)) {
                String ct = MIME_TYPES.getOrDefault(ext, "image/jpeg");
                return new LocalFile(p, ct);
            }
        }
        return null;
    }

    private ProxyResult fetchBinary(String url, String method) throws Exception {
        // Usar HttpClient del JDK para seguir redirecciones de Cloudinary
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
        String contentType = Optional.ofNullable(resp.headers().firstValue("content-type").orElse(null))
                .map(v -> v.split(";", 2)[0])
                .orElse(null);
        long len = -1;
        try {
            String cl = resp.headers().firstValue("content-length").orElse(null);
            if (cl != null) len = Long.parseLong(cl);
        } catch (Exception ignored) {}
        byte[] body = "HEAD".equalsIgnoreCase(method) ? new byte[0] : resp.body();
        return new ProxyResult(body, contentType, len);
    }
}
