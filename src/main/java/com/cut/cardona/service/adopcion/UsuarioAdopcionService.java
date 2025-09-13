package com.cut.cardona.service.adopcion;

import com.cut.cardona.errores.UnprocessableEntityException;
import com.cut.cardona.modelo.adopcion.DocumentoSolicitud;
import com.cut.cardona.modelo.adopcion.RepositorioDocumentoSolicitud;
import com.cut.cardona.modelo.adopcion.RepositorioSolicitudAdopcion;
import com.cut.cardona.modelo.adopcion.SolicitudAdopcion;
import com.cut.cardona.modelo.adopcion.enums.SolicitudEstado;
import com.cut.cardona.modelo.dto.adopcion.DtoSolicitudAdopcion;
import com.cut.cardona.modelo.perros.Perro;
import com.cut.cardona.modelo.perros.RepositorioPerro;
import com.cut.cardona.modelo.perros.enums.PerroEstadoAdopcion;
import com.cut.cardona.modelo.perros.enums.PerroEstadoRevision;
import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import com.cut.cardona.service.infra.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioAdopcionService {
    private final RepositorioSolicitudAdopcion repoSolicitud;
    private final RepositorioDocumentoSolicitud repoDocumento;
    private final RepositorioPerro repoPerro;
    private final RepositorioUsuario repoUsuario;
    private final ImageStorageService imageStorageService;

    private Usuario currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return repoUsuario.findByUserName(username).orElseThrow();
    }

    // -------------------- CREAR SOLICITUD --------------------
    @PreAuthorize("hasRole('USER')")
    @Transactional
    public DtoSolicitudAdopcion crearSolicitud(String perroId, String mensaje, String tipoDocumento, MultipartFile file) throws Exception {
        Usuario solicitante = currentUser();
        Perro perro = repoPerro.findById(perroId).orElseThrow();
        if (perro.getUsuario() != null && solicitante.getId().equals(perro.getUsuario().getId())) {
            throw new UnprocessableEntityException("No puedes solicitar adopción de tu propio perro");
        }
        if (perro.getEstadoRevision() != PerroEstadoRevision.APROBADO || perro.getEstadoAdopcion() != PerroEstadoAdopcion.DISPONIBLE) {
            throw new UnprocessableEntityException("Solo se puede solicitar adopción de perros aprobados y disponibles");
        }
        SolicitudAdopcion s = SolicitudAdopcion.builder()
                .id(UUID.randomUUID().toString())
                .perro(perro)
                .solicitante(solicitante)
                .estado(SolicitudEstado.PENDIENTE)
                .mensaje(mensaje)
                .fechaSolicitud(new Timestamp(System.currentTimeMillis()))
                .build();
        repoSolicitud.save(s);

        var upload = imageStorageService.uploadDocumentImage(file);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    try { imageStorageService.deleteDocumentImage(upload.getId()); } catch (Exception ignored) {}
                }
            }
        });
        try {
            String tipoDoc = (tipoDocumento == null || tipoDocumento.isBlank()) ? "Documento" : tipoDocumento.trim();
            DocumentoSolicitud d = DocumentoSolicitud.builder()
                    .id(upload.getId())
                    .solicitud(s)
                    .tipoDocumento(tipoDoc)
                    .urlDocumento(upload.getUrl())
                    .nombreArchivo(upload.getFilename())
                    .tipoMime(upload.getContentType())
                    .tamanoBytes(upload.getSize())
                    .fechaSubida(new Timestamp(System.currentTimeMillis()))
                    .build();
            repoDocumento.save(d);
            // Añadir a la colección en memoria para que el DTO detalle lo incluya sin recarga
            s.getDocumentos().add(d);
            return DtoSolicitudAdopcion.fromWithDocumentos(s);
        } catch (RuntimeException ex) {
            try { imageStorageService.deleteDocumentImage(upload.getId()); } catch (Exception ignored) {}
            throw ex;
        }
    }

    // -------------------- CONSULTAS DEL USUARIO --------------------
    @PreAuthorize("hasRole('USER')")
    public List<DtoSolicitudAdopcion> misSolicitudes() {
        Usuario solicitante = currentUser();
        return repoSolicitud.findBySolicitanteId(solicitante.getId()).stream()
                .map(DtoSolicitudAdopcion::from)
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    public DtoSolicitudAdopcion obtenerSolicitud(String solicitudId) {
        SolicitudAdopcion s = repoSolicitud.findById(solicitudId).orElseThrow();
        Usuario requester = currentUser();
        boolean isOwner = s.getSolicitante() != null && requester.getId().equals(s.getSolicitante().getId());
        boolean hasPriv = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_REVIEWER"));
        if (!isOwner && !hasPriv) {
            throw new org.springframework.security.access.AccessDeniedException("No autorizado para ver esta solicitud");
        }
        return DtoSolicitudAdopcion.fromWithDocumentos(s);
    }

    @PreAuthorize("isAuthenticated()")
    public void eliminarSolicitud(String solicitudId) {
        SolicitudAdopcion s = repoSolicitud.findById(solicitudId).orElseThrow();
        Usuario requester = currentUser();
        boolean isOwner = s.getSolicitante() != null && requester.getId().equals(s.getSolicitante().getId());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isOwner && !isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("No autorizado para eliminar esta solicitud");
        }
        if (s.getEstado() != SolicitudEstado.PENDIENTE && !isAdmin) {
            throw new UnprocessableEntityException("Solo se puede eliminar una solicitud pendiente");
        }
        List<String> docIds = s.getDocumentos().stream().map(DocumentoSolicitud::getId).collect(Collectors.toList());
        repoSolicitud.delete(s);
        for (String id : docIds) {
            try { imageStorageService.deleteDocumentImage(id); } catch (Exception ignored) {}
        }
    }

    // -------------------- MODIFICACIONES DEL USUARIO --------------------
    @PreAuthorize("hasRole('USER')")
    public DtoSolicitudAdopcion actualizarMensaje(String solicitudId, DtoSolicitudAdopcion req) {
        SolicitudAdopcion s = repoSolicitud.findById(solicitudId).orElseThrow();
        Usuario u = currentUser();
        if (!u.getId().equals(s.getSolicitante().getId())) {
            throw new org.springframework.security.access.AccessDeniedException("No autorizado");
        }
        if (s.getEstado() != SolicitudEstado.PENDIENTE) {
            throw new UnprocessableEntityException("Solo se puede actualizar mensaje si la solicitud está pendiente");
        }
        s.setMensaje(req.mensaje());
        repoSolicitud.save(s);
        return DtoSolicitudAdopcion.from(s);
    }

    @PreAuthorize("hasRole('USER')")
    public DtoSolicitudAdopcion cancelarSolicitud(String solicitudId) {
        SolicitudAdopcion s = repoSolicitud.findById(solicitudId).orElseThrow();
        Usuario u = currentUser();
        if (!u.getId().equals(s.getSolicitante().getId())) {
            throw new org.springframework.security.access.AccessDeniedException("No autorizado");
        }
        if (s.getEstado() == SolicitudEstado.ACEPTADA || s.getEstado() == SolicitudEstado.RECHAZADA) {
            throw new UnprocessableEntityException("No se puede cancelar una solicitud ya resuelta");
        }
        s.setEstado(SolicitudEstado.CANCELADA);
        s.setFechaRespuesta(new Timestamp(System.currentTimeMillis()));
        repoSolicitud.save(s);
        return DtoSolicitudAdopcion.from(s);
    }

    // -------------------- DOCUMENTOS --------------------
    @PreAuthorize("isAuthenticated()")
    public List<DtoSolicitudAdopcion.Documento> listarDocumentos(String solicitudId) {
        SolicitudAdopcion s = repoSolicitud.findById(solicitudId).orElseThrow();
        Usuario requester = currentUser();
        boolean owner = s.getSolicitante() != null && requester.getId().equals(s.getSolicitante().getId());
        boolean priv = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_REVIEWER"));
        if (!owner && !priv) throw new org.springframework.security.access.AccessDeniedException("No autorizado");
        return repoDocumento.findBySolicitud_Id(solicitudId).stream()
                .map(DtoSolicitudAdopcion.Documento::of)
                .toList();
    }

    @PreAuthorize("isAuthenticated()")
    public DtoSolicitudAdopcion.Documento obtenerDocumento(String solicitudId, String documentoId) {
        DocumentoSolicitud d = repoDocumento.findById(documentoId).orElseThrow();
        if (d.getSolicitud() == null || !d.getSolicitud().getId().equals(solicitudId)) {
            throw new IllegalArgumentException("Documento no pertenece a la solicitud");
        }
        SolicitudAdopcion s = d.getSolicitud();
        Usuario requester = currentUser();
        boolean owner = s.getSolicitante() != null && requester.getId().equals(s.getSolicitante().getId());
        boolean priv = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_REVIEWER"));
        if (!owner && !priv) throw new org.springframework.security.access.AccessDeniedException("No autorizado");
        return DtoSolicitudAdopcion.Documento.of(d);
    }

}
