# Plan de Acción Frontend — Perritos Cut (fases alineadas con Backend)

Objetivo
- Mantener planes separados pero alineados fase a fase con Backend: F0↔F0, F1↔F1, F2↔F2, etc. Entregar: cimientos, catálogo público, autenticación con verificación de email y recuperación, y a futuro gestión de perros/adopciones y panel admin.

Mapa de alineación (estado actual)
- F0 Back ↔ F0 Front: Diagnóstico y Cimientos. [Completado]
- F1 Back ↔ F1 Front: Catálogo público (filtros/paginación). [Completado]
- F2 Back ↔ F2 Front: Autenticación + verificación email + recuperación. [Completado]
- F3 Back ↔ F3 Front: Mis Perros + Alta (Front en curso; Back preparando storage). [En curso]
- F4+ Back ↔ F4+ Front: Solicitudes/adopciones, admin, etc. [Planificado]

Supuestos y alcance
- Backend F1–F2 listo: catálogo con filtros/paginación; enums/estados; verificación de email y recuperación; roles USER/ADMIN/REVIEWER; refresh tokens operativo.
- Subida binaria y SMS OTP quedan fuera (planificadas a futuro). Por ahora, imágenes/documentos por URL.

Estado actual (front)
- Stack: Vue 3, Vite, Tailwind/Flowbite, Vue Router, Pinia, Axios.
- F0 completado: api.js central con interceptores, guards en router, Header con sesión y logout, .env.example con VITE_API_BASE_URL.

Fases (entregables y criterios)

F0 — Cimientos (infra y consistencia) [Completado]
Entregables
- Instancia Axios central (src/utils/api.js) usando VITE_API_BASE_URL.
- Interceptores: Authorization Bearer + refresh en 401 con exclusiones.
- Router guards: requiresAuth y roles en meta; redirecciones estándar.
- Navbar/Header con estado de sesión, logout y enlaces condicionales.
Criterios
- Todas las llamadas usan api.js; baseURL configurable vía .env; sesión persiste y se renueva.

F1 — Catálogo público (mapea a Backend F1) [Completado]
Entregables
- Catalogo.vue (ruta /perros) listando perros disponibles/aprobados.
- Filtros (sexo, tamaño, ubicación) y paginación simple.
- DogCard.vue (imagen principal, nombre, sexo/tamaño, ubicación).
Checklist de estado (F1 Front)
- [x] Vistas: Catalogo.vue + componentes DogCard.vue y Filters (o similar).
- [x] Ruta pública /perros con integración a GET /api/perros/catalogo.
- [x] Manejo de estados: carga, vacío, error; paginación y filtros básicos.
Criterios
- Listado rápido con loaders; vacío con mensaje útil; enlaces a adoptar.

F2 — Autenticación + verificación de email + recuperación (mapea a Backend F2) [Completado]
Entregables
- Login/Register integrados con feedback claro.
- Verificación de email: UI para request/confirm/resend.
- Forgot/Reset: formularios y llamada a endpoints.
Checklist de estado (F2 Front)
- [x] Vistas: auth/VerifyEmail.vue, auth/Forgot.vue, auth/Reset.vue.
- [x] Rutas públicas en router: /verify-email, /forgot, /reset.
- [x] Llamadas a /api/verify/email/*, /api/forgot, /api/reset usando api.js.
- [x] Mensajería de éxito/errores básica visible en UI.
- [x] Login/Register presentes (Login.vue, Register.vue) integrados a store.
Criterios
- Usuario no puede loguear hasta email_verificado=true y activo=true; mensajes adecuados.
- Forgot/Reset funciona end-to-end.

F3 — Alta y gestión básica de perros (usuario) [En curso]
Entregables
- NuevoPerro.vue (ruta protegida /perros/nuevo, ROLE_USER) con validaciones mínimas.
- MisPerros.vue (ruta protegida /perros/mios) con estado_revision/adopcion y visibilidad en catálogo.
- Store perros (catálogo, crear, listar míos).
Notas
- Backend ya expone /api/perros/mis y POST /api/perros; se puede adelantar tras F1 si se prioriza.
- Ver plan detallado: planes_de_accion/frontend/fases/FASE_3_PLAN_DE_IMPLEMENTACION_FRONTEND.md
Criterios
- Crear perro con ≥1 imagen y una principal; se refleja en “Mis perros”.

F4 — Solicitudes de adopción (usuario) [Planificado]
Entregables
- Acción “Adoptar” → POST /api/solicitudes.
- DocumentUploader: adjuntar 2 documentos (URL) a la solicitud.
- MisSolicitudes.vue (ruta protegida /solicitudes/mias).
Criterios
- Solicitud creada con feedback; documentos adjuntados; listado propio funcional.

F5 — Panel de administración (admin/reviewer) [Planificado]
Entregables
- AdminDashboard: Perros pendientes, Solicitudes pendientes.
- Acciones aprobar/rechazar perros; actualizar estado de solicitudes.
- Guards de rol (ROLE_ADMIN o ROLE_REVIEWER).
Criterios
- Acciones mutan estado y refrescan listas; acceso restringido.

F6 — Perfil y UX base
Entregables
- Perfil (datos + imagen por URL) integrado a endpoints.
- Sistema de Alert/Toast unificado; loaders/skeletons.
- Documentación .env y README del front actualizados.
Criterios
- UX consistente; perfil editable y persistente.

F7 — QA mínima y humo E2E
Entregables
- Smoke (Playwright/Cypress): login, catálogo, crear perro, crear solicitud.
- Scripts npm para smoke local.
Criterios
- Smoke verde en local; rutas clave sanas.

Criterios de aceptación por fase (resumen)
- F0: api.js + guards operativos; baseURL por .env. [Hecho]
- F1: catálogo con filtros/paginación simple y loaders. [Hecho]
- F2: verificación email y reset funcionales; bloqueo de login si no verificado. [Hecho]
- F3: Mis Perros + Alta en curso (rutas, store y formulario implementados; pendiente pulidos). [En curso]
- F4+: sujetos a backend y priorización. [Planificado]

Riesgos y mitigaciones
- Divergencia de contratos: aislar en api.js y adaptar payloads.
- CORS/ambientes: usar .env y coordinar allowed-origins.
- Estados/roles: probar con usuarios de distintos roles.
- Subida de archivos: por URL ahora; migrar a pre-signed URLs después.

Siguientes pasos inmediatos
- Finalizar F3 Front (lista/creación y UX) y coordinar con Back F3 (storage) y F4 (migraciones) para siguientes iteraciones.
