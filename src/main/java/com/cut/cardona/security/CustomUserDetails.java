package com.cut.cardona.security;

import com.cut.cardona.modelo.usuarios.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;



public class CustomUserDetails implements UserDetails {
    private final Usuario usuario;  // Add this field
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Usuario usuario) {  // Modified constructor
        this.usuario = usuario;
        this.email = usuario.getEmail();
        this.authorities = usuario.getAuthorities();
    }

    // Add this getter
    public Usuario getUsuario() {
        return usuario;
    }

    // Keep existing UserDetails method implementations
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return usuario.getPassword();
    }

    @Override
    public String getUsername() {
        return usuario.getUsername();
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
        return usuario.isActivo();
    }

    @Override
    public boolean isEnabled() {
        return usuario.isActivo();
    }

    // ... other UserDetails methods (isEnabled, etc) ...
}