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

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class CloudinaryImageStorageService implements ImageStorageService {

    private static final long MAX_PROFILE_SIZE = 15L * 1024 * 1024; // 15MB
    private static final long MAX_DOG_SIZE = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final Cloudinary cloudinary;
    private final String perrosFolder;
    private final String perfilesFolder;

    @Override
    public UploadResult uploadDogImage(MultipartFile file) throws Exception {
        validate(file, MAX_DOG_SIZE);
        String id = UUID.randomUUID().toString();
        String publicId = perrosFolder + "/" + id;
        Map<?, ?> res = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "public_id", publicId,
                "overwrite", true,
                "resource_type", "image",
                // recorte cuadrado
                "transformation", new Transformation().gravity("auto").crop("fill").aspectRatio("1:1")
        ));
        String secureUrl = (String) res.get("secure_url");
        String format = (String) res.get("format");
        String filename = id + (StringUtils.hasText(format) ? "." + format : "");
        return UploadResult.builder()
                .id(id)
                .filename(filename)
                .url(secureUrl)
                .contentType(file.getContentType())
                .size(file.getSize())
                .build();
    }

    @Override
    public UploadResult uploadProfileImage(MultipartFile file) throws Exception {
        validate(file, MAX_PROFILE_SIZE);
        String id = UUID.randomUUID().toString();
        String publicId = perfilesFolder + "/" + id;
        Map<?, ?> res = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "public_id", publicId,
                "overwrite", true,
                "resource_type", "image",
                "transformation", new Transformation().gravity("auto").crop("fill").aspectRatio("1:1")
        ));
        String secureUrl = (String) res.get("secure_url");
        String format = (String) res.get("format");
        String filename = id + (StringUtils.hasText(format) ? "." + format : "");
        return UploadResult.builder()
                .id(id)
                .filename(filename)
                .url(secureUrl)
                .contentType(file.getContentType())
                .size(file.getSize())
                .build();
    }

    private void validate(MultipartFile file, long maxSize) throws IllegalArgumentException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Archivo vacío");
        if (file.getSize() > maxSize) throw new IllegalArgumentException("El archivo excede el tamaño permitido");
        String ct = file.getContentType();
        if (ct == null || !ALLOWED.contains(ct.toLowerCase())) throw new IllegalArgumentException("Tipo de archivo no soportado");
    }

    @Override
    public boolean isCloudProvider() { return true; }

    @Override
    public String resolveDogImagePublicUrl(String id) {
        String publicId = perrosFolder + "/" + id;
        // Mantener formato cuadrado
        return cloudinary.url()
                .secure(true)
                .transformation(new Transformation().gravity("auto").crop("fill").aspectRatio("1:1"))
                .generate(publicId);
    }

    @Override
    public String resolveProfileImagePublicUrl(String id) {
        String publicId = perfilesFolder + "/" + id;
        return cloudinary.url()
                .secure(true)
                .transformation(new Transformation().gravity("auto").crop("fill").aspectRatio("1:1"))
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
}
