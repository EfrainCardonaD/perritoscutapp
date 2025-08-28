package com.cut.cardona.modelo.perfil;

import com.cut.cardona.modelo.imagenes.ImagenPerfil;
import com.cut.cardona.modelo.usuarios.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "perfiles_usuario")
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class PerfilUsuario {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "perfilUsuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ImagenPerfil> imagenesPerfil;

    @Column(name = "nombre_real", length = 100)
    private String nombreReal;

    @Pattern(regexp = "^(\\+?\\d{1,4}[\\s\\-]?)?\\d{10,14}$", message = "Formato de teléfono inválido. Ejemplo: +52 1234567890 o 1234567890")
    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "idioma", length = 10, columnDefinition = "VARCHAR(10) DEFAULT 'es'")
    private String idioma;

    @Column(name = "zona_horaria", length = 50, columnDefinition = "VARCHAR(50) DEFAULT 'America/Mexico_City'")
    private String zonaHoraria;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "fecha_creacion", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp fechaCreacion;

    @Column(name = "fecha_actualizacion", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Timestamp fechaActualizacion;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.fechaCreacion == null) {
            this.fechaCreacion = new Timestamp(System.currentTimeMillis());
        }
        if (this.idioma == null) {
            this.idioma = "es";
        }
        if (this.zonaHoraria == null) {
            this.zonaHoraria = "America/Mexico_City";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = new Timestamp(System.currentTimeMillis());
    }

    // Método para validar mayoría de edad (18+ años)
    public boolean esMayorDeEdad() {
        if (fechaNacimiento == null) {
            return false;
        }
        return fechaNacimiento.isBefore(LocalDate.now().minusYears(18));
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PerfilUsuario that = (PerfilUsuario) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
