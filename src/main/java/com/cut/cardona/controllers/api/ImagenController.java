package com.cut.cardona.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/imagenes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Imágenes", description = "Gestión de imágenes del sistema")
public class ImagenController {

    private static final String PERFIL_UPLOAD_DIR = "uploads/perfiles/";
    @Value("${app.storage.perros-dir:uploads/perritos/}")
    private String perrosUploadDir;
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

    // ====== IMÁGENES DE PERFIL EXISTENTES ======

    @Operation(summary = "Obtener imagen de perfil", description = "Devuelve una imagen de perfil por su nombre de archivo")
    @ApiResponse(responseCode = "200", description = "Imagen encontrada y devuelta")
    @ApiResponse(responseCode = "404", description = "Imagen no encontrada")
    @ApiResponse(responseCode = "400", description = "Nombre de archivo inválido")
    @GetMapping("/perfil/{filename:.+}")
    public ResponseEntity<Resource> obtenerImagenPerfil(@Parameter(description = "Nombre del archivo de imagen", required = true) @PathVariable String filename) {
        return serveByFilename(PERFIL_UPLOAD_DIR, filename);
    }

    @Operation(summary = "Verificar disponibilidad de imagen", description = "Verifica si una imagen de perfil existe sin descargarla")
    @ApiResponse(responseCode = "200", description = "Imagen existe")
    @ApiResponse(responseCode = "404", description = "Imagen no encontrada")
    @RequestMapping(value = "/perfil/{filename:.+}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> verificarImagenPerfil(@PathVariable String filename) {
        return headByFilename(PERFIL_UPLOAD_DIR, filename);
    }

    // ====== IMÁGENES DE PERROS (LOCAL POR ID) ======

