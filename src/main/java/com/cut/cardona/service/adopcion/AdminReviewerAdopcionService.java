package com.cut.cardona.service.adopcion;

import com.cut.cardona.errores.DomainConflictException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReviewerAdopcionService {

    private final RepositorioSolicitudAdopcion repoSolicitud;
    private final RepositorioDocumentoSolicitud repoDocumento;
    private final RepositorioPerro repoPerro;
    private final RepositorioUsuario repoUsuario;

    private Usuario currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return repoUsuario.findByUserName(username).orElseThrow();
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public List<DtoSolicitudAdopcion> pendientesRevision() {
        return repoSolicitud.findByEstado(SolicitudEstado.PENDIENTE).stream()
                .map(DtoSolicitudAdopcion::from)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public List<DtoSolicitudAdopcion> buscar(String estado, String perroId, String solicitanteId) {
        SolicitudEstado st = null;
        if (estado != null && !estado.isBlank()) st = SolicitudEstado.fromLabel(estado);
        return repoSolicitud.buscarFiltrado(st, perroId, solicitanteId).stream()
                .map(DtoSolicitudAdopcion::from)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    @Transactional
    public DtoSolicitudAdopcion actualizarEstado(String solicitudId, String nuevoEstadoLabel) {
        Usuario revisor = currentUser();
        SolicitudAdopcion s = repoSolicitud.findById(solicitudId).orElseThrow();
        SolicitudEstado nuevoEstado = SolicitudEstado.fromLabel(nuevoEstadoLabel);

        // Validar transiciones básicas
        if (s.getEstado() == SolicitudEstado.CANCELADA) {
            throw new UnprocessableEntityException("No se puede modificar una solicitud cancelada por el usuario");
        }
        if (s.getEstado() == SolicitudEstado.ACEPTADA && nuevoEstado != SolicitudEstado.ACEPTADA) {
            throw new UnprocessableEntityException("Una solicitud aceptada no puede cambiar a otro estado");
        }

        // Reglas de documentos cuando pasa a EN_REVISION o ACEPTADA
        if (nuevoEstado == SolicitudEstado.EN_REVISION || nuevoEstado == SolicitudEstado.ACEPTADA) {
            long tipos = repoDocumento.contarTiposPorSolicitud(solicitudId);
            if (tipos < 2) {
                throw new UnprocessableEntityException("Faltan documentos requeridos para continuar la revisión");
            }
        }

        if (nuevoEstado == SolicitudEstado.ACEPTADA) {
            Perro perro = s.getPerro();
            if (perro.getEstadoRevision() != PerroEstadoRevision.APROBADO || perro.getEstadoAdopcion() != PerroEstadoAdopcion.DISPONIBLE) {
                throw new UnprocessableEntityException("El perro no está disponible para ser adoptado");
            }
            long aceptadas = repoSolicitud.countAceptadasByPerro(perro.getId());
            if (aceptadas > 0) {
                throw new DomainConflictException("Ya existe una solicitud aceptada para este perro");
            }
            // Marcar perro como adoptado
            perro.setEstadoAdopcion(PerroEstadoAdopcion.ADOPTADO);
            repoPerro.save(perro);
            // Rechazar las demás solicitudes pendientes/en revisión para el mismo perro
            repoSolicitud.findByPerroId(perro.getId()).forEach(sol -> {
                if (!sol.getId().equals(s.getId()) && sol.getEstado() != SolicitudEstado.RECHAZADA && sol.getEstado() != SolicitudEstado.CANCELADA && sol.getEstado() != SolicitudEstado.ACEPTADA) {
                    sol.setEstado(SolicitudEstado.RECHAZADA);
                    sol.setFechaRespuesta(new Timestamp(System.currentTimeMillis()));
                    sol.setRevisadoPor(revisor);
                    repoSolicitud.save(sol);
                }
            });
        }

        s.setEstado(nuevoEstado);
        s.setRevisadoPor(revisor);
        s.setFechaRespuesta(new Timestamp(System.currentTimeMillis()));
        repoSolicitud.save(s);
        return DtoSolicitudAdopcion.from(s);
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    @Transactional
    public DtoSolicitudAdopcion revertirAdopcion(String solicitudId) {
        Usuario revisor = currentUser();
        SolicitudAdopcion s = repoSolicitud.findById(solicitudId).orElseThrow();
        if (s.getEstado() != SolicitudEstado.ACEPTADA) {
            throw new UnprocessableEntityException("Solo se puede revertir una solicitud aceptada");
        }
        Perro perro = s.getPerro();
        // Cambiar estado solicitud a RECHAZADA para evitar múltiples aceptadas históricas
        s.setEstado(SolicitudEstado.RECHAZADA);
        s.setFechaRespuesta(new Timestamp(System.currentTimeMillis()));
        s.setRevisadoPor(revisor);
        repoSolicitud.save(s);
        // Volver perro a Disponible si estaba Adoptado y sigue aprobado
        if (perro.getEstadoAdopcion() == PerroEstadoAdopcion.ADOPTADO) {
            if (perro.getEstadoRevision() != PerroEstadoRevision.APROBADO) {
                throw new UnprocessableEntityException("No se puede volver a Disponible un perro no aprobado");
            }
            perro.setEstadoAdopcion(PerroEstadoAdopcion.DISPONIBLE);
            repoPerro.save(perro);
        }
        return DtoSolicitudAdopcion.from(s);
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public List<DtoSolicitudAdopcion.Documento> listarDocumentos(String solicitudId) {
        repoSolicitud.findById(solicitudId).orElseThrow();
        return repoDocumento.findBySolicitud_Id(solicitudId).stream()
                .map(DtoSolicitudAdopcion.Documento::of)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN','REVIEWER')")
    public DtoSolicitudAdopcion.Documento obtenerDocumento(String solicitudId, String documentoId) {
        DocumentoSolicitud d = repoDocumento.findById(documentoId).orElseThrow();
        if (d.getSolicitud() == null || !d.getSolicitud().getId().equals(solicitudId)) {
            throw new IllegalArgumentException("Documento no pertenece a la solicitud");
        }
        return DtoSolicitudAdopcion.Documento.of(d);
    }
}
