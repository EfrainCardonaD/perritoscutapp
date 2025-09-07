# Plan de Implementación — Fase 2 (Seguridad avanzada y recuperación de contraseña)

Objetivo
- Completar el flujo de autenticación seguro con refresh tokens y recuperación de contraseña.
- Implantar verificación de contacto previa a completar el registro para garantizar canales confiables de recuperación.

Nota de alcance (MVP)
- Esta iteración implementa verificación por EMAIL únicamente.
- Verificación por SMS (teléfono) queda PENDIENTE (Fase 2B) y se documenta el plan para su posterior implementación.

Alcance
- Backend (Spring Boot): endpoints de auth (login/refresh/logout), recuperación (forgot/reset), verificación de email (enlace). (SMS/OTP pendiente)
- Base de datos (Flyway): tablas para tokens de recuperación y verificación; columnas de estado de verificación en usuarios; unicidades/índices y limpieza (estructura preparada para SMS pero sin lógica activa).
- Seguridad: rutas públicas adicionales, rate limit en endpoints sensibles.
- Documentación y pruebas: OpenAPI, Insomnia, unit/integration (email); casos SMS quedan planificados.

Estado actual (confirmado en repo)
- Hecho:
  - Endpoints /api/login, /api/refresh, /api/logout en AuthenticationController.
  - Verificación de email: VerificationController con /api/verify/email/request, /confirm, /resend.
  - Recuperación: RecoveryController con /api/forgot y /api/reset.
  - Flyway V7 creado: columnas en usuarios (activo, email_verificado) y tablas verification_tokens y tokens_reset.
  - Entidades y servicios: VerificationToken, ResetToken; VerificationService y PasswordRecoveryService.
  - Seguridad: SecurityConfiguration permite verify/forgot/reset y refresh; RateLimitFilter registrado.
  - MailService con enlaces a front (app.mail.enabled para modo log en dev).
  - Usuario: bloquea login hasta activo=true y emailVerificado=true.
- Pendiente:
  - OpenAPI/Insomnia: validar y actualizar ejemplos y contratos.
  - Tests unitarios/integración específicos de verificación y recuperación.
  - SMS/OTP (Fase 2B): no incluido.

Checklist (F2 — Email MVP)
- [x] Flyway V7: columnas usuarios + tablas verification_tokens y tokens_reset.
- [x] Entidades/Repos: VerificationToken, ResetToken.
- [x] Servicios: VerificationService (email), PasswordRecoveryService (email).
- [x] Controladores: VerificationController y RecoveryController; ajustes en Registro/Authentication para activo/verificado.
- [x] SecurityConfiguration: verify/forgot/reset permitAll; /api/refresh permitido; RateLimitFilter activo.
- [x] Bean Validation: email normalizado/validado donde aplica.
- [x] SMTP adapter (MailService) con logging en dev.
- [ ] OpenAPI & Insomnia actualizados (verify/forgot/reset).
- [ ] Tests unitarios e integración para verificación y recuperación.

Pendiente (Fase 2B — SMS)
- [ ] Activar teléfono/telefono_verificado; UNIQUE(telefono) si aplica
- [ ] VerificationService: enviarOTP/confirmarOTP y rate limit específico
- [ ] Endpoints /api/verify/phone/* y reset por OTP
- [ ] Validación E.164 y normalización
- [ ] Integración con proveedor SMS (o mock)
- [ ] Tests para OTP/SMS

Subplan — Verificación (email ahora, SMS pendiente)
- Política: se requiere al menos UN canal verificado para activar el usuario.
- Flujo (email – incluido):
  1) Registro con email → crear usuario activo=false, email_verificado=false
  2) Enviar enlace de verificación (token)
  3) POST /api/verify/email/confirm { token }
  4) Marcar email_verificado=true y activo=true → login permitido
- Flujo (teléfono – 2B):
  1) Generar OTP 6-dígitos (10 min)
  2) POST /api/verify/phone/confirm { telefono, otp }
  3) Marcar telefono_verificado=true y activar usuario si corresponde
- Reintentos/seguridad: máx 3/h reenvío; invalidar tokens previos; bloquear tras fallos repetidos.
