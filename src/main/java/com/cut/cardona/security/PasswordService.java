package com.cut.cardona.security;

import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor // ✅ Constructor injection automático
public class PasswordService {

    // ✅ Constructor injection - inmutable y testeable
    private final PasswordEncoder passwordEncoder;
    private final RepositorioUsuario repositorioUsuario;

    public boolean comprobarContrasenia(String username, String rawPassword) {
        // Aquí obtienes tu entidad Usuario en lugar de UserDetails
        Optional<Usuario> usuario = repositorioUsuario.findByUserNameOrEmail(username, username);
        // Compara la contraseña cruda con la encriptada en la base de datos
        return usuario.filter(value -> passwordEncoder.matches(rawPassword, value.getPassword())).isPresent();
    }
}
