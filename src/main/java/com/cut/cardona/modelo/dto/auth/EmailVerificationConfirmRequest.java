package com.cut.cardona.modelo.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationConfirmRequest(
        @NotBlank String token
) {}

