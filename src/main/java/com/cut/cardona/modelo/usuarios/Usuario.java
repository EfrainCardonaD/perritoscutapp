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


@Entity
@Table(name = "usuarios")
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class Usuario implements UserDetails {
    /*
        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)
        private Long id;*/
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

    @Column(name = "token")
    private String token;

    @Column(name = "fecha_expiracion_token")
    private Timestamp fechaExpiracionToken;

    @Enumerated(EnumType.STRING)
    private Roles rol;


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
        return activo;
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
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }

    public Usuario(DtoRegistroUsuario dtoRegistroUsuario, PasswordEncoder passwordEncoder) {
        this.userName = dtoRegistroUsuario.userName();
        this.email = dtoRegistroUsuario.email();
        this.password = passwordEncoder.encode(dtoRegistroUsuario.password());
        this.activo = true;
        this.fechaCreacion = new Timestamp(System.currentTimeMillis());
    }


    @PrePersist
    public void prePersist() {
        this.id = UUID.randomUUID().toString();
    }

    public boolean isActivo() {
        return true;
    }
}