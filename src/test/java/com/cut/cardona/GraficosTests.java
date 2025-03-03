package com.cut.cardona;

import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

@SpringBootTest
class GraficosTests {
    @Autowired
    private RepositorioUsuario repositorioUsuario;

    @Test
    void contextLoads() {
    }

    @Test
    void testMain() {
        Usuario usuario = new Usuario();
        usuario.setUserName("testUser");
        usuario.setEmail("testUser@example.com");
        usuario.setPassword("password");

        // Save the user (assuming save method exists)
        repositorioUsuario.save(usuario);

        // Retrieve the user (assuming findByUserName method exists)
        Optional<Usuario> testUser = repositorioUsuario.findByUserNameOrEmail("testUser", "testUser@example.com");
        if (testUser.isPresent()) {
            System.out.println("Retrieved User: " + testUser.get().getEmail());
        } else {
            System.out.println("User not found");
        }
        System.out.println("Retrieved User: " + testUser.get().getUsername());
    }

}
