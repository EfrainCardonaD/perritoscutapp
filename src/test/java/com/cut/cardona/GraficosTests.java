package com.cut.cardona;

import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

@SpringBootTest
@SpringJUnitConfig
@RequiredArgsConstructor // ✅ Constructor injection incluso en tests
class PerritoscutappTests {

    // ✅ Constructor injection - mejor práctica incluso en tests
    private final RepositorioUsuario repositorioUsuario;

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
