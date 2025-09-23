package com.cut.cardona.service.perfil;

import com.cut.cardona.service.infra.storage.ImageStorageService;
import com.cut.cardona.service.infra.storage.UploadResult;
import com.cut.cardona.modelo.dto.perfil.DtoActualizarPerfilRequest;
import com.cut.cardona.modelo.dto.perfil.DtoPerfilCompleto;
import com.cut.cardona.modelo.dto.registro.DtoRegistroCompletoRequest;
import com.cut.cardona.modelo.imagenes.ImagenPerfil;
import com.cut.cardona.modelo.imagenes.RepositorioImagenPerfil;
import com.cut.cardona.modelo.perfil.PerfilUsuario;
import com.cut.cardona.modelo.perfil.RepositorioPerfilUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import com.cut.cardona.modelo.usuarios.Roles;
import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PerfilUsuarioService {

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioPerfilUsuario repositorioPerfilUsuario;
    private final RepositorioImagenPerfil repositorioImagenPerfil;
    private final PasswordEncoder passwordEncoder;
    private final ImageStorageService imageStorageService;

    private static final long MAX_FILE_SIZE = 15L * 1024 * 1024; // 15MB

    /**
     * Registra un usuario con información extendida de perfil usando el DTO unificado
     */
    public DtoPerfilCompleto registrarUsuarioExtendido(DtoRegistroCompletoRequest request) {
        return registrarUsuarioExtendido(request, null);
    }

    /**
     * Variante que permite imagen de perfil
     */
    public DtoPerfilCompleto registrarUsuarioExtendido(DtoRegistroCompletoRequest request, MultipartFile fotoPerfil) {
        // Validar mayoría de edad (ya validado por anotación, doble chequeo por seguridad)
        if (request.fechaNacimiento() != null && request.fechaNacimiento().isAfter(java.time.LocalDate.now().minusYears(15))) {
            throw new IllegalArgumentException("Debe ser mayor de 15 años");
        }

        // Validar teléfono único si se proporciona
        if (request.telefono() != null && repositorioPerfilUsuario.existsByTelefono(request.telefono())) {
            throw new IllegalArgumentException("El teléfono ya está registrado");
        }

        // Crear usuario básico directamente desde el request
        Usuario usuario = new Usuario();
        usuario.setUserName(request.userName());
        usuario.setEmail(request.email() != null ? request.email().trim().toLowerCase() : null);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setActivo(false); // Inactivo hasta verificar email
        usuario.setEmailVerificado(false);
        usuario.setRol(Roles.ROLE_USER);
        usuario.setFechaCreacion(new java.sql.Timestamp(System.currentTimeMillis()));
        usuario = repositorioUsuario.save(usuario);

        // Crear perfil extendido
        com.cut.cardona.modelo.perfil.PerfilUsuario perfil = new com.cut.cardona.modelo.perfil.PerfilUsuario();
        perfil.setUsuario(usuario);
        perfil.setNombreReal(request.nombreReal());
        perfil.setTelefono(request.telefono());
        // Usar idioma y zona horaria del request si vienen, con valores por defecto
        String idioma = (request.idioma() != null && !request.idioma().isBlank()) ? request.idioma() : "es";
        String zonaHoraria = (request.zonaHoraria() != null && !request.zonaHoraria().isBlank()) ? request.zonaHoraria() : "America/Mexico_City";
        perfil.setIdioma(idioma);
        perfil.setZonaHoraria(zonaHoraria);
        perfil.setFechaNacimiento(request.fechaNacimiento());
        perfil = repositorioPerfilUsuario.save(perfil);

        // Procesar imagen de perfil si se proporciona
        com.cut.cardona.modelo.imagenes.ImagenPerfil imagenPerfil = null;
        if (fotoPerfil != null && !fotoPerfil.isEmpty()) {
            imagenPerfil = procesarImagenPerfil(fotoPerfil, perfil);
        }

        return construirDtoPerfilCompleto(usuario, perfil, imagenPerfil);
    }

    /**
     * Obtiene el perfil completo de un usuario por ID
     */
    @Transactional(readOnly = true)
    public Optional<DtoPerfilCompleto> obtenerPerfilCompleto(String usuarioId) {
        return repositorioPerfilUsuario.findByUsuarioIdWithUsuario(usuarioId)
                .map(perfil -> {
                    ImagenPerfil imagen = repositorioImagenPerfil.findActivaByUsuarioId(usuarioId).orElse(null);
                    return construirDtoPerfilCompleto(perfil.getUsuario(), perfil, imagen);
                });
    }

    /**
     * Actualiza los campos básicos del perfil (sin imagen)
     */
    public DtoPerfilCompleto actualizarPerfilCampos(String usuarioId, DtoActualizarPerfilRequest req) {
        PerfilUsuario perfil = repositorioPerfilUsuario.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil no encontrado"));
        // Validaciones específicas
        if (req.telefono() != null && !req.telefono().isBlank()) {
            String telLimpio = req.telefono().replaceAll("[\\s()\\-]", "");
            if (!telLimpio.equals(perfil.getTelefono()) && repositorioPerfilUsuario.existsByTelefono(telLimpio)) {
                throw new IllegalArgumentException("El teléfono ya está registrado");
            }
            perfil.setTelefono(telLimpio);
        }
        if (req.nombreReal() != null) perfil.setNombreReal(req.nombreReal());
        if (req.idioma() != null) perfil.setIdioma(req.idioma());
        if (req.zonaHoraria() != null) perfil.setZonaHoraria(req.zonaHoraria());
        if (req.fechaNacimiento() != null) {
            if (req.fechaNacimiento().isAfter(java.time.LocalDate.now().minusYears(15))) {
                throw new IllegalArgumentException("Debe ser mayor de 15 años");
            }
            perfil.setFechaNacimiento(req.fechaNacimiento());
        }
        perfil = repositorioPerfilUsuario.save(perfil);
        ImagenPerfil imagen = repositorioImagenPerfil.findActivaByUsuarioId(usuarioId).orElse(null);
        return construirDtoPerfilCompleto(perfil.getUsuario(), perfil, imagen);
    }

    /**
     * Actualiza la imagen de perfil (elimina la anterior en el storage si aplica)
     */
    public String actualizarImagenPerfil(String usuarioId, MultipartFile archivo) {
        PerfilUsuario perfil = repositorioPerfilUsuario.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil no encontrado"));

        // Desactivar imagen anterior y capturar para borrado
        final String[] oldPublicIdHolder = {null};
        repositorioImagenPerfil.findActivaByUsuarioId(usuarioId)
                .ifPresent(imagenAnterior -> {
                    imagenAnterior.setActiva(false);
                    repositorioImagenPerfil.save(imagenAnterior);
                    // Derivar public_id de Cloudinary: nombreArchivo sin la extensión
                    String nombreArchivo = imagenAnterior.getNombreArchivo();
                    if (nombreArchivo != null) {
                        int dot = nombreArchivo.lastIndexOf('.');
                        if (dot > 0) {
                            oldPublicIdHolder[0] = nombreArchivo.substring(0, dot);
                        } else {
                            oldPublicIdHolder[0] = nombreArchivo; // fallback
                        }
                    }
                });

        ImagenPerfil nuevaImagen = procesarImagenPerfil(archivo, perfil);

        // Borrado best-effort de imagen anterior en storage
        if (oldPublicIdHolder[0] != null) {
            try { imageStorageService.deleteProfileImage(oldPublicIdHolder[0]); } catch (Exception ignored) {}
        }
        return nuevaImagen.getUrlPublica();
    }

    /**
     * Procesa y guarda una imagen de perfil con Cloudinary
     */
    @Transactional
    protected ImagenPerfil procesarImagenPerfil(MultipartFile archivo, PerfilUsuario perfil) {
        // Validaciones básicas; límites estrictos se validan en el storage service
        validarArchivo(archivo);
        try {
            UploadResult up = imageStorageService.uploadProfileImage(archivo);
            ImagenPerfil imagenPerfil = new ImagenPerfil();
            imagenPerfil.setPerfilUsuario(perfil);
            imagenPerfil.setNombreArchivo(up.getFilename());
            // Usaremos la URL como ruta para cumplir no-null en esquema existente
            imagenPerfil.setRutaArchivo(up.getUrl());
            imagenPerfil.setTipoMime(up.getContentType());
            imagenPerfil.setTamanoBytes(up.getSize());
            imagenPerfil.setUrlPublica(up.getUrl());
            imagenPerfil.setActiva(true);
            return repositorioImagenPerfil.save(imagenPerfil);
        } catch (Exception e) {
            throw new RuntimeException("Error al subir la imagen de perfil", e);
        }
    }

    /**
     * Construye el DTO de perfil completo
     */
    private DtoPerfilCompleto construirDtoPerfilCompleto(Usuario usuario, PerfilUsuario perfil, ImagenPerfil imagen) {
        return new DtoPerfilCompleto(
            usuario.getId(),
            usuario.getUsername(),
            usuario.getEmail(),
            usuario.getEmailVerificado(),
            usuario.getRol(),
            usuario.getActivo(),
            usuario.getFechaCreacion(),
            usuario.getUltimoAcceso(),
            perfil.getId(),
            perfil.getNombreReal(),
            perfil.getTelefono(),
            perfil.getIdioma(),
            perfil.getZonaHoraria(),
            perfil.getFechaNacimiento(),
            perfil.esMayorDeEdad(),
            imagen != null ? imagen.getId() : null,
            imagen != null ? imagen.getUrlPublica() : null,
            imagen != null ? imagen.getNombreArchivo() : null,
            imagen != null ? imagen.getTipoMime() : null,
            imagen != null ? imagen.getTamanoBytes() : null,
            perfil.getFechaCreacion(),
            perfil.getFechaActualizacion()
        );
    }

    /**
     * Valida el archivo de imagen (15MB, tipos comunes)
     */
    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        if (archivo.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo no puede exceder 15MB");
        }
        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }
        if (!contentType.equals("image/jpeg") &&
            !contentType.equals("image/png") &&
            !contentType.equals("image/webp") &&
            !contentType.equals("image/gif")) {
            throw new IllegalArgumentException("Formato de imagen no soportado. Use JPEG, PNG, WebP o GIF");
        }
    }

    /**
     * Verifica si un teléfono ya está registrado en la base de datos
     */
    public boolean existeTelefono(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) {
            return false;
        }
        String telefonoLimpio = telefono.replaceAll("[\\s()\\-]", "");
        return repositorioPerfilUsuario.existsByTelefono(telefonoLimpio);
    }
}
