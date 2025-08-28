package com.cut.cardona.controllers.api;

import com.cut.cardona.modelo.dto.auth.AuthenticationRequest;
import com.cut.cardona.modelo.dto.auth.AuthenticationResponse;
import com.cut.cardona.modelo.dto.usuarios.DtoUsuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import com.cut.cardona.controllers.service.AuthenticationService;
import com.cut.cardona.security.SecurityFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthenticationController.class)
@DisplayName("Tests para AuthenticationController")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    // Evitar inicializar el filtro real (que depende de TokenService)
    @MockitoBean
    private SecurityFilter securityFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthenticationRequest validRequest;
    private AuthenticationResponse validResponse;

    @BeforeEach
    void setUp() {
        validRequest = new AuthenticationRequest("testuser", "password123");

        // Crear DtoUsuario mock
        DtoUsuario usuarioDto = new DtoUsuario("testuser", "test@example.com", "USER");

        // AuthenticationResponse solo espera 4 parámetros: token, refreshToken, usuario, expiresIn
        validResponse = new AuthenticationResponse(
            "jwt-access-token",
            "jwt-refresh-token",
            usuarioDto,
            3600L
        );
    }

    @Test
    @DisplayName("Debe autenticar usuario con credenciales válidas")
    void debeAutenticarUsuarioConCredencialesValidas() throws Exception {
        // Given
        when(authenticationService.authenticate(any(AuthenticationRequest.class)))
            .thenReturn(validResponse);

        // When & Then
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("jwt-refresh-token"))
                .andExpect(jsonPath("$.usuario.userName").value("testuser"))
                .andExpect(jsonPath("$.usuario.rol").value("USER"))
                .andExpect(jsonPath("$.expiresIn").value(3600));

        verify(authenticationService, times(1)).authenticate(any(AuthenticationRequest.class));
    }

    @Test
    @DisplayName("Debe rechazar login con credenciales inválidas")
    void debeRechazarLoginConCredencialesInvalidas() throws Exception {
        // Given
        when(authenticationService.authenticate(any(AuthenticationRequest.class)))
            .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        // When & Then
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest))
                .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(authenticationService, times(1)).authenticate(any(AuthenticationRequest.class));
    }

    @Test
    @DisplayName("Debe validar formato JSON en login")
    void debeValidarFormatoJsonEnLogin() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invalid\": \"json\"}")
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).authenticate(any(AuthenticationRequest.class));
    }

    @Test
    @DisplayName("Debe rechazar login con campos vacíos")
    void debeRechazarLoginConCamposVacios() throws Exception {
        // Given
        AuthenticationRequest requestVacio = new AuthenticationRequest("", "");

        // When & Then
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestVacio))
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).authenticate(any(AuthenticationRequest.class));
    }

    @Test
    @DisplayName("Debe rechazar login con userName nulo")
    void debeRechazarLoginConUserNameNulo() throws Exception {
        // Given
        String requestJson = "{\"userName\": null, \"password\": \"password123\"}";

        // When & Then
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).authenticate(any(AuthenticationRequest.class));
    }

    @Test
    @DisplayName("Debe refreshar token exitosamente")
    void debeRefresharTokenExitosamente() throws Exception {
        // Given
        String refreshToken = "Bearer valid-refresh-token";
        DtoUsuario usuarioDto = new DtoUsuario("testuser", "test@example.com", "USER");

        AuthenticationResponse refreshResponse = new AuthenticationResponse(
            "new-access-token",
            "new-refresh-token",
            usuarioDto,
            3600L
        );

        when(authenticationService.refreshToken(refreshToken))
            .thenReturn(refreshResponse);

        // When & Then
        mockMvc.perform(post("/api/refresh")
                .header("Authorization", refreshToken)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.usuario.userName").value("testuser"));

        verify(authenticationService, times(1)).refreshToken(refreshToken);
    }

    @Test
    @DisplayName("Debe rechazar refresh con token inválido")
    void debeRechazarRefreshConTokenInvalido() throws Exception {
        // Given
        String invalidToken = "Bearer invalid-token";
        when(authenticationService.refreshToken(invalidToken))
            .thenThrow(new IllegalArgumentException("Token inválido"));

        // When & Then
        mockMvc.perform(post("/api/refresh")
                .header("Authorization", invalidToken)
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authenticationService, times(1)).refreshToken(invalidToken);
    }

    @Test
    @DisplayName("Debe rechazar refresh sin header Authorization")
    void debeRechazarRefreshSinHeaderAuthorization() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/refresh")
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).refreshToken(anyString());
    }

    @Test
    @DisplayName("Debe hacer logout exitosamente")
    void debeHacerLogoutExitosamente() throws Exception {
        // Given
        String token = "Bearer valid-access-token";
        doNothing().when(authenticationService).logout(token);

        // When & Then
        mockMvc.perform(post("/api/logout")
                .header("Authorization", token)
                .with(csrf()))
                .andExpect(status().isOk());

        verify(authenticationService, times(1)).logout(token);
    }

    @Test
    @DisplayName("Debe manejar error en logout")
    void debeManejarErrorEnLogout() throws Exception {
        // Given
        String token = "Bearer invalid-token";
        doThrow(new IllegalArgumentException("Token inválido"))
            .when(authenticationService).logout(token);

        // When & Then
        mockMvc.perform(post("/api/logout")
                .header("Authorization", token)
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authenticationService, times(1)).logout(token);
    }

    @Test
    @DisplayName("Debe rechazar logout sin header Authorization")
    void debeRechazarLogoutSinHeaderAuthorization() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/logout")
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).logout(anyString());
    }

    @Test
    @DisplayName("Debe manejar Content-Type incorrecto en login")
    void debeManejarContentTypeIncorrectoEnLogin() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.TEXT_PLAIN)
                .content(objectMapper.writeValueAsString(validRequest))
                .with(csrf()))
                .andExpect(status().isUnsupportedMediaType());

        verify(authenticationService, never()).authenticate(any(AuthenticationRequest.class));
    }

    @Test
    @DisplayName("Debe validar longitud mínima de password")
    void debeValidarLongitudMinimaDePassword() throws Exception {
        // Given
        AuthenticationRequest requestPasswordCorto = new AuthenticationRequest("testuser", "123");

        // When & Then
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestPasswordCorto))
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(authenticationService, never()).authenticate(any(AuthenticationRequest.class));
    }
}
