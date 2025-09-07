# Fase 0 — Cimientos Frontend (Vue 3 + Vite + Tailwind)

Objetivo
- Estabilizar la base del frontend con configuración de API por entorno, autenticación robusta (Bearer + refresh), guards de rutas por rol, y navegación/UI mínima de sesión.

Resultado esperado
- Todas las llamadas HTTP pasan por una instancia central de Axios parametrizada por .env.
- Sesiones persistentes y renovación transparente del access token.
- Rutas protegidas y de administrador accesibles solo con los roles adecuados.
- Navbar/Header con estado de sesión y enlaces condicionales.

Alcance y supuestos
- Backend F1–F2 en marcha: login/refresh, verificación email, recuperación, roles (USER/ADMIN/REVIEWER).
- .env.example ya contiene VITE_API_BASE_URL (verificado en front/.env.example).
- Estructura actual: src/router/index.js, src/stores/auth.js, src/utils/, src/navbar.js.

Entregables (estado)
1) Instancia Axios central: src/utils/api.js. [Hecho]
2) Interceptores: Authorization (request) y refresh en 401 (response) con exclusiones y single-flight. [Hecho]
3) Guards en router: requiresAuth y roles con beforeEach + limpieza si expiró. [Hecho]
4) Navbar/Header: integra Pinia para sesión y logout. [Hecho]
5) Documentación breve en README/.env.example. [No requerido en esta iteración]

Notas de implementación (delta aplicado)
- api.js: se agregó single-flight (cola de refresh) y exclusiones ampliadas: /verify/email/*, /forgot, /reset.
- router: se limpia sesión si token expiró antes de evaluar requiresAuth; se mantiene redirect con query redirect.
- Header.vue: ahora usa useAuthStore para isAuthenticated, usuario y logout; ya no lee localStorage directo.
- main.js: comprobación periódica de expiración cada 60s llamando a auth.checkTokenExpiration().

Contratos (mini)
- login POST /login → guarda tokens/user/expiresIn (ms) y navega.
- refresh POST /refresh con Authorization: Bearer <refreshToken> → actualiza tokens; reintenta 1 vez la request original.
- logout POST /logout (best effort) → limpia sesión y redirige a /login.

Edge cases cubiertos
- 401 concurrentes: single-flight de refresh y reintento único.
- Refresh inválido/expirado: logout y redirect.
- Token expirado en cliente: limpieza anticipada en guard.
- Falta de baseURL: fallback a window.location.origin + /api.

Criterios de aceptación (validado)
- Llamadas usan api.js con Authorization cuando procede. [OK]
- 401 con refresh válido recupera; con refresh inválido hace logout. [OK]
- Rutas protegidas y admin con rol funcionan. [OK]
- Navbar refleja sesión y permite logout. [OK]
- Sin IPs hardcodeadas; .env controla baseURL. [OK]

Pruebas realizadas (manual)
- Login correcto/incorrecto con mensajes. [OK]
- Acceso a ruta protegida sin sesión redirige a /login. [OK]
- Simulación de expiración → refresh y posterior logout al fallar refresh. [OK]
- Intento /admin sin rol adecuado bloquea. [OK]

Riesgos y mitigaciones
- Contratos backend pueden variar: se aísla en api.js.
- Race conditions de refresh: single-flight implementado.
- CORS: coordinar allowed-origins con el backend.

Rollback
- No necesario; cambios compatibles y encapsulados.

Checklist (PR F0)
- [x] src/utils/api.js con interceptores, exclusiones y single-flight.
- [x] src/stores/auth.js usando api.js (sin cambios de contrato público).
- [x] src/router/index.js con limpieza por expiración y guards.
- [x] Header con Pinia; logout vía store.
- [x] Validaciones manuales pasadas.

Estado final F0: Completado.
