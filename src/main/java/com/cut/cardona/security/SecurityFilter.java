package com.cut.cardona.security;
import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class SecurityFilter extends OncePerRequestFilter {


    @Autowired
    private TokenService tokenService;

    @Autowired
    private RepositorioUsuario repositorioUsuario;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //Obtener el token del header
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null) {
            String token = authHeader.replace("Bearer ", "");
            String subject = tokenService.getSubject(token);

            if (subject != null){
                UserDetails usuario = repositorioUsuario.findByUserName(subject);
                var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

            /*if (subject != null){

                Optional<Usuario> usuario = Optional.ofNullable(repositorioUsuario.findByUserNameOrEmail(subject, subject).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + subject)));
                if (usuario.isPresent()){
                    var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.get().getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            }*/
        }


        filterChain.doFilter(request, response);

    }
}
