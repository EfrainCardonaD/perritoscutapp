package com.cut.cardona.controllers.api;

import com.cut.cardona.controllers.service.RegistroService;
import com.cut.cardona.controllers.service.PerfilUsuarioService;
import com.cut.cardona.modelo.dto.registro.DtoRegistroUsuario;
import com.cut.cardona.modelo.usuarios.RepositorioUsuario;
import com.cut.cardona.modelo.usuarios.Usuario;
import com.cut.cardona.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistroController.class)
@DisplayName("Tests para RegistroController")
class RegistroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistroService registroService;

    @MockitoBean
    private PerfilUsuarioService perfilUsuarioService;

    @MockitoBean
    private TokenService tokenService;

    // Mantenemos estos mocks si otros tests los usan indirectamente
    @MockitoBean
    private RepositorioUsuario repositorioUsuario;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn("password-encoded");
    }

    @Test
    @DisplayName("Debe mostrar formulario de registro")
    void debeMostrarFormularioRegistro() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeExists("registroUsuario"))
                .andExpect(model().attribute("registroUsuario",
                    org.hamcrest.Matchers.instanceOf(DtoRegistroUsuario.class)));
    }

    @Test
    @DisplayName("Debe registrar usuario exitosamente con datos válidos")
    void debeRegistrarUsuarioExitosamente() throws Exception {
        // Given
        Usuario usuarioMock = mock(Usuario.class);
        when(usuarioMock.getUsername()).thenReturn("testuser");
        when(registroService.crearUsuarioBasico(any(DtoRegistroUsuario.class))).thenReturn(usuarioMock);

        // When & Then
        mockMvc.perform(post("/registro")
                .param("userName", "testuser")
                .param("email", "test@example.com")
                .param("confirmEmail", "test@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123")
                .param("terms", "true")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("successfulRegistroUsuario"));

        verify(registroService, times(1)).crearUsuarioBasico(any(DtoRegistroUsuario.class));
        verifyNoInteractions(repositorioUsuario); // ahora no se usa directamente en el controlador
    }

    @Test
    @DisplayName("Debe rechazar registro con emails que no coinciden")
    void debeRechazarRegistroConEmailsNoCoinciden() throws Exception {
        mockMvc.perform(post("/registro")
                .param("userName", "testuser")
                .param("email", "test@example.com")
                .param("confirmEmail", "diferente@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123")
                .param("terms", "true")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("registroUsuario", "confirmEmail"));

        verify(registroService, never()).crearUsuarioBasico(any(DtoRegistroUsuario.class));
    }

    @Test
    @DisplayName("Debe rechazar registro con contraseñas que no coinciden")
    void debeRechazarRegistroConPasswordsNoCoinciden() throws Exception {
        mockMvc.perform(post("/registro")
                .param("userName", "testuser")
                .param("email", "test@example.com")
                .param("confirmEmail", "test@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password456")
                .param("terms", "true")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("registroUsuario", "confirmPassword"));

        verify(registroService, never()).crearUsuarioBasico(any(DtoRegistroUsuario.class));
    }

    @Test
    @DisplayName("Debe rechazar registro con nombre de usuario inválido")
    void debeRechazarRegistroConUserNameInvalido() throws Exception {
        mockMvc.perform(post("/registro")
                .param("userName", "test-user!")
                .param("email", "test@example.com")
                .param("confirmEmail", "test@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123")
                .param("terms", "true")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("registroUsuario", "userName"));

        verify(registroService, never()).crearUsuarioBasico(any(DtoRegistroUsuario.class));
    }

    @Test
    @DisplayName("Debe manejar error de integridad de datos (usuario duplicado)")
    void debeManejarErrorIntegridadDatos() throws Exception {
        // Given
        when(registroService.crearUsuarioBasico(any(DtoRegistroUsuario.class)))
            .thenThrow(new DataIntegrityViolationException("Usuario ya existe"));

        // When & Then
        mockMvc.perform(post("/registro")
                .param("userName", "testuser")
                .param("email", "test@example.com")
                .param("confirmEmail", "test@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123")
                .param("terms", "true")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registro"));

        verify(registroService, times(1)).crearUsuarioBasico(any(DtoRegistroUsuario.class));
    }

    @Test
    @DisplayName("Debe manejar errores genéricos durante el registro")
    void debeManejarErroresGenericos() throws Exception {
        // Given
        when(registroService.crearUsuarioBasico(any(DtoRegistroUsuario.class)))
            .thenThrow(new RuntimeException("Error de base de datos"));

        // When & Then
        mockMvc.perform(post("/registro")
                .param("userName", "testuser")
                .param("email", "test@example.com")
                .param("confirmEmail", "test@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123")
                .param("terms", "true")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registro"));

        verify(registroService, times(1)).crearUsuarioBasico(any(DtoRegistroUsuario.class));
    }

    @Test
    @DisplayName("Debe validar campos requeridos")
    void debeValidarCamposRequeridos() throws Exception {
        mockMvc.perform(post("/registro")
                .param("userName", "")
                .param("email", "")
                .param("confirmEmail", "")
                .param("password", "")
                .param("confirmPassword", "")
                .param("terms", "false")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().hasErrors());

        verifyNoInteractions(registroService);
    }

    @Test
    @DisplayName("Debe aceptar nombre de usuario válido (solo alfanumérico)")
    void debeAceptarUserNameValido() throws Exception {
        // Given
        Usuario usuarioMock = mock(Usuario.class);
        when(usuarioMock.getUsername()).thenReturn("testuser123");
        when(registroService.crearUsuarioBasico(any(DtoRegistroUsuario.class))).thenReturn(usuarioMock);

        // When & Then
        mockMvc.perform(post("/registro")
                .param("userName", "testuser123")
                .param("email", "test@example.com")
                .param("confirmEmail", "test@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123")
                .param("terms", "true")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(registroService, times(1)).crearUsuarioBasico(any(DtoRegistroUsuario.class));
    }

    @Test
    @DisplayName("Debe rechazar términos no aceptados")
    void debeRechazarTerminosNoAceptados() throws Exception {
        mockMvc.perform(post("/registro")
                .param("userName", "testuser")
                .param("email", "test@example.com")
                .param("confirmEmail", "test@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123")
                .param("terms", "false")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().hasErrors());

        verifyNoInteractions(registroService);
    }

    @Test
    @DisplayName("Debe validar formato de email básico")
    void debeValidarFormatoEmailBasico() throws Exception {
        // Given
        Usuario usuarioMock = mock(Usuario.class);
        when(usuarioMock.getUsername()).thenReturn("testuser");
        when(registroService.crearUsuarioBasico(any(DtoRegistroUsuario.class))).thenReturn(usuarioMock);

        // When & Then
        mockMvc.perform(post("/registro")
                .param("userName", "testuser")
                .param("email", "test@example.com")
                .param("confirmEmail", "test@example.com")
                .param("password", "password123")
                .param("confirmPassword", "password123")
                .param("terms", "true")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(registroService, times(1)).crearUsuarioBasico(any(DtoRegistroUsuario.class));
    }
}
