package com.cut.cardona.service.imagenes;

import com.cut.cardona.modelo.imagenes.ImagenPerfil;
import com.cut.cardona.modelo.imagenes.RepositorioImagenPerfil;
import com.cut.cardona.modelo.perros.ImagenPerro;
import com.cut.cardona.modelo.perros.RepositorioImagenPerro;
import com.cut.cardona.security.CustomUserDetails;
import com.cut.cardona.service.infra.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * ImagenService encapsula la lógica de negocio y acceso a almacenamiento
 * para imágenes de perros y de perfil. El controlador solo construye la respuesta HTTP.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImagenService {

    private final ImageStorageService imageStorageService;
    private final RepositorioImagenPerfil repositorioImagenPerfil;
    private final RepositorioImagenPerro repositorioImagenPerro;

    @Value("${app.storage.perros-dir:uploads/perritos/}")
    private String perrosDir;

    private static final Map<String, String> MIME_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp"
    );

    // ================== LÓGICA IMÁGEN PERRO (GET) ==================

    public ImagenPerroContenidoResult obtenerContenidoImagenPerro(String id) {
        if (!imageStorageService.isCloudProvider()) {
            LocalFile lf = findLocalDogFile(id);
            if (lf == null) return ImagenPerroContenidoResult.notFound();
            try {
                byte[] body = Files.readAllBytes(lf.path());
                return ImagenPerroContenidoResult.ok(body, lf.contentType(), body.length);
            } catch (IOException e) {
                log.warn("Error leyendo imagen local {}: {}", id, e.toString());
                return ImagenPerroContenidoResult.notFound();
            }
        }
        String url = imageStorageService.resolveDogImagePublicUrl(id);
        try {
            ProxyResult pr = fetchBinary(url, true);
            if (pr == null || pr.body == null || pr.body.length == 0) return ImagenPerroContenidoResult.notFound();
            String mt = pr.mediaType != null ? pr.mediaType : MediaType.IMAGE_JPEG_VALUE;
            long len = pr.length >= 0 ? pr.length : pr.body.length;
            return ImagenPerroContenidoResult.ok(pr.body, mt, len);
        } catch (Exception ex) {
            log.warn("Fallo proxy GET {}: {}", id, ex.toString());
            return ImagenPerroContenidoResult.badGateway();
        }
    }

    public ImagenPerroHeadResult obtenerHeadImagenPerro(String id) {
        if (!imageStorageService.isCloudProvider()) {
            LocalFile lf = findLocalDogFile(id);
            if (lf == null) return ImagenPerroHeadResult.notFound();
            try {
                long len = Files.size(lf.path());
                return ImagenPerroHeadResult.ok(lf.contentType(), len);
            } catch (IOException e) {
                log.warn("Error HEAD local {}: {}", id, e.toString());
                return ImagenPerroHeadResult.notFound();
            }
        }
        String url = imageStorageService.resolveDogImagePublicUrl(id);
        try {
            ProxyResult pr = fetchBinary(url, false);
            if (pr == null) return ImagenPerroHeadResult.notFound();
            String mt = pr.mediaType != null ? pr.mediaType : MediaType.IMAGE_JPEG_VALUE;
            return ImagenPerroHeadResult.ok(mt, pr.length);
        } catch (Exception ex) {
            log.warn("Fallo proxy HEAD {}: {}", id, ex.toString());
            return ImagenPerroHeadResult.badGateway();
        }
    }

    // ================== LÓGICA IMÁGEN PERFIL (REDIRECT) ==================

    public PerfilImagenResult obtenerUrlImagenPerfilAutorizada(String filename, CustomUserDetails userDetails) {
        String id = extraerId(filename);
        if (id == null) return PerfilImagenResult.of(HttpStatus.BAD_REQUEST, null);
        if (userDetails == null) return PerfilImagenResult.of(HttpStatus.UNAUTHORIZED, null);
        var imagenOpt = repositorioImagenPerfil.findByIdWithUsuario(id);
        if (imagenOpt.isEmpty()) return PerfilImagenResult.of(HttpStatus.NOT_FOUND, null);
        ImagenPerfil imagen = imagenOpt.get();
        if (!estaAutorizadoImagenPerfil(userDetails, imagen)) return PerfilImagenResult.of(HttpStatus.FORBIDDEN, null);
        String url = imageStorageService.resolveProfileImagePublicUrl(id);
        return PerfilImagenResult.of(HttpStatus.FOUND, url);
    }

    // ================== ELIMINACIÓN IMAGEN PERRO ==================

    public EliminacionImagenPerroResult eliminarImagenPerro(String id, CustomUserDetails user) {
        if (!esUUID(id)) return EliminacionImagenPerroResult.of(HttpStatus.BAD_REQUEST, "ID inválido");
        var asociadaOpt = repositorioImagenPerro.findById(id);
        if (asociadaOpt.isPresent()) {
            ImagenPerro img = asociadaOpt.get();
            if (user == null) return EliminacionImagenPerroResult.of(HttpStatus.UNAUTHORIZED, "No autenticado");
            boolean esDueno = img.getPerro() != null && img.getPerro().getUsuario() != null && Objects.equals(img.getPerro().getUsuario().getId(), user.getUsuario().getId());
            boolean esPriv = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_REVIEWER"));
            if (!esDueno && !esPriv) return EliminacionImagenPerroResult.of(HttpStatus.FORBIDDEN, "No autorizado");
            try { repositorioImagenPerro.delete(img); } catch (Exception e) { log.warn("No se pudo borrar entidad {}: {}", id, e.getMessage()); }
        }
        try {
            if (imageStorageService.isCloudProvider()) {
                imageStorageService.deleteDogImage(id);
            } else {
                deleteLocalDogFiles(id);
            }
        } catch (Exception e) {
            log.warn("Error eliminando archivo imagen {}: {}", id, e.getMessage());
        }
        return EliminacionImagenPerroResult.of(HttpStatus.OK, "Imagen eliminada (o ya inexistente)");
    }

    // ================== HELPERS ==================

    private boolean esUUID(String v) { try { UUID.fromString(v); return true; } catch (Exception e) { return false; } }

    private String extraerId(String filename) {
        if (!StringUtils.hasText(filename)) return null;
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) return null;
        int dot = filename.lastIndexOf('.');
        String base = dot > 0 ? filename.substring(0, dot) : filename;
        return base.isBlank() ? null : base;
    }

    private boolean estaAutorizadoImagenPerfil(CustomUserDetails userDetails, ImagenPerfil imagen) {
        if (userDetails == null || imagen == null) return false;
        String ownerId = imagen.getPerfilUsuario().getUsuario().getId();
        if (Objects.equals(ownerId, userDetails.getUsuario().getId())) return true;
        return userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private record LocalFile(Path path, String contentType) {}
    private record ProxyResult(byte[] body, String mediaType, long length) {}

    private LocalFile findLocalDogFile(String id) {
        Path base = Paths.get(perrosDir);
        String[] exts = {"jpg", "jpeg", "png", "gif", "webp"};
        try {
            for (String ext : exts) {
                Path p = base.resolve(id + "." + ext);
                if (Files.exists(p)) {
                    String ct = MIME_TYPES.getOrDefault(ext, MediaType.IMAGE_JPEG_VALUE);
                    return new LocalFile(p, ct);
                }
            }
            if (Files.exists(base) && Files.isDirectory(base)) {
                try (var stream = Files.list(base)) {
                    Optional<Path> found = stream.filter(Files::isRegularFile).filter(p -> {
                        String fn = p.getFileName().toString();
                        return fn.startsWith(id + ".") || fn.equals(id);
                    }).findFirst();
                    if (found.isPresent()) {
                        Path p = found.get();
                        String fn = p.getFileName().toString();
                        int dot = fn.lastIndexOf('.');
                        String ext = dot > 0 ? fn.substring(dot + 1).toLowerCase() : "jpg";
                        String ct = MIME_TYPES.getOrDefault(ext, MediaType.IMAGE_JPEG_VALUE);
                        return new LocalFile(p, ct);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("findLocalDogFile fallo id={}: {}", id, ex.getMessage());
        }
        return null;
    }

    private ProxyResult fetchBinary(String url, boolean includeBody) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest.Builder rb = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", "perritoscutapp/1.0");
        if (includeBody) rb.GET(); else rb.method("HEAD", HttpRequest.BodyPublishers.noBody());
        HttpResponse<byte[]> resp = client.send(rb.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() >= 400) return null;
        String ct = resp.headers().firstValue("content-type").map(v -> v.split(";",2)[0]).orElse(null);
        long len = -1;
        try { var clOpt = resp.headers().firstValue("content-length"); if (clOpt.isPresent()) len = Long.parseLong(clOpt.get()); } catch (Exception ignored) {}
        byte[] body = includeBody ? resp.body() : new byte[0];
        return new ProxyResult(body, ct, len);
    }

    private void deleteLocalDogFiles(String id) {
        try {
            Path base = Paths.get(perrosDir);
            String[] exts = {"jpg", "jpeg", "png", "gif", "webp"};
            for (String ext : exts) {
                Path p = base.resolve(id + "." + ext);
                if (Files.exists(p)) {
                    try { Files.delete(p); } catch (Exception ex) { log.warn("No se pudo borrar archivo {}: {}", p, ex.getMessage()); }
                }
            }
        } catch (Exception ex) {
            log.debug("deleteLocalDogFiles fallo id={}: {}", id, ex.getMessage());
        }
    }

    // ================== RESULT RECORDS ==================

    public record ImagenPerroContenidoResult(HttpStatus status, byte[] body, String mediaType, long length) {
        public static ImagenPerroContenidoResult ok(byte[] b, String mt, long len) { return new ImagenPerroContenidoResult(HttpStatus.OK, b, mt, len); }
        public static ImagenPerroContenidoResult notFound() { return new ImagenPerroContenidoResult(HttpStatus.NOT_FOUND, null, null, -1); }
        public static ImagenPerroContenidoResult badGateway() { return new ImagenPerroContenidoResult(HttpStatus.BAD_GATEWAY, null, null, -1); }
    }
    public record ImagenPerroHeadResult(HttpStatus status, String mediaType, long length) {
        public static ImagenPerroHeadResult ok(String mt, long len) { return new ImagenPerroHeadResult(HttpStatus.OK, mt, len); }
        public static ImagenPerroHeadResult notFound() { return new ImagenPerroHeadResult(HttpStatus.NOT_FOUND, null, -1); }
        public static ImagenPerroHeadResult badGateway() { return new ImagenPerroHeadResult(HttpStatus.BAD_GATEWAY, null, -1); }
    }
    public record PerfilImagenResult(HttpStatus status, String redirectUrl) { public static PerfilImagenResult of(HttpStatus st, String url){ return new PerfilImagenResult(st, url);} }
    public record EliminacionImagenPerroResult(HttpStatus status, String mensaje) { public static EliminacionImagenPerroResult of(HttpStatus st, String m){ return new EliminacionImagenPerroResult(st,m);} }
}

