package com.cut.cardona.service.perros;

import com.cut.cardona.service.infra.storage.ImageStorageService;
import com.cut.cardona.modelo.dto.perros.ActualizarPerroRequest;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PerroService {

    // TODO(Fase2): extraer attachImagenes(Perro, List<RequestImagen>) para reutilizar en PATCH
    // TODO(Fase2): implementar planUpdateImagenes(perro, request) -> {toAdd, toRemove, toKeep, nuevaPrincipal}
    // TODO(Fase5): método eliminarPerro(String id) con borrado diferido Cloudinary

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

    private void validateLimiteImagenes(List<com.cut.cardona.modelo.dto.perros.ImagenPerroRequest> imagenes) {
        if (imagenes.size() > MAX_IMGS) {
            for (int i = MAX_IMGS; i < imagenes.size(); i++) { // limpieza sobrantes subida previa
                try {
                    var extra = imagenes.get(i);
                    if (extra != null && extra.id() != null) {
                        imageStorageService.deleteDogImage(extra.id());
                    }
                } catch (Exception ignored) {}
            }
            throw new UnprocessableEntityException("Solo se permiten hasta " + MAX_IMGS + " imágenes por perro");
        }
        if (imagenes.isEmpty()) {
            throw new UnprocessableEntityException("Debe incluir al menos una imagen");
        }
    }

    private void validatePrincipalUnica(List<com.cut.cardona.modelo.dto.perros.ImagenPerroRequest> imagenes) {
        long count = imagenes.stream().filter(i -> Boolean.TRUE.equals(i.principal())).count();
        if (count != 1) throw new UnprocessableEntityException("Debe marcar exactamente una imagen como principal");
    }

    private void validateImagenIds(List<com.cut.cardona.modelo.dto.perros.ImagenPerroRequest> imagenes) {
        Set<String> vistos = new HashSet<>();
        for (var imgReq : imagenes) {
            String id = imgReq.id();
            if (id == null || id.isBlank()) throw new UnprocessableEntityException("ID de imagen faltante");
            try { UUID.fromString(id); } catch (IllegalArgumentException ex) {
                throw new UnprocessableEntityException("ID de imagen inválido: " + id);
            }
            if (!vistos.add(id)) throw new UnprocessableEntityException("IDs de imagen duplicados");
        }
    }

    private void attachImagenes(Perro perro, List<com.cut.cardona.modelo.dto.perros.ImagenPerroRequest> imagenes) {
        boolean principalYaAsignada = false;
        for (var imgReq : imagenes) {
            boolean principal = Boolean.TRUE.equals(imgReq.principal());
            if (principal && principalYaAsignada) principal = false;
            principalYaAsignada = principalYaAsignada || principal;
            String imagenId = imgReq.id();
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
    }



    private record UpdatePlan(Set<String> toAdd, Set<String> toRemove, Set<String> toKeep, String principalId) {}

    // TODO(Fase2): tests unitarios de planUpdateImagenes
    private UpdatePlan planUpdateImagenes(Perro perro, List<com.cut.cardona.modelo.dto.perros.ImagenPerroRequest> nuevas) {
        validateLimiteImagenes(nuevas);
        validatePrincipalUnica(nuevas);
        validateImagenIds(nuevas);
        Set<String> actuales = repositorioImagenPerro.findByPerro_Id(perro.getId()).stream().map(ImagenPerro::getId).collect(java.util.stream.Collectors.toSet());
        Set<String> nuevasIds = nuevas.stream().map(com.cut.cardona.modelo.dto.perros.ImagenPerroRequest::id).collect(java.util.stream.Collectors.toSet());
        Set<String> toKeep = new HashSet<>(actuales);
        toKeep.retainAll(nuevasIds);
        Set<String> toAdd = new HashSet<>(nuevasIds);
        toAdd.removeAll(actuales);
        Set<String> toRemove = new HashSet<>(actuales);
        toRemove.removeAll(nuevasIds);
        String principalId = nuevas.stream().filter(i -> Boolean.TRUE.equals(i.principal())).findFirst().map(com.cut.cardona.modelo.dto.perros.ImagenPerroRequest::id).orElse(null);
        return new UpdatePlan(toAdd, toRemove, toKeep, principalId);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'REVIEWER')")
    @Transactional
    public DtoPerro crearPerro(CrearPerroRequest req) {
        // Validaciones de imágenes solo si vienen
        if (req.imagenes() != null && !req.imagenes().isEmpty()) {
            validateLimiteImagenes(req.imagenes());
            validatePrincipalUnica(req.imagenes());
            validateImagenIds(req.imagenes());
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
        if (req.imagenes() != null && !req.imagenes().isEmpty()) {
            attachImagenes(perro, req.imagenes());
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


    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public DtoPerro cambiarRevision(String id, String revision) {
        Perro perro = repositorioPerro.findById(id).orElseThrow();
        PerroEstadoRevision anterior = perro.getEstadoRevision();
        PerroEstadoRevision nuevo = PerroEstadoRevision.fromLabel(revision);
        if (nuevo == PerroEstadoRevision.RECHAZADO && perro.getEstadoAdopcion() == PerroEstadoAdopcion.ADOPTADO) {
            throw new UnprocessableEntityException("No se puede cambiar el estado de este perro");
        }
        if (nuevo == PerroEstadoRevision.RECHAZADO){
            perro.setFechaRevision(new Timestamp(System.currentTimeMillis()));
            perro.setRevisadoPor(
                    repositorioUsuario.findByUserName(SecurityContextHolder.getContext().getAuthentication().getName())
                            .orElseThrow()
            );
            perro.setEstadoAdopcion(PerroEstadoAdopcion.NO_DISPONIBLE);
        }
        if (nuevo == PerroEstadoRevision.APROBADO ){
            perro.setFechaRevision(new Timestamp(System.currentTimeMillis()));
            perro.setRevisadoPor(
                    repositorioUsuario.findByUserName(SecurityContextHolder.getContext().getAuthentication().getName())
                            .orElseThrow()
            );
            perro.setEstadoAdopcion(PerroEstadoAdopcion.DISPONIBLE);
        }
        if (nuevo == PerroEstadoRevision.PENDIENTE) {
            perro.setFechaRevision(new Timestamp(System.currentTimeMillis()));
            perro.setRevisadoPor(
                    repositorioUsuario.findByUserName(SecurityContextHolder.getContext().getAuthentication().getName())
                            .orElseThrow()
            );
            perro.setEstadoAdopcion(PerroEstadoAdopcion.PENDIENTE);
        }
        // Si estaba en catálogo y se cambia a no aprobado, poner a No Disponible);
        perro.setEstadoRevision(nuevo);
        Perro saved = repositorioPerro.save(perro);
        return new DtoPerro(saved);
    }

    @Transactional(readOnly = true)
    public DtoPerro obtenerPerro(String id) {
        Perro perro = repositorioPerro.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Perro no encontrado"));
        return new DtoPerro(perro);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'REVIEWER')")
    @Transactional
    public DtoPerro actualizarPerro(String perroId, ActualizarPerroRequest req) {
        Perro perro = repositorioPerro.findById(perroId).orElseThrow(() -> new IllegalArgumentException("Perro no encontrado"));
        if (perro.getEstadoAdopcion() == PerroEstadoAdopcion.ADOPTADO || perro.getEstadoAdopcion() == PerroEstadoAdopcion.NO_DISPONIBLE) {
            throw new UnprocessableEntityException("No se puede editar un perro que está adoptado o no disponible");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean esPrivilegiado = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_REVIEWER"));
        if (!esPrivilegiado && (perro.getUsuario() == null || !perro.getUsuario().getUsername().equals(username))) {
            throw new SecurityException("No autorizado para actualizar este perro");
        }
        var plan = planUpdateImagenes(perro, req.imagenes());
        if (req.nombre() != null) perro.setNombre(req.nombre());
        if (req.edad() != null) perro.setEdad(req.edad());
        if (req.sexo() != null) perro.setSexo(req.sexo());
        if (req.tamano() != null) perro.setTamano(req.tamano());
        if (req.raza() != null) perro.setRaza(req.raza());
        if (req.descripcion() != null) perro.setDescripcion(req.descripcion());
        if (req.ubicacion() != null) perro.setUbicacion(req.ubicacion());

        java.util.Map<String, com.cut.cardona.modelo.dto.perros.ImagenPerroRequest> reqMap = req.imagenes().stream()
                .collect(java.util.stream.Collectors.toMap(com.cut.cardona.modelo.dto.perros.ImagenPerroRequest::id, i -> i));

        if (!plan.toRemove().isEmpty()) {
            for (String rid : plan.toRemove()) {
                repositorioImagenPerro.findById(rid).ifPresent(repositorioImagenPerro::delete);
            }
        }
        if (!plan.toAdd().isEmpty()) {
            for (String nid : plan.toAdd()) {
                var imgReq = reqMap.get(nid);
                if (imgReq != null) {
                    String publicUrl = imageStorageService.resolveDogImagePublicUrl(nid);
                    ImagenPerro nueva = ImagenPerro.builder()
                            .id(nid)
                            .perro(perro)
                            .url(publicUrl)
                            .descripcion(imgReq.descripcion())
                            .principal(false) // se ajustará después de clearPrincipal
                            .fechaSubida(new Timestamp(System.currentTimeMillis()))
                            .build();
                    repositorioImagenPerro.save(nueva);
                }
            }
        }
        if (!plan.toKeep().isEmpty()) {
            for (String kid : plan.toKeep()) {
                repositorioImagenPerro.findById(kid).ifPresent(img -> {
                    img.setDescripcion(reqMap.get(kid) != null ? reqMap.get(kid).descripcion() : img.getDescripcion());
                    repositorioImagenPerro.save(img);
                });
            }
        }

        // Reasignación atómica de principal usando bulk update para limpiar primero.
        if (plan.principalId() != null) {
            // 1) Desmarcar cualquier principal existente (single UPDATE)
            repositorioImagenPerro.clearPrincipal(perro.getId());
            // 2) Marcar la nueva principal (existente o recién agregada)
            repositorioImagenPerro.findById(plan.principalId()).ifPresent(img -> {
                if (!Boolean.TRUE.equals(img.getPrincipal())) {
                    img.setPrincipal(true);
                    repositorioImagenPerro.save(img);
                }
            });
        }

        repositorioPerro.save(perro);

        if (!plan.toRemove().isEmpty()) {
            List<String> removidas = List.copyOf(plan.toRemove());
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        removidas.forEach(r -> { try { imageStorageService.deleteDogImage(r); } catch (Exception ignored) {} });
                    }
                });
            } else {
                removidas.forEach(r -> { try { imageStorageService.deleteDogImage(r); } catch (Exception ignored) {} });
            }
        }
        return new DtoPerro(perro);
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'REVIEWER')")
    @Transactional
    public void eliminarPerro(String perroId) {
        Perro perro = repositorioPerro.findById(perroId)
                .orElseThrow(() -> new IllegalArgumentException("Perro no encontrado"));
        // Regla de negocio: no permitir eliminar perros adoptados o no disponibles
        if (perro.getEstadoAdopcion() == PerroEstadoAdopcion.ADOPTADO || perro.getEstadoAdopcion() == PerroEstadoAdopcion.NO_DISPONIBLE) {
            throw new UnprocessableEntityException("No se puede eliminar un perro que está adoptado o no disponible");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean esPrivilegiado = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_REVIEWER"));
        if (!esPrivilegiado && (perro.getUsuario() == null || !perro.getUsuario().getUsername().equals(username))) {
            throw new SecurityException("No autorizado para eliminar este perro");
        }
        List<String> imagenIds = repositorioImagenPerro.findByPerro_Id(perroId).stream()
                .map(ImagenPerro::getId)
                .toList();
        repositorioPerro.delete(perro);
        if (!imagenIds.isEmpty()) {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        imagenIds.forEach(id -> { try { imageStorageService.deleteDogImage(id); } catch (Exception ignored) {} });
                    }
                });
            } else {
                imagenIds.forEach(id -> { try { imageStorageService.deleteDogImage(id); } catch (Exception ignored) {} });
            }
        }
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'REVIEWER')")
    @Transactional
    public ImagenPerro agregarImagen(String perroId, org.springframework.web.multipart.MultipartFile file, String descripcion, Boolean principal) {
        Perro perro = repositorioPerro.findById(perroId)
                .orElseThrow(() -> new IllegalArgumentException("Perro no encontrado"));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        boolean esPrivilegiado = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .anyMatch(r -> r.equals("ROLE_ADMIN") || r.equals("ROLE_REVIEWER"));
        if (!esPrivilegiado && (perro.getUsuario() == null || !perro.getUsuario().getUsername().equals(username))) {
            throw new SecurityException("No autorizado para agregar imágenes a este perro");
        }
        int existentes = repositorioImagenPerro.findByPerro_Id(perroId).size();
        if (existentes >= MAX_IMGS) {
            throw new UnprocessableEntityException("Solo se permiten hasta " + MAX_IMGS + " imágenes por perro");
        }
        if (file == null || file.isEmpty()) {
            throw new UnprocessableEntityException("Archivo vacío");
        }
        if (file.getSize() > 15L * 1024 * 1024) {
            throw new UnprocessableEntityException("El archivo supera el tamaño máximo de 15MB");
        }
        final String uploadId;
        try {
            var upload = imageStorageService.uploadDogImage(file);
            uploadId = upload.getId();
        } catch (Exception ex) {
            throw new UnprocessableEntityException(ex.getMessage() != null ? ex.getMessage() : "Error al subir imagen");
        }
        boolean principalRequested = Boolean.TRUE.equals(principal);
        if (principalRequested) {
            // Limpiar principal previa antes de insertar nueva para evitar conflicto de índice único
            repositorioImagenPerro.clearPrincipal(perroId);
        }
        String publicUrl = imageStorageService.resolveDogImagePublicUrl(uploadId);
        ImagenPerro img = ImagenPerro.builder()
                .id(uploadId)
                .perro(perro)
                .url(publicUrl)
                .descripcion(descripcion)
                .principal(principalRequested)
                .fechaSubida(new Timestamp(System.currentTimeMillis()))
                .build();
        repositorioImagenPerro.save(img);
        return img;
    }

}
