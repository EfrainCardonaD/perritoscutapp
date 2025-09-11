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
    private static final long MAX_DOG_SIZE = 15L * 1024 * 1024; // 15MB
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final Cloudinary cloudinary;
    private final String perrosFolder;
    private final String perfilesFolder;

    @Override
    public UploadResult uploadDogImage(MultipartFile file) throws Exception {
        validate(file, MAX_DOG_SIZE);
        String id = UUID.randomUUID().toString();
        // Usar public_id sin carpeta y establecer 'folder' explícitamente
        String publicId = id;
        Map<?, ?> res = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "public_id", publicId,
                "folder", perrosFolder,
                "overwrite", true,
                "resource_type", "image"
                // Sin transformación: conservar proporciones y resolución originales
        ));
        log.info("Cloudinary upload dog -> public_id={}, folder={}, url={}",
                res.get("public_id"), res.get("folder"), res.get("secure_url"));
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
        // Usar public_id sin carpeta y establecer 'folder' explícitamente
        String publicId = id;
        Map<?, ?> res = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "public_id", publicId,
                "folder", perfilesFolder,
                "overwrite", true,
                "resource_type", "image"
        ));
        log.info("Cloudinary upload profile -> public_id={}, folder={}, url={}",
                res.get("public_id"), res.get("folder"), res.get("secure_url"));
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
        // Sin forzar recorte ni resize; mantener proporciones. Opcional: optimizar formato/calidad en entrega.
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
}
