package com.cut.cardona.infra.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    UploadResult uploadDogImage(MultipartFile file) throws Exception;

    UploadResult uploadProfileImage(MultipartFile file) throws Exception;

    boolean isCloudProvider();

    String resolveDogImagePublicUrl(String id);

    String resolveProfileImagePublicUrl(String id);

    // Nuevo: borrar imagen de perro por id (public_id)
    default void deleteDogImage(String id) {
        // no-op por defecto
    }

    // Nuevo: borrar imagen de perfil por id (public_id)
    default void deleteProfileImage(String id) {
        // no-op por defecto
    }
}
