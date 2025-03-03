package com.cut.cardona.security;

import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomAutenticationService implements UserDetailsService {

    @Autowired
    private RepositorioUsuario usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUserNameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + usernameOrEmail));

        return new CustomUserDetails(usuario);
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Usuario usuario) {
        return List.of(new SimpleGrantedAuthority(usuario.getRol().name()));
    }

}

    /*
        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            return repositorioUsuario.findByUserNameOrEmail(username, username);
        }*/

