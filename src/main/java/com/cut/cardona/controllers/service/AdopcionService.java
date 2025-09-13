package com.cut.cardona.controllers.service;

import com.cut.cardona.modelo.adopcion.DocumentoSolicitud;
import com.cut.cardona.modelo.adopcion.RepositorioDocumentoSolicitud;
import com.cut.cardona.modelo.adopcion.RepositorioSolicitudAdopcion;
import com.cut.cardona.modelo.adopcion.SolicitudAdopcion;
import com.cut.cardona.modelo.adopcion.enums.SolicitudEstado;
import com.cut.cardona.modelo.dto.adopcion.CrearSolicitudRequest;
import com.cut.cardona.modelo.dto.adopcion.DtoSolicitud;
import com.cut.cardona.modelo.dto.adopcion.DocumentoRequest;
import com.cut.cardona.modelo.perros.Perro;
import com.cut.cardona.modelo.perros.enums.PerroEstadoAdopcion;
import com.cut.cardona.modelo.perros.enums.PerroEstadoRevision;
import com.cut.cardona.modelo.perros.RepositorioPerro;
import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import com.cut.cardona.errores.UnprocessableEntityException;
import com.cut.cardona.errores.DomainConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdopcionService {

    private final RepositorioSolicitudAdopcion repoSolicitud;
    private final RepositorioDocumentoSolicitud repoDocumento;
    private final RepositorioPerro repoPerro;
    private final RepositorioUsuario repoUsuario;

    @PreAuthorize("hasRole('USER')")
    public DtoSolicitud crearSolicitud(CrearSolicitudRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario solicitante = repoUsuario.findByUserName(username).orElseThrow();
        Perro perro = repoPerro.findById(req.perroId()).orElseThrow();

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
                .mensaje(req.mensaje())
                .fechaSolicitud(new Timestamp(System.currentTimeMillis()))
                .build();
        repoSolicitud.save(s);
        return new DtoSolicitud(s);
    }

    @PreAuthorize("hasRole('USER')")
    public void subirDocumento(String solicitudId, DocumentoRequest req) {
        SolicitudAdopcion s = repoSolicitud.findById(solicitudId).orElseThrow();
        DocumentoSolicitud d = DocumentoSolicitud.builder()
                .id(UUID.randomUUID().toString())
                .solicitud(s)
                .tipoDocumento(req.tipoDocumento())
                .urlDocumento(req.urlDocumento())
                .nombreArchivo(req.nombreArchivo())
                .tipoMime(req.tipoMime())
                .tamanoBytes(req.tamanoBytes())
                .fechaSubida(new Timestamp(System.currentTimeMillis()))
                .build();
        repoDocumento.save(d);
    }

    @PreAuthorize("hasRole('USER')")
    public List<DtoSolicitud> misSolicitudes() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario solicitante = repoUsuario.findByUserName(username).orElseThrow();
        return repoSolicitud.findBySolicitanteId(solicitante.getId()).stream().map(DtoSolicitud::new).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public List<DtoSolicitud> pendientesRevision() {
        return repoSolicitud.findByEstado(SolicitudEstado.PENDIENTE).stream().map(DtoSolicitud::new).toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public DtoSolicitud actualizarEstado(String solicitudId, String nuevoEstado) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario revisor = repoUsuario.findByUserName(username).orElseThrow();
        SolicitudAdopcion s = repoSolicitud.findById(solicitudId).orElseThrow();
        SolicitudEstado target = SolicitudEstado.fromLabel(nuevoEstado);

        if (target == SolicitudEstado.EN_REVISION || target == SolicitudEstado.ACEPTADA) {
            long tipos = repoDocumento.contarTiposPorSolicitud(solicitudId);
            if (tipos < 2) {
                throw new UnprocessableEntityException("Faltan documentos requeridos para continuar la revisión");
            }
        }
        if (target == SolicitudEstado.ACEPTADA) {
            Perro perro = s.getPerro();
            if (perro.getEstadoRevision() != PerroEstadoRevision.APROBADO || perro.getEstadoAdopcion() != PerroEstadoAdopcion.DISPONIBLE) {
                throw new UnprocessableEntityException("El perro no está disponible para ser adoptado");
            }
            long aceptadas = repoSolicitud.countAceptadasByPerro(perro.getId());
            if (aceptadas > 0) {
                throw new DomainConflictException("Ya existe una solicitud aceptada para este perro");
            }
        }

        s.setEstado(target);
        s.setRevisadoPor(revisor);
        s.setFechaRespuesta(new Timestamp(System.currentTimeMillis()));
        repoSolicitud.save(s);
        return new DtoSolicitud(s);
    }

    /**
     * Obtiene una solicitud por id. Solo el solicitante o roles ADMIN/REVIEWER pueden verla.
     */
    @PreAuthorize("isAuthenticated()")
    public DtoSolicitud obtenerSolicitud(String solicitudId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario requester = repoUsuario.findByUserName(username).orElseThrow();
        SolicitudAdopcion s = repoSolicitud.findById(solicitudId).orElseThrow();

        boolean isOwner = s.getSolicitante() != null && requester.getId().equals(s.getSolicitante().getId());
        boolean hasPriv = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_REVIEWER"));
        if (!isOwner && !hasPriv) {
            throw new org.springframework.security.access.AccessDeniedException("No autorizado para ver esta solicitud");
        }
        return new DtoSolicitud(s);
    }

    /**
     * Elimina una solicitud. Solo el solicitante o ADMIN pueden borrar.
     */
    @PreAuthorize("isAuthenticated()")
    public void eliminarSolicitud(String solicitudId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario requester = repoUsuario.findByUserName(username).orElseThrow();
        SolicitudAdopcion s = repoSolicitud.findById(solicitudId).orElseThrow();

        boolean isOwner = s.getSolicitante() != null && requester.getId().equals(s.getSolicitante().getId());
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isOwner && !isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException("No autorizado para eliminar esta solicitud");
        }
        repoSolicitud.delete(s);
    }
}
