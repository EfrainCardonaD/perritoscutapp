package com.cut.cardona.infra.storage.local;

import com.cut.cardona.infra.storage.ImageStorageService;
import com.cut.cardona.infra.storage.UploadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class LocalImageStorageService implements ImageStorageService {

    private static final long MAX_PROFILE_SIZE = 15L * 1024 * 1024; // 15MB
    private static final long MAX_DOG_SIZE = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final String perrosDir;
    private final String perfilesDir;

    @Override
    public UploadResult uploadDogImage(MultipartFile file) throws Exception {
        validate(file, MAX_DOG_SIZE);
        ensureDir(perrosDir);
        return saveToDir(file, perrosDir);
    }

    @Override
    public UploadResult uploadProfileImage(MultipartFile file) throws Exception {
        validate(file, MAX_PROFILE_SIZE);
        ensureDir(perfilesDir);
        return saveToDir(file, perfilesDir);
    }

    private UploadResult saveToDir(MultipartFile file, String baseDir) throws IOException {
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
}
