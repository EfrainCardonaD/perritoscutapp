package com.cut.cardona.infra.storage.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.cut.cardona.infra.storage.ImageStorageService;
import com.cut.cardona.infra.storage.UploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

@Slf4j
@RequiredArgsConstructor
public class CloudinaryImageStorageService implements ImageStorageService {

    // Permitir entrada de hasta 15MB
    private static final long MAX_PROFILE_SIZE = 15L * 1024 * 1024; // 15MB
    private static final long MAX_DOG_SIZE = 15L * 1024 * 1024; // 15MB
    // Objetivo para subir a Cloudinary (plan 10MB); dejamos margen a 9MB
    private static final long TARGET_UPLOAD_BYTES = 9L * 1024 * 1024; // 9MB
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final Cloudinary cloudinary;
    private final String perrosFolder;
    private final String perfilesFolder;

    @Override
    public UploadResult uploadDogImage(MultipartFile file) throws Exception {
        validate(file, MAX_DOG_SIZE);
        String id = UUID.randomUUID().toString();
        byte[] payload = prepareForUpload(file, TARGET_UPLOAD_BYTES);
        Map<?, ?> res = cloudinary.uploader().upload(payload, ObjectUtils.asMap(
                "public_id", id,
                "folder", perrosFolder,
                "asset_folder", perrosFolder,
                "overwrite", true,
                "resource_type", "image"
        ));
        log.info("Cloudinary upload dog -> public_id={}, folder={}, asset_folder={}, url={}",
                res.get("public_id"), res.get("folder"), res.get("asset_folder"), res.get("secure_url"));
        String secureUrl = (String) res.get("secure_url");
        String format = (String) res.get("format");
        String filename = id + (StringUtils.hasText(format) ? "." + format : "");
        return UploadResult.builder()
                .id(id)
                .filename(filename)
                .url(secureUrl)
                .contentType(file.getContentType())
                .size((long) payload.length)
                .build();
    }

    @Override
    public UploadResult uploadProfileImage(MultipartFile file) throws Exception {
        validate(file, MAX_PROFILE_SIZE);
        String id = UUID.randomUUID().toString();
        byte[] payload = prepareForUpload(file, TARGET_UPLOAD_BYTES);
        Map<?, ?> res = cloudinary.uploader().upload(payload, ObjectUtils.asMap(
                "public_id", id,
                "folder", perfilesFolder,
                "asset_folder", perfilesFolder,
                "overwrite", true,
                "resource_type", "image"
        ));
        log.info("Cloudinary upload profile -> public_id={}, folder={}, asset_folder={}, url={}",
                res.get("public_id"), res.get("folder"), res.get("asset_folder"), res.get("secure_url"));
        String secureUrl = (String) res.get("secure_url");
        String format = (String) res.get("format");
        String filename = id + (StringUtils.hasText(format) ? "." + format : "");
        return UploadResult.builder()
                .id(id)
                .filename(filename)
                .url(secureUrl)
                .contentType(file.getContentType())
                .size((long) payload.length)
                .build();
    }

    private void validate(MultipartFile file, long maxSize) throws IllegalArgumentException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Archivo vacío");
        if (file.getSize() > maxSize) throw new IllegalArgumentException("El archivo excede el tamaño permitido (máximo 15MB)");
        String ct = file.getContentType();
        if (ct == null || !ALLOWED.contains(ct.toLowerCase())) throw new IllegalArgumentException("Tipo de archivo no soportado");
    }

    private byte[] prepareForUpload(MultipartFile file, long targetBytes) throws Exception {
        long size = file.getSize();
        String ct = safeLower(file.getContentType());
        if (size <= targetBytes) {
            return file.getBytes();
        }
        // GIF animado no se reencapsula fácilmente sin perder animación; forzamos cambio o rechazo
        if ("image/gif".equals(ct)) {
            throw new IllegalArgumentException("GIF demasiado grande (>9MB). Usa JPG/PNG o reduce el tamaño.");
        }
        // Reescalar/recomprimir a JPEG hasta aproximar target
        byte[] original = file.getBytes();
        try (InputStream in = new ByteArrayInputStream(original)) {
            BufferedImage src = ImageIO.read(in);
            if (src == null) {
                // Si no podemos leer, devolver original (puede fallar en Cloudinary si >10MB)
                return original;
            }
            // Si tiene alpha, pintar sobre fondo blanco
            if (src.getColorModel().hasAlpha()) {
                BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = rgb.createGraphics();
                g2.setComposite(AlphaComposite.SrcOver);
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
                g2.drawImage(src, 0, 0, null);
                g2.dispose();
                src = rgb;
            }
            double scale = 1.0;
            float quality = 0.85f;
            int iterations = 0;
            byte[] best = null;
            while (iterations < 12) {
                iterations++;
                BufferedImage scaled = scaleImage(src, scale);
                byte[] jpg = writeJpeg(scaled, quality);
                if (best == null || jpg.length < best.length) best = jpg;
                if (jpg.length <= targetBytes) {
                    log.debug("Preprocesado OK: {} bytes (q={} scale={})", jpg.length, quality, scale);
                    return jpg;
                }
                // Reducir calidad hasta 0.5, luego empezar a reducir escala
                if (quality > 0.5f) {
                    quality -= 0.1f;
                } else {
                    scale *= 0.85; // 15% menos cada paso
                    // Evitar reducir demasiado
                    if (scaled.getWidth() < 800 || scaled.getHeight() < 800) {
                        // Nos quedamos con lo mejor logrado
                        break;
                    }
                }
            }
            log.debug("Preprocesado límite alcanzado. Mejor tamaño: {} bytes", best != null ? best.length : -1);
            return best != null ? best : original;
        }
    }

    private static BufferedImage scaleImage(BufferedImage src, double scale) {
        if (scale >= 0.999) return src;
        int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private static byte[] writeJpeg(BufferedImage img, float quality) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 1024);
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(Math.max(0.1f, Math.min(quality, 1.0f)));
            param.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
        }
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    private static String safeLower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean isCloudProvider() { return true; }

    @Override
    public String resolveDogImagePublicUrl(String id) {
        String publicId = perrosFolder + "/" + id;
        return cloudinary.url()
                .secure(true)
                .transformation(new Transformation()
                        .quality("auto")
                        .fetchFormat("auto"))
                .generate(publicId);
    }

    @Override
    public String resolveProfileImagePublicUrl(String id) {
        String publicId = perfilesFolder + "/" + id;
        return cloudinary.url()
                .secure(true)
                .transformation(new Transformation()
                        .quality("auto")
                        .fetchFormat("auto"))
                .generate(publicId);
    }

    @Override
    public void deleteDogImage(String id) {
        try {
            String publicId = perrosFolder + "/" + id;
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (Exception ex) {
            log.warn("No se pudo eliminar imagen en Cloudinary id={}: {}", id, ex.getMessage());
        }
    }

    @Override
    public void deleteProfileImage(String id) {
        try {
            String publicId = perfilesFolder + "/" + id;
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (Exception ex) {
            log.warn("No se pudo eliminar imagen de perfil en Cloudinary id={}: {}", id, ex.getMessage());
        }
    }
}
