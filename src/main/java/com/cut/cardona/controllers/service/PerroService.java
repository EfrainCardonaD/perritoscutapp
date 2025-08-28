package com.cut.cardona.controllers.service;

import com.cut.cardona.modelo.dto.perros.CrearPerroRequest;
import com.cut.cardona.modelo.dto.perros.DtoPerro;
import com.cut.cardona.modelo.perros.ImagenPerro;
import com.cut.cardona.modelo.perros.Perro;
import com.cut.cardona.modelo.perros.RepositorioImagenPerro;
import com.cut.cardona.modelo.perros.RepositorioPerro;
import com.cut.cardona.modelo.perros.enums.PerroEstadoAdopcion;
import com.cut.cardona.modelo.perros.enums.PerroEstadoRevision;
import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import com.cut.cardona.errores.UnprocessableEntityException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PerroService {

    @Value("${app.storage.perros-dir:uploads/perritos}")
    private String perrosUploadDir;

    private final RepositorioPerro repositorioPerro;
    private final RepositorioImagenPerro repositorioImagenPerro;
    private final RepositorioUsuario repositorioUsuario;

    @Transactional(readOnly = true)
    public List<DtoPerro> catalogoPublico() {
        return repositorioPerro.findCatalogoPublico().stream().map(DtoPerro::new).toList();
    }

    @Transactional(readOnly = true)
    public List<DtoPerro> catalogoPublico(String sexo, String tamano, String ubicacion, Integer page, Integer size) {
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size <= 0 || size > 100 ? 20 : size;
        Pageable pageable = PageRequest.of(p, s);
        return repositorioPerro.findCatalogoPublicoFiltrado(sexo, tamano, ubicacion, pageable)
                .map(DtoPerro::new)
                .getContent();
    }

    @Transactional(readOnly = true)
    public List<DtoPerro> perrosDelUsuarioActual() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Usuario> usuarioOpt = repositorioUsuario.findByUserName(username);
        if (usuarioOpt.isEmpty()) return List.of();
        return repositorioPerro.findByUsuarioId(usuarioOpt.get().getId()).stream().map(DtoPerro::new).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    @Transactional(readOnly = true)
    public List<DtoPerro> pendientesRevision() {
        return repositorioPerro.findPendientesRevision().stream().map(DtoPerro::new).toList();
    }

    @PreAuthorize("hasRole('USER')")
    public DtoPerro crearPerro(CrearPerroRequest req) {
        if (req.imagenes() == null || req.imagenes().isEmpty()) {
            throw new UnprocessableEntityException("Debe incluir al menos una imagen");
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = repositorioUsuario.findByUserName(username).orElseThrow();

        Perro perro = Perro.builder()
                .id(UUID.randomUUID().toString())
                .nombre(req.nombre())
                .edad(req.edad())
                .sexo(req.sexo())
                .tamano(req.tamano())
                .raza(req.raza())
                .descripcion(req.descripcion())
                .ubicacion(req.ubicacion())
                .fechaPublicacion(new Timestamp(System.currentTimeMillis()))
                .estadoAdopcion(PerroEstadoAdopcion.PENDIENTE)
                .estadoRevision(PerroEstadoRevision.PENDIENTE)
                .usuario(usuario)

                .build();

        repositorioPerro.save(perro);

        boolean principalYaAsignada = false;
        for (var imgReq : req.imagenes()) {
            boolean principal = Boolean.TRUE.equals(imgReq.principal());
            if (principal && principalYaAsignada) {
                principal = false; // evitar múltiples principales
            }
            principalYaAsignada = principalYaAsignada || principal;

            // Validar existencia de archivo local con el id dado (cualquier extensión)
            String imagenId = imgReq.id();
            if (!existeArchivoLocalPorId(imagenId)) {
                throw new UnprocessableEntityException("La imagen con id=" + imagenId + " no existe en el almacenamiento local");
            }

            ImagenPerro img = ImagenPerro.builder()
                    .id(imagenId)
                    .perro(perro)
                    .url(imagenId) // ya no guardamos URL, se servirá por id
                    .descripcion(imgReq.descripcion())
                    .principal(principal)
                    .fechaSubida(new Timestamp(System.currentTimeMillis()))
                    .build();
            repositorioImagenPerro.save(img);
        }
        return new DtoPerro(perro);
    }

    private boolean existeArchivoLocalPorId(String id) {
        try {
            Path dir = Paths.get(perrosUploadDir);
            if (!Files.exists(dir)) return false;
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, id + ".*")) {
                for (Path p : stream) {
                    if (Files.isRegularFile(p) && Files.isReadable(p)) return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public DtoPerro aprobarPerro(String perroId) {
        Perro perro = repositorioPerro.findById(perroId).orElseThrow();
        // Validación: debe tener una imagen principal
        long principales = repositorioImagenPerro.findByPerro_Id(perroId).stream().filter(i -> Boolean.TRUE.equals(i.getPrincipal())).count();
        if (principales == 0) {
            throw new UnprocessableEntityException("No se puede aprobar un perro sin imagen principal");
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario moderador = repositorioUsuario.findByUserName(username).orElseThrow();
        perro.setEstadoRevision(PerroEstadoRevision.APROBADO);
        perro.setRevisadoPor(moderador);
        perro.setFechaRevision(new Timestamp(System.currentTimeMillis()));
        return new DtoPerro(repositorioPerro.save(perro));
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public DtoPerro rechazarPerro(String perroId) {
        Perro perro = repositorioPerro.findById(perroId).orElseThrow();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario moderador = repositorioUsuario.findByUserName(username).orElseThrow();
        perro.setEstadoRevision(PerroEstadoRevision.RECHAZADO);
        perro.setRevisadoPor(moderador);
        perro.setFechaRevision(new Timestamp(System.currentTimeMillis()));
        return new DtoPerro(repositorioPerro.save(perro));
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public DtoPerro cambiarEstadoAdopcion(String perroId, String estado) {
        Perro perro = repositorioPerro.findById(perroId).orElseThrow();
        PerroEstadoAdopcion nuevo = PerroEstadoAdopcion.fromLabel(estado);
        if (nuevo == PerroEstadoAdopcion.DISPONIBLE && perro.getEstadoRevision() != PerroEstadoRevision.APROBADO) {
            throw new UnprocessableEntityException("Solo perros aprobados pueden estar disponibles");
        }
        perro.setEstadoAdopcion(nuevo);
        return new DtoPerro(repositorioPerro.save(perro));
    }
}
