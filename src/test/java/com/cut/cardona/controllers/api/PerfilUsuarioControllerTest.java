package com.cut.cardona.controllers.api;

import com.cut.cardona.api.controller.auth.RegistroController;
import com.cut.cardona.service.perfil.PerfilUsuarioService;
import com.cut.cardona.modelo.dto.perfil.DtoPerfilCompleto;
import com.cut.cardona.modelo.usuarios.Roles;
import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistroController.class)
@DisplayName("Tests para RegistroController - Registro y Perfil Consolidado")
class RegistroControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PerfilUsuarioService perfilUsuarioService;

    // Agregados: dependencias requeridas por el controlador
    @MockitoBean
    private RepositorioUsuario repositorioUsuario;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private DtoPerfilCompleto perfilCompletoMock;

    @BeforeEach
    void setUp() {
        perfilCompletoMock = new DtoPerfilCompleto(
            "user-123", "testuser", "test@example.com", Roles.ROLE_USER, true,
            new Timestamp(System.currentTimeMillis()), "perfil-456", "Juan Pérez",
            "+5215551234567", "es", "America/Mexico_City", LocalDate.of(1990, 5, 15),
            true, "imagen-789", "/api/imagenes/perfil/foto.jpg", "foto.jpg",
            "image/jpeg", 1024L, new Timestamp(System.currentTimeMillis()),
            new Timestamp(System.currentTimeMillis())
        );
    }

    @Test
    @DisplayName("Debe registrar usuario con perfil completo exitosamente")
    void debeRegistrarUsuarioConPerfilCompletoExitosamente() throws Exception {
        MockMultipartFile fotoPerfil = new MockMultipartFile(
            "fotoPerfil", "foto.jpg", "image/jpeg",
            "contenido de imagen".getBytes(StandardCharsets.UTF_8)
        );

        when(perfilUsuarioService.registrarUsuarioExtendido(any()))
            .thenReturn(perfilCompletoMock);

        // RUTA CORREGIDA: /api/registro en lugar de /api/perfil/registro-extendido
        mockMvc.perform(multipart("/api/registro")
                .file(fotoPerfil)
                .param("userName", "testuser")
                .param("email", "test@example.com")
                .param("confirmEmail", "test@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123")
                .param("terms", "true")
                .param("nombreReal", "Juan Pérez")
                .param("telefono", "+5215551234567")
                .param("idioma", "es")
                .param("zonaHoraria", "America/Mexico_City")
                .param("fechaNacimiento", "1990-05-15")
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado exitosamente"))
                .andExpect(jsonPath("$.usuario.userName").value("testuser"))
                .andExpect(jsonPath("$.usuario.email").value("test@example.com"));
    }

    @Test
    @DisplayName("Debe registrar usuario sin datos opcionales")
    void debeRegistrarUsuarioSinDatosOpcionales() throws Exception {
        when(perfilUsuarioService.registrarUsuarioExtendido(any()))
            .thenReturn(perfilCompletoMock);

        mockMvc.perform(multipart("/api/registro")
                .param("userName", "usuario123")
                .param("email", "usuario@example.com")
                .param("confirmEmail", "usuario@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123")
                .param("terms", "true")
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado exitosamente"));
    }

    @Test
    @WithMockUser(username = "user-123")
    @DisplayName("Debe obtener perfil completo del usuario autenticado")
    void debeObtenerPerfilCompletoDelUsuario() throws Exception {
        when(perfilUsuarioService.obtenerPerfilCompleto("user-123"))
            .thenReturn(Optional.of(perfilCompletoMock));

        mockMvc.perform(get("/api/perfil/completo").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(username = "user-123")
    @DisplayName("Debe actualizar imagen de perfil exitosamente")
    void debeActualizarImagenPerfilExitosamente() throws Exception {
        MockMultipartFile imagen = new MockMultipartFile(
            "imagen", "nueva-foto.jpg", "image/jpeg",
            "contenido de nueva imagen".getBytes(StandardCharsets.UTF_8)
        );

        when(perfilUsuarioService.actualizarImagenPerfil(eq("user-123"), any()))
            .thenReturn("/api/imagenes/perfil/nueva-foto.jpg");

        mockMvc.perform(multipart("/api/perfil/imagen")
                .file(imagen)
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Imagen de perfil actualizada exitosamente"));
    }

    @Test
    @DisplayName("Debe validar emails que no coinciden")
    void debeValidarEmailsQueNoCoinciden() throws Exception {
        mockMvc.perform(multipart("/api/registro")
                .param("userName", "testuser")
                .param("email", "test@example.com")
                .param("confirmEmail", "diferente@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123")
                .param("terms", "true")
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Los correos electrónicos no coinciden"));
    }
}
