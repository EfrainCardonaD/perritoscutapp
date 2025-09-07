package com.cut.cardona.infra.storage;

import com.cloudinary.Cloudinary;
import com.cut.cardona.infra.storage.cloudinary.CloudinaryImageStorageService;
import com.cut.cardona.infra.storage.local.LocalImageStorageService;
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

    @Value("${cloudinary.perros_folder:perritos}")
    private String perrosFolder;
    @Value("${cloudinary.perfiles_folder:perfiles}")
    private String perfilesFolder;
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
                    log.info("ImageStorageService: usando Cloudinary via CLOUDINARY_URL (folders perros='{}', perfiles='{}')", perrosFolder, perfilesFolder);
                } catch (Exception ex) {
                    log.error("CLOUDINARY_URL inválida: '{}'", raw);
                    throw ex;
                }
            } else {
                cloud = new Cloudinary();
                log.warn("ImageStorageService: CLOUDINARY_URL no definida; se intentará leer del entorno. Folders perros='{}', perfiles='{}'", perrosFolder, perfilesFolder);
            }
            return new CloudinaryImageStorageService(cloud, perrosFolder, perfilesFolder);
        }
        log.info("ImageStorageService: usando almacenamiento local en {} y {}", perrosDir, perfilesDir);
        return new LocalImageStorageService(perrosDir, perfilesDir);
    }
}
