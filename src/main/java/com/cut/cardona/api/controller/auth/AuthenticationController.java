package com.cut.cardona.api.controller.auth;

import com.cut.cardona.service.auth.AuthenticationService;
import com.cut.cardona.modelo.dto.auth.AuthenticationRequest;
import com.cut.cardona.modelo.dto.auth.AuthenticationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody AuthenticationRequest request) {

        log.info("Attempting authentication for user: {}", request.userName());

        AuthenticationResponse response = authenticationService.authenticate(request);

        log.info("Authentication successful for user: {}", request.userName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            @RequestHeader("Authorization") String authHeader) {

        // Pasar el header completo al servicio (el test espera "Bearer ...")
        AuthenticationResponse response = authenticationService.refreshToken(authHeader);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        authenticationService.logout(token);
        return ResponseEntity.ok().build();
    }
}
