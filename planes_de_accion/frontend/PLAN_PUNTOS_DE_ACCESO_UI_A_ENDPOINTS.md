# Plan de Puntos de Acceso UI → Endpoints (Frontend)

Objetivo
- Definir desde qué lugares de la interfaz (navbar, dropdowns, vistas y CTAs) se accede a cada endpoint del backend, qué store/servicio lo consume y cuál es el flujo de navegación esperado.

Convenciones
- Todas las llamadas HTTP pasan por src/utils/api.js (interceptores + refresh).
- Acciones de negocio viven en stores Pinia (auth, perros, …) o servicios específicos.
- Rutas protegidas usan meta.requiresAuth y meta.roles cuando aplique.

Mapa UI → Endpoint → Store/Servicio → Ruta

1) Autenticación
- Login
  - UI: Ruta pública /login con formulario.
  - Endpoint: POST /api/login; POST /api/refresh; POST /api/logout
  - Store: useAuthStore.login(), .refreshAccessToken(), .logout()
  - Navegación: tras login → redirect query o “/”. Logout en dropdown usuario.
- Registro
  - UI: /register
  - Endpoint: POST /api/registro (y opcional /api/registro-completo)
  - Store: useAuthStore.register(), .registerWithImage()
  - Navegación: tras registro mostrar aviso y CTA a verificación de correo.

2) Verificación y recuperación
- Verificación de email
  - UI: /verify-email; banner en Header si usuario no verificado.
  - Endpoints: POST /api/verify/email/request, /api/verify/email/confirm, /api/verify/resend
  - Servicio: llamadas directas con api.js desde VerifyEmail.vue o un pequeño helper verifyService.
  - Navegación: desde banner/CTA y desde post-registro.
- Recuperación de contraseña
  - UI: /forgot (enlace desde Login); /reset (desde email)
  - Endpoints: POST /api/forgot, POST /api/reset
  - Servicio: llamadas con api.js desde Forgot.vue/Reset.vue.

3) Catálogo y detalle (público)
- Catálogo
  - UI: Navbar “Perros” visible para todos → /perros (hacer público)
  - Endpoint: GET /api/perros/catalogo (filtros/paginación)
  - Servicio/Store: puede usarse llamada directa desde Catalogo.vue o crear store catalogo si se complejiza.
  - Navegación: accesible desde Header; tarjetas con botón “Adoptar” (pendiente F4 backend).
- Detalle (futuro)
  - UI: /perros/:id (Planificado)
  - Endpoints: GET /api/perros/:id (cuando exista)

4) Mis Perros y Alta (usuario)
- Mis Perros
  - UI: /perros/mios; accesos: dropdown usuario “Mis Perros” + CTA tras crear.
  - Endpoint: GET /api/perros/mis
  - Store: usePerrosStore.fetchMy()
- Nuevo Perro
  - UI: /perros/nuevo; accesos: dropdown usuario “Nuevo Perro” + botón en Mis Perros vacío.
  - Endpoint: POST /api/perros
  - Store: usePerrosStore.create(payload mapeado a CrearPerroRequest)
  - Validaciones: ≥1 imagen y exactamente una principal antes de enviar.

5) Imágenes (carga y render)
- Subida imagen de perro
  - UI: Componente DogImagesEditor dentro de NuevoPerro.
  - Endpoint: POST /api/imagenes/perritos → devuelve { id, url }
  - Servicio: api.js directo en el componente; preview con GET /api/imagenes/perritos/{id} (blob)
- Render imagen principal
  - UI: DogCard.vue en Catálogo/Mis Perros
  - Endpoint: GET /api/imagenes/perritos/{id} (blob)

6) Administración (planificado)
- Panel revisor/admin
  - UI: /admin (visible solo para ROLE_ADMIN/ROLE_REVIEWER)
  - Endpoints: POST /api/admin/perros/{id}/aprobar, /rechazar, /estado?estado=...
  - Store/Servicio: adminPerrosStore con acciones aprobar/rechazar/cambiarEstado.
  - Acceso: item “Admin” en dropdown si rol cumple.

7) Perfil (planificado)
- UI: /perfil (edición y avatar)
- Endpoints: a definir (GET/PUT /api/usuarios/me, carga imagen perfil)
- Acceso: dropdown usuario “Mi perfil”.

Cambios UI concretos
- Header.vue
  - Mostrar “Perros” para todos (catálogo público) → router: /perros sin requiresAuth.
  - Dropdown usuario (si logueado): añadir “Mis Perros” → /perros/mios y “Nuevo Perro” → /perros/nuevo.
  - Banner condicional (top o bajo navbar) si usuario no verificado con enlace a /verify-email.
- Catalogo.vue
  - Asegurar estados: carga/vacío/error; botón “Adoptar” (deshabilitado hasta F4); CTA “Publicar perro” si logueado (lleva a /perros/nuevo).
- Login.vue
  - Enlace “¿Olvidaste tu contraseña?” → /forgot.
- Register.vue
  - Tras éxito: mensaje con enlace a /verify-email.
- MisPerros.vue
  - Vacío: CTA “Crea tu primer perro” → /perros/nuevo.

Guardas y roles
- /perros: público (quitar requiresAuth en router meta) → Header visible siempre.
- /perros/mios y /perros/nuevo: requiresAuth + roles: ['USER'] (ya configurado).
- Admin (/admin/*): requiresAuth + roles: ['ADMIN','REVIEWER'] (cuando exista).

Contratos y stores sugeridos
- authStore (existente): login, logout, refresh, register, registerWithImage.
- perrosStore (existente): fetchMy, create.
- verifyService (opcional): request, confirm, resend de verificación.
- catalogService (opcional): listar catálogo con filtros/paginación.

Checklist de tareas
1) Router
- [ ] Hacer pública la ruta /perros (quitar meta.requiresAuth)
2) Header
- [ ] Mostrar link “Perros” siempre.
- [ ] Añadir items dropdown: “Mis Perros” y “Nuevo Perro”.
- [ ] Mostrar banner si email no verificado con CTA a /verify-email.
3) Login/Register
- [ ] Agregar enlace a /forgot en Login.
- [ ] Post-registro: mensaje con CTA a /verify-email.
4) Catalogo
- [ ] Botón “Adoptar” deshabilitado (copy explicativo) hasta F4; CTA “Publicar perro” si logueado.
5) Mis Perros/Nuevo Perro
- [ ] Validaciones front y manejo de errores uniformes (toasts/alerts).
6) Admin (Planificado)
- [ ] Menú y rutas protegidas; acciones a endpoints admin.

Criterios de aceptación
- Navegación clara a cada flujo; endpoints invocados como se espera (200/4xx manejados).
- /perros accesible sin sesión; “Mis Perros”/“Nuevo Perro” visibles solo si autenticado.
- Estados de UI (carga/vacío/error) presentes en catálogo y mis perros.

Notas
- Ajustar i18n/microcopy y accesibilidad (aria/roles) en botones/banners.
- Reforzar manejo de 401/403/422 con mensajes amigables en stores/servicios.

