package com.cut.cardona.api.controller.auth;

import com.cut.cardona.service.auth.PasswordRecoveryService;
import com.cut.cardona.modelo.dto.auth.ForgotPasswordRequest;
import com.cut.cardona.modelo.dto.auth.ResetPasswordRequest;
import com.cut.cardona.modelo.dto.common.RestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/forgot")
    public ResponseEntity<RestResponse<Void>> forgot(@Valid @RequestBody ForgotPasswordRequest body) {
        passwordRecoveryService.solicitarRecuperacion(body.email());
        return ResponseEntity.ok(RestResponse.success("Si el correo existe y está verificado, enviaremos un enlace de recuperación"));
    }

    @PostMapping("/reset")
    public ResponseEntity<RestResponse<Void>> reset(@Valid @RequestBody ResetPasswordRequest body) {
        passwordRecoveryService.resetearConToken(body.token(), body.nuevaPassword());
        return ResponseEntity.ok(RestResponse.success("Contraseña actualizada correctamente"));
    }
}

