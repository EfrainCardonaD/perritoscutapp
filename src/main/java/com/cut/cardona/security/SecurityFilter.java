package com.cut.cardona.security;

import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor // ✅ Constructor injection automático
public class SecurityFilter extends OncePerRequestFilter {

    // ✅ Constructor injection - inmutable y testeable
    private final TokenService tokenService;
    private final RepositorioUsuario repositorioUsuario;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //Obtener el token del header
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null) {
            String token = authHeader.replace("Bearer ", "");
            String subject = tokenService.getSubject(token);

            if (subject != null){
                // ✅ Usar el código corregido que estaba comentado
                Optional<Usuario> usuario = repositorioUsuario.findByUserNameOrEmail(subject, subject);
                if (usuario.isPresent()){
                    CustomUserDetails userDetails = new CustomUserDetails(usuario.get());
                    var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }


        filterChain.doFilter(request, response);

    }
}
