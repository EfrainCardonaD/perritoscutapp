package com.cut.cardona.controllers;

import com.cut.cardona.api.controller.auth.RegistroController;
import com.cut.cardona.service.perfil.PerfilUsuarioService;
import com.cut.cardona.modelo.dto.perfil.DtoPerfilCompleto;
import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import com.cut.cardona.modelo.usuarios.Roles;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistroController.class)
public class RegistroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RepositorioUsuario repositorioUsuario;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private PerfilUsuarioService perfilUsuarioService;

    @Test
    public void testRegistroCompletoExitoso() throws Exception {
        // Mock del servicio
        DtoPerfilCompleto perfilMock = new DtoPerfilCompleto(
            "test-id", "testuser123", "test@example.com", Roles.ROLE_USER, true,
            new Timestamp(System.currentTimeMillis()), "perfil-id", "Usuario Test",
            "+5212345678901", "es", "America/Mexico_City", LocalDate.of(1990, 1, 1),
            true, "imagen-id", "http://example.com/imagen.jpg", "imagen.jpg",
            "image/jpeg", 1024L, new Timestamp(System.currentTimeMillis()),
            new Timestamp(System.currentTimeMillis())
        );

        when(perfilUsuarioService.registrarUsuarioExtendido(any())).thenReturn(perfilMock);
        when(repositorioUsuario.save(any(Usuario.class))).thenReturn(new Usuario());
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");

        // Simular archivo de imagen
        MockMultipartFile fotoPerfil = new MockMultipartFile(
            "fotoPerfil",
            "test-image.jpg",
            "image/jpeg",
            "test image content".getBytes()
        );

        mockMvc.perform(multipart("/api/registro")
                .file(fotoPerfil)
                .param("userName", "testuser123")
                .param("email", "test@example.com")
                .param("confirmEmail", "test@example.com")
                .param("password", "Password123!")
                .param("confirmPassword", "Password123!")
                .param("terms", "true")
                .param("nombreReal", "Usuario Test")
                .param("telefono", "+5212345678901")
                .param("idioma", "es")
                .param("zonaHoraria", "America/Mexico_City")
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado exitosamente"));
    }

    @Test
    public void testRegistroConEmailsNoCoinciden() throws Exception {
        mockMvc.perform(multipart("/api/registro")
                .param("userName", "testuser789")
                .param("email", "test@example.com")
                .param("confirmEmail", "different@example.com")
                .param("password", "Password123!")
                .param("confirmPassword", "Password123!")
                .param("terms", "true")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los correos electrónicos no coinciden"));
    }

    @Test
    public void testRegistroConPasswordsNoCoinciden() throws Exception {
        mockMvc.perform(multipart("/api/registro")
                .param("userName", "testuser999")
                .param("email", "test@example.com")
                .param("confirmEmail", "test@example.com")
                .param("password", "Password123!")
                .param("confirmPassword", "DifferentPassword!")
                .param("terms", "true")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Las contraseñas no coinciden"));
    }

    @Test
    public void testRegistroSinAceptarTerminos() throws Exception {
        mockMvc.perform(multipart("/api/registro")
                .param("userName", "testuser000")
                .param("email", "test@example.com")
                .param("confirmEmail", "test@example.com")
                .param("password", "Password123!")
                .param("confirmPassword", "Password123!")
                .param("terms", "false")
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Debe aceptar los términos y condiciones"));
    }
}
