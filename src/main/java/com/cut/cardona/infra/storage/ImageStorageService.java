package com.cut.cardona.infra.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    UploadResult uploadDogImage(MultipartFile file) throws Exception;

    UploadResult uploadProfileImage(MultipartFile file) throws Exception;

    boolean isCloudProvider();

    String resolveDogImagePublicUrl(String id);

    String resolveProfileImagePublicUrl(String id);
}
