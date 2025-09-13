package com.cut.cardona.service.infra.storage.local;

import com.cut.cardona.service.infra.storage.ImageStorageService;
import com.cut.cardona.service.infra.storage.UploadResult;
import com.cut.cardona.modelo.perros.RepositorioImagenPerro;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

@Slf4j
@RequiredArgsConstructor
public class LocalImageStorageService implements ImageStorageService {

    private static final long MAX_PROFILE_SIZE = 15L * 1024 * 1024; // 15MB
    private static final long MAX_DOG_SIZE = 15L * 1024 * 1024; // Cambiado a 15MB
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final String perrosDir;
    private final String perfilesDir;

    @Override
    public UploadResult uploadDogImage(MultipartFile file) throws Exception {
        validate(file, MAX_DOG_SIZE);
        ensureDir(perrosDir);
        return saveDogToDir(file, perrosDir);
    }

    @Override
    public UploadResult uploadProfileImage(MultipartFile file) throws Exception {
        validate(file, MAX_PROFILE_SIZE);
        ensureDir(perfilesDir);
        return saveProfileToDir(file, perfilesDir);
    }

    private UploadResult saveDogToDir(MultipartFile file, String baseDir) throws IOException {
        // Conservar proporciones: guardar el archivo tal cual con su extensión original
        String original = file.getOriginalFilename();
        String ext = "." + getExtension(original);
        String id = UUID.randomUUID().toString();
        String filename = id + ext.toLowerCase();
        Path destino = Paths.get(baseDir).resolve(filename).normalize();
        Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        return UploadResult.builder()
                .id(id)
                .filename(filename)
                .url("/api/imagenes/perritos/" + id)
                .contentType(file.getContentType())
                .size(file.getSize())
                .build();
    }

    private UploadResult saveProfileToDir(MultipartFile file, String baseDir) throws IOException {
        // Procesar imagen: recortar al centro cuadrado y reducir a max 1024x1024
        BufferedImage img = readImageSafely(file.getInputStream());
        if (img == null) {
            // Si no se pudo leer como imagen, guardar el stream original como fallback
            String original = file.getOriginalFilename();
            String ext = "." + getExtension(original);
            String id = UUID.randomUUID().toString();
            String filename = id + ext.toLowerCase();
            Path destino = Paths.get(baseDir).resolve(filename).normalize();
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return UploadResult.builder()
                    .id(id)
                    .filename(filename)
                    .url("/api/imagenes/perfil/" + filename)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .build();
        }

        BufferedImage squared = cropCenterSquare(img);
        BufferedImage resized = resizeToMax(squared, 1024);

        // Convertir y comprimir a JPEG para compatibilidad y menor peso
        byte[] outBytes = writeJpegToBytes(resized, 0.85f);

        String id = UUID.randomUUID().toString();
        String filename = id + ".jpg";
        Path destino = Paths.get(baseDir).resolve(filename).normalize();
        Files.createDirectories(destino.getParent());
        Files.write(destino, outBytes);

        return UploadResult.builder()
                .id(id)
                .filename(filename)
                .url("/api/imagenes/perfil/" + filename)
                .contentType("image/jpeg")
                .size(outBytes.length)
                .build();
    }

    private BufferedImage readImageSafely(InputStream in) {
        try (InputStream is = in) {
            return ImageIO.read(is);
        } catch (IOException e) {
            log.warn("No se pudo leer imagen: {}", e.getMessage());
            return null;
        }
    }

    private BufferedImage cropCenterSquare(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int size = Math.min(w, h);
        int x = (w - size) / 2;
        int y = (h - size) / 2;
        return src.getSubimage(x, y, size, size);
    }

    private BufferedImage resizeToMax(BufferedImage src, int max) {
        int s = src.getWidth(); // src is square
        if (s <= max) return src;
        Image tmp = src.getScaledInstance(max, max, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(max, max, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }

    private byte[] writeJpegToBytes(BufferedImage img, float quality) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            if (!writers.hasNext()) {
                // Fallback to default write
                ImageIO.write(img, "jpg", baos);
                return baos.toByteArray();
            }
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(Math.max(0.01f, Math.min(1.0f, quality)));
            }
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(img, null, null), param);
                ios.flush();
            } finally {
                writer.dispose();
            }
            return baos.toByteArray();
        }
    }

    private void ensureDir(String dir) throws IOException {
        Path path = Paths.get(dir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    private void validate(MultipartFile file, long maxSize) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Archivo vacío");
        if (file.getSize() > maxSize) throw new IllegalArgumentException("El archivo excede el tamaño permitido");
        String ct = file.getContentType();
        if (ct == null || !ALLOWED.contains(ct.toLowerCase())) throw new IllegalArgumentException("Tipo de archivo no soportado");
    }

    private String getExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    @Override
    public boolean isCloudProvider() { return false; }

    @Override
    public String resolveDogImagePublicUrl(String id) { return "/api/imagenes/perritos/" + id; }

    @Override
    public String resolveProfileImagePublicUrl(String id) { return "/api/imagenes/perfil/" + id + ".jpg"; }

    @Override
    public void deleteProfileImage(String id) {
        // Intentar eliminar archivo de perfil basado en patrones comunes
        try {
            Path dir = Paths.get(perfilesDir);
            if (!Files.exists(dir)) return;
            String[] exts = {"jpg", "jpeg", "png", "gif", "webp"};
            for (String ext : exts) {
                Path candidate = dir.resolve(id + "." + ext);
                if (Files.exists(candidate)) {
                    try { Files.delete(candidate); } catch (Exception ex) { log.warn("No se pudo borrar archivo {}: {}", candidate, ex.getMessage()); }
                }
            }
        } catch (Exception e) {
            log.debug("deleteProfileImage local ignorado: {}", e.getMessage());
        }
    }

    @Override
    public int cleanupOrphanImages(RepositorioImagenPerro repositorioImagenPerro) {

        return 0;
    }
}
