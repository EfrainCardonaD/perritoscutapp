package com.cut.cardona.controllers.api;

import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImagenController.class)
class ImagenControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    TokenService tokenService;

    RepositorioUsuario repositorioUsuario;

    private final Path uploadDir = Paths.get("uploads/perritos");

    @BeforeEach
    void setUp() throws Exception {
        Files.createDirectories(uploadDir);
        Path file = uploadDir.resolve("2.png");
        if (!Files.exists(file)) {
            // Contenido PNG mínimo (no válido real, pero suficiente para servir bytes)
            byte[] content = new byte[] {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
            Files.write(file, content);
        }
    }

    @Test
    void getImagenPerro_sinAuth_retorna401() throws Exception {
        mockMvc.perform(get("/api/imagenes/perritos/2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void headImagenPerro_sinAuth_retorna401() throws Exception {
        mockMvc.perform(head("/api/imagenes/perritos/2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getImagenPerro_conAuth_retorna200() throws Exception {
        mockMvc.perform(get("/api/imagenes/perritos/2"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void headImagenPerro_conAuth_retorna200() throws Exception {
        mockMvc.perform(head("/api/imagenes/perritos/2"))
                .andExpect(status().isOk());
    }
}
