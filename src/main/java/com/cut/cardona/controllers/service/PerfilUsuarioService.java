package com.cut.cardona.controllers.service;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PerfilUsuarioService {

    private final RepositorioUsuario repositorioUsuario;
    private final RepositorioPerfilUsuario repositorioPerfilUsuario;
    private final RepositorioImagenPerfil repositorioImagenPerfil;
    private final PasswordEncoder passwordEncoder;

    private static final String UPLOAD_DIR = "uploads/perfiles/";
    private static final long MAX_FILE_SIZE = 5_242_880L; // 5MB

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
     * Actualiza la imagen de perfil de un usuario
     */
    public String actualizarImagenPerfil(String usuarioId, MultipartFile archivo) {
        PerfilUsuario perfil = repositorioPerfilUsuario.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Perfil no encontrado"));

        // Desactivar imagen anterior si existe
        repositorioImagenPerfil.findActivaByUsuarioId(usuarioId)
                .ifPresent(imagenAnterior -> {
                    imagenAnterior.setActiva(false);
                    repositorioImagenPerfil.save(imagenAnterior);
                });

        ImagenPerfil nuevaImagen = procesarImagenPerfil(archivo, perfil);
        return nuevaImagen.getUrlPublica();
    }

    /**
     * Procesa y guarda una imagen de perfil
     */
    @Transactional // ✅ CRÍTICO: Agregar @Transactional para rollback automático
    protected ImagenPerfil procesarImagenPerfil(MultipartFile archivo, PerfilUsuario perfil) {
        try {
            // Validaciones
            validarArchivo(archivo);

            // Crear directorio si no existe
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generar nombre único para el archivo
            String extension = obtenerExtension(archivo.getOriginalFilename());
            String nombreArchivo = UUID.randomUUID() + "." + extension;
            Path rutaArchivo = uploadPath.resolve(nombreArchivo);

            // Guardar archivo
            Files.copy(archivo.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);

            // Crear entidad ImagenPerfil
            ImagenPerfil imagenPerfil = new ImagenPerfil();
            imagenPerfil.setPerfilUsuario(perfil); // ✅ CORRECCIÓN: usar la relación JPA en lugar del ID
            imagenPerfil.setNombreArchivo(nombreArchivo);
            imagenPerfil.setRutaArchivo(rutaArchivo.toString());
            imagenPerfil.setTipoMime(archivo.getContentType());
            imagenPerfil.setTamanoBytes(archivo.getSize());
            imagenPerfil.setUrlPublica("/api/imagenes/perfil/" + nombreArchivo);

            return repositorioImagenPerfil.save(imagenPerfil);

        } catch (IOException e) {
            throw new RuntimeException("Error al procesar la imagen de perfil", e);
        }
    }

    /**
     * Construye el DTO de perfil completo
     */
    private DtoPerfilCompleto construirDtoPerfilCompleto(Usuario usuario, PerfilUsuario perfil, ImagenPerfil imagen) {
        return new DtoPerfilCompleto(
            usuario.getId(),
            usuario.getUsername(), // Corregido: era getUserName()
            usuario.getEmail(),
            usuario.getRol(),
            usuario.getActivo(),
            usuario.getFechaCreacion(),
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
     * Valida el archivo de imagen
     */
    private void validarArchivo(MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }

        if (archivo.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo no puede exceder 5MB");
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
     * Obtiene la extensión del archivo
     */
    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return "jpg";
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Verifica si un teléfono ya está registrado
     */
    public boolean existeTelefono(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) {
            return false;
        }
        // Limpiar el teléfono antes de buscar
        String telefonoLimpio = telefono.replaceAll("[\\s()\\-]", "");
        return repositorioPerfilUsuario.existsByTelefono(telefonoLimpio);
    }
}
