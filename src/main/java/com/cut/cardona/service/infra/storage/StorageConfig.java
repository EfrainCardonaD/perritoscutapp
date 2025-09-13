package com.cut.cardona.service.infra.storage;

import com.cloudinary.Cloudinary;
import com.cut.cardona.service.infra.storage.cloudinary.CloudinaryImageStorageService;
import com.cut.cardona.service.infra.storage.local.LocalImageStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@Slf4j
public class StorageConfig {

    @Value("${app.storage.provider:local}")
    private String provider;

    @Value("${app.storage.perros-dir:uploads/perritos/}")
    private String perrosDir;

    @Value("${app.storage.perfiles-dir:uploads/perfiles/}")
    private String perfilesDir;

    @Value("${app.storage.documentos-dir:uploads/documentos/}")
    private String documentosDir;

    @Value("${cloudinary.perros_folder:perritos}")
    private String perrosFolder;
    @Value("${cloudinary.perfiles_folder:perfiles}")
    private String perfilesFolder;
    @Value("${cloudinary.documentos_folder:documentos}")
    private String documentosFolder;
    @Value("${cloudinary.url:${CLOUDINARY_URL:}}")
    private String cloudinaryUrl;

    @Bean
    public ImageStorageService imageStorageService() {
        if ("cloudinary".equalsIgnoreCase(provider)) {
            Cloudinary cloud;
            String raw = cloudinaryUrl;
            if (StringUtils.hasText(raw)) {
                try {
                    cloud = new Cloudinary(raw.trim());
                    log.info("ImageStorageService: usando Cloudinary (perros='{}', perfiles='{}', documentos='{}')", perrosFolder, perfilesFolder, documentosFolder);
                } catch (Exception ex) {
                    log.error("CLOUDINARY_URL inválida: '{}'", raw);
                    throw ex;
                }
            } else {
                cloud = new Cloudinary();
                log.warn("ImageStorageService: CLOUDINARY_URL no definida; folders perros='{}', perfiles='{}', documentos='{}'", perrosFolder, perfilesFolder, documentosFolder);
            }
            return new CloudinaryImageStorageService(cloud, perrosFolder, perfilesFolder, documentosFolder);
        }
        log.info("ImageStorageService: usando almacenamiento local en perrosDir='{}', perfilesDir='{}', documentosDir='{}'", perrosDir, perfilesDir, documentosDir);
        return new LocalImageStorageService(perrosDir, perfilesDir, documentosDir);
    }
}
