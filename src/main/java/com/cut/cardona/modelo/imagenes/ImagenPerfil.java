package com.cut.cardona.modelo.imagenes;

import com.cut.cardona.modelo.perfil.PerfilUsuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "imagenes_perfil")
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class ImagenPerfil {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "perfil_usuario_id", nullable = false, columnDefinition = "CHAR(36)", insertable = false, updatable = false)
    private String perfilUsuarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_usuario_id", referencedColumnName = "id", nullable = false)
    private PerfilUsuario perfilUsuario;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "tipo_mime", length = 100)
    private String tipoMime;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "url_publica", length = 500)
    private String urlPublica;

    @Column(name = "activa", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean activa;

    @Column(name = "fecha_subida", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp fechaSubida;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.fechaSubida == null) {
            this.fechaSubida = new Timestamp(System.currentTimeMillis());
        }
        if (this.activa == null) {
            this.activa = true;
        }
    }

    // Método para validar tipos de imagen permitidos
    public boolean esTipoImagenValido() {
        if (tipoMime == null) return false;
        return tipoMime.equals("image/jpeg") ||
               tipoMime.equals("image/png") ||
               tipoMime.equals("image/webp") ||
               tipoMime.equals("image/gif");
    }

    // Método para validar tamaño máximo (5MB)
    public boolean esTamanoValido() {
        if (tamanoBytes == null) return false;
        return tamanoBytes <= 5_242_880L; // 5MB
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        ImagenPerfil that = (ImagenPerfil) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
