package com.cut.cardona.security;

import com.cut.cardona.modelo.usuarios.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class CustomUserDetails implements UserDetails {
    private final Usuario usuario;
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Usuario usuario) {
        this.usuario = usuario;
        this.email = usuario.getEmail();
        this.authorities = usuario.getAuthorities();
    }

    public Usuario getUsuario() {
        return usuario;
    }

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
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(usuario.getActivo()) && Boolean.TRUE.equals(usuario.getEmailVerificado());
    }
}