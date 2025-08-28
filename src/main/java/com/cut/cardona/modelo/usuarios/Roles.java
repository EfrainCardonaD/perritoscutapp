package com.cut.cardona.modelo.usuarios;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public enum Roles {
    ROLE_ADMIN,
    ROLE_USER,
    ROLE_REVIEWER;

    public List<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(this.name()));
    }

}