    @Operation(summary = "Subir imagen de perro", description = "Sube una imagen y devuelve su id (UUID) para asociarla con un perro")
    @ApiResponse(responseCode = "200", description = "Imagen subida")
    @PostMapping(value = "/perritos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadImagenPerro(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Archivo vacío"));
        }
        try {
            crearDirectorioSiNoExiste(perrosUploadDir);
            String original = Objects.requireNonNullElse(file.getOriginalFilename(), "archivo");
            String ext = obtenerExtension(original);
            if (!MIME_TYPES.containsKey(ext.toLowerCase())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Extensión no soportada"));
            }
            String id = UUID.randomUUID().toString();
            String filename = id + "." + ext.toLowerCase();
            Path destino = Paths.get(perrosUploadDir).resolve(filename).normalize();
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            Map<String, Object> resp = new HashMap<>();
            resp.put("id", id);
            resp.put("filename", filename);
            resp.put("contentType", MIME_TYPES.get(ext.toLowerCase()));
            resp.put("size", file.getSize());
            resp.put("url", "/api/imagenes/perritos/" + id);
            return ResponseEntity.ok(resp);
        } catch (IOException e) {
            log.error("Error subiendo imagen de perro", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "No se pudo guardar la imagen"));
        }
    }

    @Operation(summary = "Obtener imagen de perro", description = "Devuelve la imagen de perro por id")
    @ApiResponse(responseCode = "200", description = "Imagen encontrada")
    @ApiResponse(responseCode = "404", description = "Imagen no encontrada")
    @GetMapping("/perritos/{id}")
    public ResponseEntity<Resource> getImagenPerro(@PathVariable String id) {
        log.debug("[Imagenes] GET perritos id={} dir={}", id, perrosUploadDir);
        return serveById(perrosUploadDir, id);
    }

    @Operation(summary = "HEAD imagen de perro", description = "Verifica si existe la imagen por id")
    @ApiResponse(responseCode = "200", description = "Existe")
    @ApiResponse(responseCode = "404", description = "No existe")
    @RequestMapping(value = "/perritos/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headImagenPerro(@PathVariable String id) {
        log.debug("[Imagenes] HEAD perritos id={} dir={}", id, perrosUploadDir);
        return headById(perrosUploadDir, id);
    }

    // ====== Helpers comunes ======

    private ResponseEntity<Resource> serveByFilename(String baseDir, String filename) {
        try {
            if (!esNombreArchivoValido(filename)) {
                return ResponseEntity.badRequest().build();
            }
            crearDirectorioSiNoExiste(baseDir);
            Path rutaArchivo = Paths.get(baseDir).resolve(filename).normalize();
            if (!Files.exists(rutaArchivo) || !Files.isReadable(rutaArchivo)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(rutaArchivo.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = determinarTipoContenido(filename);
            HttpHeaders headers = standardHeaders(contentType, filename);
            return ResponseEntity.ok().headers(headers).body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error sirviendo imagen por filename", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<Void> headByFilename(String baseDir, String filename) {
        try {
            if (!esNombreArchivoValido(filename)) return ResponseEntity.badRequest().build();
            Path rutaArchivo = Paths.get(baseDir).resolve(filename).normalize();
            if (Files.exists(rutaArchivo) && Files.isReadable(rutaArchivo)) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error en HEAD por filename", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<Resource> serveById(String baseDir, String id) {
        try {
            crearDirectorioSiNoExiste(baseDir);
            Optional<Path> archivo = buscarArchivoPorId(baseDir, id);
            if (archivo.isEmpty()) {
                log.debug("[Imagenes] No encontrado id={} en dir={}", id, baseDir);
                return ResponseEntity.notFound().build();
            }
            Path ruta = archivo.get();
            Resource resource = new UrlResource(ruta.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.debug("[Imagenes] Recurso no legible: {}", ruta);
                return ResponseEntity.notFound().build();
            }
            String contentType = determinarTipoContenido(ruta.getFileName().toString());
            HttpHeaders headers = standardHeaders(contentType, ruta.getFileName().toString());
            log.debug("[Imagenes] Sirviendo {} como {}", ruta.getFileName(), contentType);
            return ResponseEntity.ok().headers(headers).body(resource);
        } catch (Exception e) {
            log.error("Error sirviendo imagen por id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<Void> headById(String baseDir, String id) {
        try {
            Optional<Path> archivo = buscarArchivoPorId(baseDir, id);
            return archivo.isPresent() ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error HEAD por id", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Optional<Path> buscarArchivoPorId(String baseDir, String id) throws IOException {
        Path dir = Paths.get(baseDir);
        if (!Files.exists(dir)) return Optional.empty();
        // Evitar depender del orden del sistema de archivos o del glob; comparar nombre-base exactamente
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (!Files.isRegularFile(p) || !Files.isReadable(p)) continue;
                String name = p.getFileName().toString();
                int dot = name.lastIndexOf('.');
                if (dot <= 0) continue;
                String base = name.substring(0, dot);
                log.debug("[Imagenes] Inspeccionando archivo={} base={} targetId={}", name, base, id);
                if (base.equals(id)) {
                    log.debug("[Imagenes] Coincidencia exacta id={} -> {}", id, name);
                    return Optional.of(p);
                }
            }
        }
        log.debug("[Imagenes] No hubo coincidencia exacta para id={} en {}", id, baseDir);
        return Optional.empty();
    }

    private HttpHeaders standardHeaders(String contentType, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        headers.add(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
        headers.add("X-Content-Type-Options", "nosniff");
        return headers;
    }

    private void crearDirectorioSiNoExiste(String baseDir) throws IOException {
        Path uploadPath = Paths.get(baseDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Directorio de uploads creado: {}", uploadPath.toAbsolutePath());
        }
    }

    private boolean esNombreArchivoValido(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return false;
        }
        String extension = obtenerExtension(filename);
        return MIME_TYPES.containsKey(extension.toLowerCase());
    }

    private String determinarTipoContenido(String filename) {
        String extension = obtenerExtension(filename);
        return MIME_TYPES.getOrDefault(extension.toLowerCase(), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private String obtenerExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }
}
