package com.cut.cardona.service.infra.storage;

import com.cut.cardona.modelo.perros.RepositorioImagenPerro;
import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    UploadResult uploadDogImage(MultipartFile file) throws Exception;

    UploadResult uploadProfileImage(MultipartFile file) throws Exception;

    // Nuevo: documentos (identificaciones / soportes de adopción)
    default UploadResult uploadDocumentImage(MultipartFile file) throws Exception { throw new UnsupportedOperationException("No implementado"); }

    boolean isCloudProvider();

    String resolveDogImagePublicUrl(String id);

    String resolveProfileImagePublicUrl(String id);

    default String resolveDocumentImagePublicUrl(String id) { return null; }

    // Borrar imagen de perro
    default void deleteDogImage(String id) { }

    // Borrar imagen de perfil
    default void deleteProfileImage(String id) { }

    // Borrar documento
    default void deleteDocumentImage(String id) { }

    int cleanupOrphanImages(RepositorioImagenPerro repositorioImagenPerro);

}
