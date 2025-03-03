package com.cut.cardona.security;

import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PasswordService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RepositorioUsuario repositorioUsuario;

    public boolean comprobarContrasenia(String username, String rawPassword) {
        // Aquí obtienes tu entidad Usuario en lugar de UserDetails
        Optional<Usuario> usuario = repositorioUsuario.findByUserNameOrEmail(username, username);
        // Compara la contraseña cruda con la encriptada en la base de datos
        return usuario.filter(value -> passwordEncoder.matches(rawPassword, value.getPassword())).isPresent();
    }
}
