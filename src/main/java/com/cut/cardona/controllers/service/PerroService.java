package com.cut.cardona.controllers.service;

import com.cut.cardona.infra.storage.ImageStorageService;
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

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PerroService {

    private static final int MAX_IMGS = 5;
    private final RepositorioPerro repositorioPerro;
    private final RepositorioImagenPerro repositorioImagenPerro;
    private final RepositorioUsuario repositorioUsuario;
    private final ImageStorageService imageStorageService;

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
        return usuarioOpt.map(usuario -> repositorioPerro.findByUsuarioId(usuario.getId()).stream().map(DtoPerro::new).toList()).orElseGet(List::of);
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    @Transactional(readOnly = true)
    public List<DtoPerro> pendientesRevision() {
        return repositorioPerro.findPendientesRevision().stream().map(DtoPerro::new).toList();
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'REVIEWER')")
    @Transactional
    public DtoPerro crearPerro(CrearPerroRequest req) {
        if (req.imagenes() == null || req.imagenes().isEmpty()) {
            throw new UnprocessableEntityException("Debe incluir al menos una imagen");
        }
        if (req.imagenes().size() > MAX_IMGS) {
            // Cleanup de imágenes sobrantes (índices >= MAX_IMGS)
            for (int i = MAX_IMGS; i < req.imagenes().size(); i++) {
                try {
                    var extra = req.imagenes().get(i);
                    if (extra != null && extra.id() != null) {
                        imageStorageService.deleteDogImage(extra.id());
                    }
                } catch (Exception ignored) {}
            }
            throw new UnprocessableEntityException("Solo se permiten hasta " + MAX_IMGS + " imágenes por perro");
        }
        // Validaciones de negocio previas a guardar nada
        long principales = req.imagenes().stream().filter(i -> Boolean.TRUE.equals(i.principal())).count();
        if (principales != 1) {
            throw new UnprocessableEntityException("Debe marcar exactamente una imagen como principal");
        }
        Set<String> vistos = new HashSet<>();
        for (var imgReq : req.imagenes()) {
            String imagenId = imgReq.id();
            if (imagenId == null || imagenId.isBlank()) {
                throw new UnprocessableEntityException("ID de imagen faltante");
            }
            // Validar UUID
            try { UUID.fromString(imagenId); } catch (IllegalArgumentException ex) {
                throw new UnprocessableEntityException("ID de imagen inválido: " + imagenId);
            }
            if (!vistos.add(imagenId)) {
                throw new UnprocessableEntityException("IDs de imagen duplicados");
            }
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

            String imagenId = imgReq.id();
            // Resolver URL pública desde Cloudinary para almacenarla en BD
            String publicUrl = imageStorageService.resolveDogImagePublicUrl(imagenId);

            ImagenPerro img = ImagenPerro.builder()
                    .id(imagenId)
                    .perro(perro)
                    .url(publicUrl)
                    .descripcion(imgReq.descripcion())
                    .principal(principal)
                    .fechaSubida(new Timestamp(System.currentTimeMillis()))
                    .build();
            repositorioImagenPerro.save(img);
        }
        return new DtoPerro(perro);
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
        // Si estaba pendiente de adopción, lo ponemos Disponible automáticamente
        if (perro.getEstadoAdopcion() == PerroEstadoAdopcion.PENDIENTE) {
            perro.setEstadoAdopcion(PerroEstadoAdopcion.DISPONIBLE);
        }
        perro.setRevisadoPor(moderador);
        perro.setFechaRevision(new Timestamp(System.currentTimeMillis()));
        Perro saved = repositorioPerro.save(perro);
        return new DtoPerro(saved);
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
        PerroEstadoAdopcion anterior = perro.getEstadoAdopcion();
        boolean estabaEnCatalogo = perro.getEstadoRevision() == PerroEstadoRevision.APROBADO && anterior == PerroEstadoAdopcion.DISPONIBLE;
        PerroEstadoAdopcion nuevo = PerroEstadoAdopcion.fromLabel(estado);
        if (nuevo == PerroEstadoAdopcion.DISPONIBLE && perro.getEstadoRevision() != PerroEstadoRevision.APROBADO) {
            throw new UnprocessableEntityException("Solo perros aprobados pueden estar disponibles");
        }
        perro.setEstadoAdopcion(nuevo);
        Perro saved = repositorioPerro.save(perro);
        return new DtoPerro(saved);
    }
}
