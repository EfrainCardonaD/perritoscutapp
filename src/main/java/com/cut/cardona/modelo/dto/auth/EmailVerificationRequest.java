package com.cut.cardona.modelo.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationRequest(
        @NotBlank @Email String email
) {}

