package com.cut.cardona.modelo.usuarios;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import java.sql.Timestamp;
import java.util.UUID; // ✅ Import faltante agregado
import com.cut.cardona.modelo.dto.registro.DtoRegistroUsuario; // ✅ Usar DTO en paquete correcto


@Entity
@Table(name = "usuarios")
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class Usuario implements UserDetails {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "user_name", unique = true, nullable = false)
    private String userName;

    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "fecha_creacion", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp fechaCreacion;

    @Column(name = "ultimo_acceso")
    private Timestamp ultimoAcceso;

    @Column(name = "activo", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean activo;

    @Column(name = "email_verificado", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean emailVerificado;

    @Column(name = "token")
    private String token;

    @Column(name = "fecha_expiracion_token")
    private Timestamp fechaExpiracionToken;

    @Enumerated(EnumType.STRING)
    private Roles rol;

    // Métodos getter/setter explícitos para resolver conflictos
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Roles getRol() {
        return rol;
    }

    public void setRol(Roles rol) {
        this.rol = rol;
    }

    public Usuario(DtoRegistroUsuario registroUsuario, PasswordEncoder passwordEncoder) {
        this.userName = registroUsuario.userName();
        this.email = registroUsuario.email() != null ? registroUsuario.email().trim().toLowerCase() : null;
        this.password = passwordEncoder.encode(registroUsuario.password());
        this.activo = false; // Ahora inactivo hasta verificar email
        this.emailVerificado = false;
        this.rol = Roles.ROLE_USER;
        this.id = UUID.randomUUID().toString();
        this.fechaCreacion = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        SimpleGrantedAuthority simpleGrantedAuthority = new SimpleGrantedAuthority(rol.toString());
        return List.of(simpleGrantedAuthority);
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActivo();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Usuario usuario = (Usuario) o;
        return getId() != null && Objects.equals(getId(), usuario.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public boolean isActivo() {
        return activo != null && activo;
    }

    @PrePersist
    private void prePersist() {
        if (this.id == null || this.id.isBlank()) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.fechaCreacion == null) {
            this.fechaCreacion = new Timestamp(System.currentTimeMillis());
        }
        if (this.activo == null) {
            this.activo = false; // default inactivo hasta verificar
        }
        if (this.emailVerificado == null) {
            this.emailVerificado = false;
        }
        if (this.rol == null) {
            this.rol = Roles.ROLE_USER;
        }
    }
}