# PerritosCutApp — Documentación General del Proyecto

## 1. Introducción

PerritosCutApp es una plataforma web para gestionar de forma centralizada información de perros vinculados a procesos de adopción. El objetivo es hacer que la adopción sea más sencilla y controlada, permitiendo a las organizaciones:

- Publicar perros en un catálogo público con filtros.
- Llevar un control de usuarios que adoptan o publican perros.
- Revisar y validar la información antes de hacerla visible.
- Gestionar el ciclo de vida de solicitudes de adopción y documentación asociada.

El sistema está dividido en dos grandes partes:

- Un **backend** en Java/Spring Boot (carpeta raíz del proyecto).
- Un **frontend** en Vue 3 + Vite (carpeta `front/`).

Ambos se comunican mediante una API REST protegida por JWT.

---

## 2. Propósito y objetivos

El propósito principal de PerritosCutApp es proporcionar una solución replicable y escalable para organizaciones que gestionan adopciones de perros.

Objetivos concretos:

1. **Registro y gestión de perros**
   - Crear, actualizar y listar perros asociados a un usuario.
   - Gestionar estados de revisión y de adopción.
   - Asociar imágenes a cada perro.

2. **Catálogo público de perros**
   - Mostrar un listado público de perros disponibles.
   - Permitir filtros por criterios como sexo, tamaño, ubicación.
   - Gestionar paginación desde el backend.

3. **Gestión de usuarios y perfiles**
   - Registro de usuarios con verificación de correo electrónico.
   - Inicio de sesión mediante JWT.
   - Recuperación de contraseña.
   - Perfil de usuario y datos adicionales (perfil extendido).

4. **Panel administrativo**
   - Revisar perros antes de aprobarlos o publicarlos.
   - Gestionar usuarios y roles.
   - Supervisar solicitudes de adopción y documentos asociados.

5. **Seguridad y robustez**
   - Autenticación y autorización con Spring Security y JWT.
   - Control de acceso por roles.
   - Rate limiting en endpoints sensibles.
   - Manejo estructurado de errores.

6. **Despliegue y operación**
   - Soporte para despliegue con Docker y Docker Compose.
   - Integración con MySQL y migraciones con Flyway.
   - Observabilidad básica con Spring Boot Actuator.

---

## 3. Visión general de la arquitectura

A alto nivel, la arquitectura se divide en:

- **Backend (Spring Boot)**  
  Proporciona la API REST que gestiona usuarios, autenticación, perfiles, perros, adopciones, imágenes y otras funcionalidades de negocio.  
  Ubicación: `src/main/java/com/cut/cardona`.

- **Frontend (Vue 3)**  
  Aplicación SPA que consume la API, gestiona rutas, estado de sesión y presenta los datos a usuarios finales y administradores.  
  Ubicación: `front/`.

- **Infraestructura**  
  - Base de datos MySQL.
  - Docker y Docker Compose para orquestar servicios.
  - Integración con almacenamiento local y/o Cloudinary para imágenes.
  - Actuator para health checks.

Comunicación:

- El frontend consume endpoints bajo `/api/**`.
- La autenticación se basa en tokens JWT enviados en cabeceras `Authorization: Bearer <token>`.

---

## 4. Backend

### 4.1 Tecnologías principales

El backend está construido con:

- Java 17
- Spring Boot 3.x
- Spring Web (REST)
- Spring Security 6
- Spring Data JPA (MySQL)
- JWT (Auth0 `java-jwt`)
- Flyway para migraciones de base de datos
- Spring Boot Actuator
- Maven
- Docker / Docker Compose

### 4.2 Estructura de paquetes

Ubicación base: `src/main/java/com/cut/cardona`

Paquetes principales:

- `api.controller`  
  Controladores REST de la API.
  - `api.controller.auth` — login, registro, verificación email, recuperación de contraseña.
  - `api.controller.perros` — gestión de perros (público + dueños + admin reviewer).
  - `api.controller.perfil` — gestión del perfil de usuario.
  - `api.controller.adopcion` — endpoints de solicitudes de adopción (usuario y admin reviewer).
  - `api.controller.admin` — operaciones administrativas generales.
  - `api.controller` (otros) — imágenes, HelloController, CustomErrorController, etc.

- `service`  
  Capa de lógica de negocio.
  - `service.auth` — autenticación, registro, verificación de email, recuperación de contraseña.
  - `service.perros` — gestión de perros.
  - `service.perfil` — servicios para perfil de usuario.
  - `service.adopcion` — servicios relacionados con solicitudes de adopción (usuario y admin reviewer).
  - `service.imagenes` — lógica de manejo de imágenes.
  - `service.infra.storage` — abstracciones de almacenamiento (local/Cloudinary).
  - `service.admin` — lógica administrativa.

- `modelo`  
  Entidades JPA, DTOs y enumeraciones.
  - `modelo.usuarios` — `Usuario`, `Roles`, `VerificationToken`, `ResetToken`, repositorios asociados.
  - `modelo.perros` — `Perro`, `ImagenPerro`, enums `PerroEstadoRevision`, `PerroEstadoAdopcion`.
  - `modelo.perfil` — `PerfilUsuario` y su repositorio.
  - `modelo.adopcion` — `SolicitudAdopcion`, `DocumentoSolicitud`, `SolicitudEstado`.
  - `modelo.imagenes` — entidades auxiliares para imágenes (`ImagenPerfil`, `Histograma`, etc.).
  - `modelo.dto` — DTOs de entrada/salida: auth, registro, perros, usuario, perfil, adopción, respuestas comunes.

- `security`  
  Configuración de seguridad y componentes relacionados:
  - `SecurityConfiguration` — configuración principal de Spring Security.
  - `SecurityFilter` — filtro JWT.
  - `TokenService` — generación y validación de tokens JWT.
  - `CustomUserDetails` — adaptación de `Usuario` a `UserDetails` de Spring.
  - `CustomAutenticationService` — implementación de `UserDetailsService` que carga usuario por username o email.
  - `PasswordService` — manejo de contraseñas.
  - `security.ratelimit` — filtro y configuración de rate limiting.

- `errores`  
  Tipos de excepción y manejadores globales:
  - `GlobalExceptionHandler`, `ErrorHandler` — centralizan manejo de errores.
  - Excepciones específicas: `UnprocessableEntityException`, `DomainConflictException`, etc.
  - `ErrorResponse` — formato de respuesta de error.

- `validaciones`  
  Validaciones personalizadas:
  - `MayorDe15`, `MayorDe15Validator` — ejemplo de validación de edad mínima.
  - `ImagenObligatoria`, `ImagenObligatoriaValidator` — validan presencia de imágenes donde sea obligatorio.

- `app`, `util`  
  Configuración de aplicación (`AppConfig`), utilidades (`JPAutil`, etc).

### 4.3 Seguridad y autenticación

La seguridad está soportada por Spring Security y JWT:

- **Autenticación**
  - El usuario se autentica a través de endpoints de login.
  - `CustomAutenticationService` (`UserDetailsService`) carga usuarios por nombre de usuario o email usando `RepositorioUsuario`.
  - `CustomUserDetails` adapta la entidad `Usuario` al modelo de Spring Security.

- **Autorización**
  - Roles (`Roles` enum) asociados a cada usuario.
  - Los endpoints están protegidos según rol y/o autenticación.
  - Métodos de servicio y controladores pueden estar anotados o configurados en `SecurityConfiguration`.

- **JWT**
  - `TokenService` se encarga de generar y validar tokens JWT con Auth0 `java-jwt`.
  - Los tokens se envían en la cabecera `Authorization`.
  - Un filtro (`SecurityFilter`) intercepta peticiones y valida el token.

- **Rate limiting**
  - `security.ratelimit.RateLimitFilter` y configuración asociada limitan el número de peticiones a ciertos endpoints para prevenir abusos.

- **Recuperación y verificación**
  - `PasswordRecoveryService` maneja el flujo de recuperación de contraseña.
  - `VerificationService` gestiona verificación de correo y reenvío de tokens.

### 4.4 Entidades principales del dominio

1. **Usuario (`modelo.usuarios.Usuario`)**
   - Representa a una persona que interactúa con el sistema.
   - Campos típicos: username, email, contraseña, rol, estado de verificación, etc.
   - Asociaciones:
     - Uno a uno con `PerfilUsuario`.
     - Relación con `Perro` (perros del usuario).
     - Tokens de verificación y reset (`VerificationToken`, `ResetToken`).

2. **Perfil de usuario (`modelo.perfil.PerfilUsuario`)**
   - Información extendida del usuario (datos personales adicionales).
   - Gestionado por `PerfilService` y `PerfilController`.

3. **Perro (`modelo.perros.Perro`)**
   - Datos del perro: nombre, edad, sexo, tamaño, descripción, etc.
   - Estado de revisión (`PerroEstadoRevision`) y de adopción (`PerroEstadoAdopcion`).
   - Relación con `Usuario` (propietario o responsable).
   - Relación con `ImagenPerro`.

4. **Imagen de perro (`modelo.perros.ImagenPerro`)**
   - Metadatos de cada imagen asociada a un perro.
   - Integración con almacenamiento local o Cloudinary a través de `ImageStorageService`.

5. **Solicitud de adopción (`modelo.adopcion.SolicitudAdopcion`)**
   - Representa la intención de adoptar un perro por parte de un usuario.
   - Estados gestionados por `SolicitudEstado`.
   - Documentos asociados (`DocumentoSolicitud`).

6. **Imágenes de perfil y métricas (`modelo.imagenes.*`)**
   - `ImagenPerfil`, `Histograma`, etc., relacionados con análisis de imágenes (histogramas).

### 4.5 Esquema de base de datos MySQL

El sistema utiliza MySQL como base de datos relacional. A continuación se describen las tablas principales y sus relaciones:

#### Tabla: `usuarios`

Almacena la información de autenticación y datos básicos de los usuarios del sistema.

**Campos principales:**
- `id` (BIGINT, PK): Identificador único del usuario
- `username` (VARCHAR): Nombre de usuario único
- `email` (VARCHAR): Correo electrónico único
- `password` (VARCHAR): Contraseña hasheada
- `rol` (VARCHAR/ENUM): Rol del usuario (USUARIO, ADMIN, REVIEWER, etc.)
- `verificado` (BOOLEAN): Indica si el email ha sido verificado
- `activo` (BOOLEAN): Indica si la cuenta está activa
- `fecha_creacion` (TIMESTAMP): Fecha de registro
- `fecha_actualizacion` (TIMESTAMP): Última modificación

**Relaciones:**
- 1:1 con `perfil_usuario`
- 1:N con `perros` (como propietario)
- 1:N con `verification_tokens`
- 1:N con `reset_tokens`
- 1:N con `solicitudes_adopcion`

---

#### Tabla: `perfil_usuario`

Información extendida del perfil de cada usuario.

**Campos principales:**
- `id` (BIGINT, PK): Identificador único del perfil
- `usuario_id` (BIGINT, FK): Referencia a `usuarios.id`
- `nombre` (VARCHAR): Nombre completo
- `apellidos` (VARCHAR): Apellidos
- `telefono` (VARCHAR): Número de teléfono
- `direccion` (VARCHAR): Dirección completa
- `ciudad` (VARCHAR): Ciudad de residencia
- `provincia` (VARCHAR): Provincia
- `codigo_postal` (VARCHAR): Código postal
- `fecha_nacimiento` (DATE): Fecha de nacimiento
- `biografia` (TEXT): Descripción o biografía del usuario
- `imagen_perfil_url` (VARCHAR): URL de la imagen de perfil

**Relaciones:**
- N:1 con `usuarios`

---

#### Tabla: `perros`

Almacena la información de cada perro registrado en el sistema.

**Campos principales:**
- `id` (BIGINT, PK): Identificador único del perro
- `nombre` (VARCHAR): Nombre del perro
- `edad` (INT): Edad en años
- `sexo` (VARCHAR/ENUM): Sexo (MACHO, HEMBRA)
- `tamaño` (VARCHAR/ENUM): Tamaño (PEQUEÑO, MEDIANO, GRANDE, MUY_GRANDE)
- `raza` (VARCHAR): Raza del perro
- `descripcion` (TEXT): Descripción detallada
- `caracteristicas` (TEXT): Características especiales
- `necesidades_especiales` (TEXT): Necesidades médicas o de cuidado
- `ubicacion` (VARCHAR): Ubicación del perro
- `usuario_id` (BIGINT, FK): Propietario o responsable del perro
- `estado_revision` (VARCHAR/ENUM): Estado de revisión (PENDIENTE, APROBADO, RECHAZADO)
- `estado_adopcion` (VARCHAR/ENUM): Estado de adopción (DISPONIBLE, EN_PROCESO, ADOPTADO, NO_DISPONIBLE)
- `fecha_creacion` (TIMESTAMP): Fecha de registro
- `fecha_actualizacion` (TIMESTAMP): Última modificación
- `fecha_publicacion` (TIMESTAMP): Fecha en que fue aprobado/publicado

**Relaciones:**
- N:1 con `usuarios` (propietario)
- 1:N con `imagen_perro`
- 1:N con `solicitudes_adopcion`

---

#### Tabla: `imagen_perro`

Metadatos de las imágenes asociadas a cada perro.

**Campos principales:**
- `id` (BIGINT, PK): Identificador único de la imagen
- `perro_id` (BIGINT, FK): Referencia a `perros.id`
- `url` (VARCHAR): URL de la imagen (local o Cloudinary)
- `public_id` (VARCHAR): ID público en servicio de almacenamiento externo
- `es_principal` (BOOLEAN): Indica si es la imagen principal del perro
- `orden` (INT): Orden de visualización
- `formato` (VARCHAR): Formato de archivo (jpg, png, etc.)
- `tamaño` (BIGINT): Tamaño en bytes
- `ancho` (INT): Ancho en píxeles
- `alto` (INT): Alto en píxeles
- `fecha_subida` (TIMESTAMP): Fecha de carga

**Relaciones:**
- N:1 con `perros`

---

#### Tabla: `solicitudes_adopcion`

Registra las solicitudes de adopción realizadas por usuarios.

**Campos principales:**
- `id` (BIGINT, PK): Identificador único de la solicitud
- `perro_id` (BIGINT, FK): Perro que se desea adoptar
- `usuario_id` (BIGINT, FK): Usuario solicitante
- `estado` (VARCHAR/ENUM): Estado de la solicitud (PENDIENTE, EN_REVISION, APROBADA, RECHAZADA, CANCELADA)
- `motivo_adopcion` (TEXT): Razones por las que desea adoptar
- `experiencia_previa` (TEXT): Experiencia previa con mascotas
- `situacion_vivienda` (TEXT): Descripción de la vivienda
- `otros_animales` (TEXT): Información sobre otras mascotas
- `comentarios_admin` (TEXT): Notas del administrador
- `fecha_solicitud` (TIMESTAMP): Fecha de creación de la solicitud
- `fecha_actualizacion` (TIMESTAMP): Última modificación
- `fecha_resolucion` (TIMESTAMP): Fecha de aprobación/rechazo

**Relaciones:**
- N:1 con `perros`
- N:1 con `usuarios`
- 1:N con `documentos_solicitud`

---

#### Tabla: `documentos_solicitud`

Documentos adjuntos a las solicitudes de adopción.

**Campos principales:**
- `id` (BIGINT, PK): Identificador único del documento
- `solicitud_id` (BIGINT, FK): Referencia a `solicitudes_adopcion.id`
- `tipo_documento` (VARCHAR/ENUM): Tipo (DNI, COMPROBANTE_DOMICILIO, REFERENCIA, etc.)
- `nombre_archivo` (VARCHAR): Nombre original del archivo
- `url` (VARCHAR): URL del documento almacenado
- `public_id` (VARCHAR): ID en servicio de almacenamiento
- `formato` (VARCHAR): Extensión del archivo
- `tamaño` (BIGINT): Tamaño en bytes
- `fecha_subida` (TIMESTAMP): Fecha de carga

**Relaciones:**
- N:1 con `solicitudes_adopcion`

---

#### Tabla: `verification_tokens`

Tokens para verificación de correo electrónico.

**Campos principales:**
- `id` (BIGINT, PK): Identificador único del token
- `usuario_id` (BIGINT, FK): Referencia a `usuarios.id`
- `token` (VARCHAR): Token de verificación único
- `fecha_creacion` (TIMESTAMP): Fecha de generación
- `fecha_expiracion` (TIMESTAMP): Fecha de expiración
- `usado` (BOOLEAN): Indica si ya fue utilizado

**Relaciones:**
- N:1 con `usuarios`

---

#### Tabla: `reset_tokens`

Tokens para recuperación de contraseña.

**Campos principales:**
- `id` (BIGINT, PK): Identificador único del token
- `usuario_id` (BIGINT, FK): Referencia a `usuarios.id`
- `token` (VARCHAR): Token de reset único
- `fecha_creacion` (TIMESTAMP): Fecha de generación
- `fecha_expiracion` (TIMESTAMP): Fecha de expiración
- `usado` (BOOLEAN): Indica si ya fue utilizado

**Relaciones:**
- N:1 con `usuarios`

---

#### Tabla: `imagen_perfil`

Almacena metadatos de imágenes de perfil de usuario (si se utiliza análisis adicional).

**Campos principales:**
- `id` (BIGINT, PK): Identificador único
- `usuario_id` (BIGINT, FK): Referencia a `usuarios.id`
- `url` (VARCHAR): URL de la imagen
- `public_id` (VARCHAR): ID en servicio de almacenamiento
- `formato` (VARCHAR): Formato del archivo
- `ancho` (INT): Ancho en píxeles
- `alto` (INT): Alto en píxeles
- `fecha_subida` (TIMESTAMP): Fecha de carga

**Relaciones:**
- N:1 con `usuarios`

---

#### Tabla: `histograma`

Tabla auxiliar para almacenar datos de histogramas de imágenes (análisis de color, etc.).

**Campos principales:**
- `id` (BIGINT, PK): Identificador único
- `imagen_perfil_id` (BIGINT, FK): Referencia a `imagen_perfil.id`
- `canal` (VARCHAR): Canal de color (RED, GREEN, BLUE)
- `data` (BLOB/TEXT): Datos del histograma serializados
- `fecha_calculo` (TIMESTAMP): Fecha de cálculo

**Relaciones:**
- N:1 con `imagen_perfil`

---

#### Tabla: `flyway_schema_history`

Tabla de control de migraciones de Flyway (gestión automática).

**Campos:**
- `installed_rank` (INT): Orden de instalación
- `version` (VARCHAR): Versión de la migración
- `description` (VARCHAR): Descripción
- `type` (VARCHAR): Tipo de migración
- `script` (VARCHAR): Nombre del script
- `checksum` (INT): Suma de verificación
- `installed_by` (VARCHAR): Usuario que ejecutó
- `installed_on` (TIMESTAMP): Fecha de ejecución
- `execution_time` (INT): Tiempo de ejecución en ms
- `success` (BOOLEAN): Éxito de la migración

---

#### Diagrama conceptual de relaciones

```
usuarios (1) ←→ (1) perfil_usuario
usuarios (1) ←→ (N) perros
usuarios (1) ←→ (N) solicitudes_adopcion
usuarios (1) ←→ (N) verification_tokens
usuarios (1) ←→ (N) reset_tokens
usuarios (1) ←→ (N) imagen_perfil

perros (1) ←→ (N) imagen_perro
perros (1) ←→ (N) solicitudes_adopcion

solicitudes_adopcion (1) ←→ (N) documentos_solicitud

imagen_perfil (1) ←→ (N) histograma
```

**Notas importantes:**

- Todas las tablas incluyen índices en claves foráneas para optimizar consultas.
- Los campos de tipo TIMESTAMP se gestionan automáticamente con `@CreatedDate` y `@LastModifiedDate` de Spring Data JPA.
- Las migraciones de esquema se gestionan con Flyway, ubicadas en `src/main/resources/db/migration/`.
- Los campos enum se mapean como VARCHAR en la base de datos y se convierten mediante `@Enumerated` en las entidades JPA.

---

### 4.5 Flujos funcionales implementados en el backend

1. **Registro de usuario y verificación de email**
   - Endpoints en `api.controller.auth.RegistroController` y `VerificationController`.
   - DTOs:
     - `DtoRegistroUsuario`, `DtoRegistroCompletoRequest`, etc.
     - `EmailVerificationRequest`, `EmailVerificationConfirmRequest`.
   - Flujo:
     1. Usuario envía datos de registro.
     2. Se crea el usuario con estado pendiente de verificación.
     3. Se genera un token de verificación (`VerificationToken`) y se envía email.
     4. Usuario confirma el email mediante enlace/token.
     5. El usuario pasa a estado verificado.

2. **Login y gestión de sesión**
   - `AuthenticationController`, `AuthenticationService`.
   - DTOs:
     - `AuthenticationRequest`, `AuthenticationResponse`.
   - Flujo:
     1. Usuario envía credenciales.
     2. `CustomAutenticationService` carga el usuario.
     3. `TokenService` genera token JWT.
     4. Respuesta con token y, opcionalmente, datos básicos del usuario.

3. **Recuperación de contraseña**
   - `RecoveryController`, `PasswordRecoveryService`.
   - DTOs:
     - `ForgotPasswordRequest`, `ResetPasswordRequest`.
   - Flujo:
     1. Usuario solicita recuperación (forgot).
     2. El sistema genera token de reset (`ResetToken`) y envía email.
     3. Usuario envía nueva contraseña junto con el token.
     4. El sistema actualiza la contraseña.

4. **Gestión de perfil de usuario**
   - `PerfilController`, `PerfilService`.
   - DTOs:
     - `DtoPerfilCompleto`, `DtoActualizarPerfilRequest`.
   - Permite ver y actualizar información de perfil (autenticado).

5. **Catálogo público de perros**
   - `PerroController` (endpoints públicos para listado).
   - DTOs:
     - `DtoPerro`.
   - Flujo:
     1. Cliente (frontend o externo) llama al endpoint de catálogo con filtros.
     2. El backend aplica filtros, paginación y devuelve lista de perros visibles.

6. **Gestión de perros por parte del usuario**
   - `PerroController` (endpoints protegidos).
   - DTOs:
     - `CrearPerroRequest`, `ActualizarPerroRequest`, `ImagenPerroRequest`.
   - Flujo:
     - Crear perro, actualizar sus datos, asociar imágenes, listar perros de un usuario.

7. **Revisión de perros (admin reviewer)**
   - `AdminReviewerPerrosController`, `AdminReviewerAdopcionController`.
   - Permite aprobar o rechazar perros y solicitudes de adopción.

8. **Manejo de imágenes**
   - `ImagenController`, `ImagenService`, `ImageStorageService`.
   - Implementaciones de almacenamiento:
     - `LocalImageStorageService`.
     - `CloudinaryImageStorageService` (según configuración).
   - Subida, borrado y obtención de imágenes.

9. **Solicitudes de adopción**
   - `UsuarioAdopcionController`, `AdminReviewerAdopcionController`.
   - DTOs:
     - `DtoSolicitudAdopcion`.
   - Flujo:
     - Usuario autenticado crea una solicitud de adopción.
     - Admin reviewer revisa, cambia estado y gestiona documentos.

---

## 5. Frontend

### 5.1 Tecnologías principales

El frontend vive en la carpeta `front/` y usa:

- Vue 3 (composition API)
- Vite (herramienta de build)
- Tailwind CSS (v4)
- Flowbite (componentes UI)
- Pinia (gestión de estado)
- Axios (HTTP client)
- Nginx (en producción, como servidor de estáticos / proxy)

El archivo `front/README.md` resume:

- API base configurable: `VITE_API_BASE_URL` (por defecto, `http://localhost:8080/api`).
- Autenticación vía JWT (access + refresh), gestionada en los stores y en `src/utils/api.js`.

### 5.2 Estructura de carpetas

Ubicación base: `front/src`

Elementos destacados:

- `main.js`  
  Punto de entrada de la app Vue, monta la aplicación, registra router, stores, etc.

- `router/index.js`  
  - Define las rutas de la aplicación (públicas, protegidas, admin).
  - Aplica guards de autenticación y roles.
  - Gestiona títulos de página y `scrollBehavior`.

- `stores/`  
  Stores de Pinia:
  - `auth.js` — estado de autenticación, tokens, usuario actual y helpers para login/logout/refresh.
  - `perros.js` — gestión de estado relacionado con perros (catálogo, perros del usuario, etc).

- `views/`  
  Vistas de alto nivel asociadas a rutas:
  - `LandingPage.vue` — página principal/landing.
  - `auth/` — `Login.vue`, `Register.vue`, `VerifyEmail.vue`, `Forgot.vue`, `Reset.vue`.
  - `perros/` — `Catalogo.vue`, `MisPerros.vue`, `NuevoPerro.vue`.
  - `admin/` — `AdminDashboard.vue`.
  - `testback/Test.vue`, `Test.vue` — vistas de prueba/integración.

- `components/`  
  Componentes reutilizables:
  - `comun/` — cabecera, pie, etc. (`Header.vue`, `Footer.vue`).
  - `perros/` — componentes relacionados con perros:
    - `DogCard.vue` — tarjeta de perro en catálogo.
    - `DogForm.vue` — formulario para crear/editar perro.
    - `DogImagesUploader.vue`, `DogImagesEditor.vue` — subida y edición de imágenes.
    - `CatalogFilters.vue`, `CatalogToolbar.vue` — filtros y herramientas del catálogo.
    - `owner/OwnerDogCard.vue`, `owner/DogEditModal.vue` — elementos específicos para dueño de perro.
  - `admin/` — componentes del panel administrativo:
    - `AdminDogsTable.vue`, `AdminUsersTable.vue`, `AdminOverview.vue`, `AdminDocs.vue`.
  - `auth/` — `RegisterForm.vue`, etc.
  - `landing/` — secciones de landing:
    - `HeroSectionComponent.vue`, `ProjectFeaturesComponent.vue`, `HeroPlusDescriptionProjectComponent.vue`, `ContactFormComponent.vue`.
  - `error/NotFound.vue` — página 404.
  - `util/` — componentes auxiliares:
    - `SpinnerOverlay.vue`, `FieldError.vue`, `ConfirmModal.vue`, `Alert.vue`.

- `utils/`  
  - `api.js` — configuración central de Axios:
    - Base URL a partir de `VITE_API_BASE_URL`.
    - Interceptores para adjuntar token Bearer.
    - Lógica de refresh ante 401, con exclusiones para ciertos endpoints.
  - `adminApi.js` — helpers específicos para llamadas administrativas.

- `composablesJS/`  
  Composables para lógica reutilizable:
  - `usePagination.js`, `useTableFilters.js` — paginación y filtros.
  - `useAdminTable.js`, `useAdminStats.js` — lógica del panel admin.
  - `useFlowbite.js`, `useDrawerFix.js` — integración y fixes para Flowbite.
  - `useMarkdownRenderer.js` — renderizado de markdown para docs dentro del front.
  - `useImageCache.js` — gestión cache de imágenes.

- `style.css`  
  Punto de entrada de estilos, integra Tailwind y Flowbite.

### 5.3 Flujos en el frontend

1. **Navegación anónima**
   - Ruta `/` → `LandingPage.vue`.
   - Información general del proyecto y llamadas a acción.

2. **Autenticación**
   - `/login` → `Login.vue`.
   - `/register` → `Register.vue` (+ `RegisterForm.vue`).
   - `/verify-email` → `VerifyEmail.vue`.
   - `/forgot` → `Forgot.vue`.
   - `/reset` → `Reset.vue`.
   - El store `auth.js` coordina llamadas a endpoints de backend de auth y gestiona tokens.

3. **Catálogo de perros**
   - `/perros` → `perros/Catalogo.vue`.
   - Usa `perros.js` y componentes `DogCard`, `CatalogFilters`, `CatalogToolbar`, con filtros y paginación.
   - Consume endpoints de catálogo público.

4. **Gestión de perros del usuario**
   - `/perros/mis` → `MisPerros.vue`.
   - `/perros/nuevo` → `NuevoPerro.vue`.
   - Usa `DogForm`, `DogImagesUploader`, `DogImagesEditor`, `OwnerDogCard`, etc.
   - Requiere usuario autenticado; el router aplica guards.

5. **Panel administrativo**
   - `/admin` y rutas relacionadas → `AdminDashboard.vue`.
   - Componentes: `AdminDogsTable`, `AdminUsersTable`, `AdminOverview`, `AdminDocs`.
   - Permite revisar perros, usuarios y revisar información de manera centralizada.
   - Protegido por roles; el router verifica que el usuario tenga rol adecuado (ej. ADMIN/REVIEWER).

6. **Errores y navegación**
   - Rutas no encontradas → `NotFound.vue`.
   - Manejo de errores de API mediante `Alert.vue`, `FieldError.vue` y lógica en composables/stores.

---

## 6. Casos de uso basados en lo implementado

Esta sección describe flujos reales soportados por el código actual.

### Caso de uso 1: Usuario anónimo consulta el catálogo de perros

1. El usuario entra a `/`.
2. Navega a `/perros`.
3. El frontend llama al endpoint de catálogo de perros del backend con filtros y paginación.
4. Se muestran tarjetas (`DogCard.vue`) con datos básicos del perro e imágenes.
5. El usuario puede ajustar filtros (sexo, tamaño, etc.) y la lista se actualiza.

### Caso de uso 2: Registro de usuario y verificación de correo

1. El usuario entra a `/register`.
2. Rellena el formulario (`RegisterForm.vue`) y envía.
3. El frontend llama al endpoint de registro (`RegistroController`) enviando los datos en un DTO de registro.
4. El backend crea el usuario y genera un `VerificationToken`.
5. Se envía un email con un enlace/token de verificación.
6. El usuario navega a `/verify-email` con el token.
7. El frontend llama al endpoint de confirmación (`VerificationController`).
8. El backend marca al usuario como verificado y ya puede autenticarse.

### Caso de uso 3: Login y acceso a rutas protegidas

1. Usuario verificado entra a `/login` y envía credenciales.
2. El frontend manda la petición al endpoint de login (`AuthenticationController`).
3. El backend valida credenciales vía `CustomAutenticationService` y `CustomUserDetails`.
4. El backend responde con `AuthenticationResponse` incluyendo token JWT.
5. El store `auth.js` guarda token y datos del usuario.
6. El usuario intenta entrar a `/perros/mis`:
   - El router detecta que la ruta requiere auth.
   - Si hay token, permite el acceso.
   - Axios envía el token en la cabecera `Authorization`.
7. Si el token expira, los interceptores ejecutan el flujo de refresh (según configuración).

### Caso de uso 4: Usuario gestiona sus propios perros

1. Usuario autenticado entra a `/perros/mis`.
2. El frontend llama al endpoint de “mis perros”, filtrando por usuario actual.
3. Los perros se muestran usando componentes tipo `OwnerDogCard`.
4. Para crear un nuevo perro:
   - Usuario va a `/perros/nuevo`.
   - Rellena formulario (`DogForm`) y adjunta imágenes (`DogImagesUploader`).
   - El frontend manda `CrearPerroRequest` e imágenes al backend (`PerroController` + `ImagenController`).
   - El backend valida datos, guarda el perro y las imágenes.
   - Dependiendo de lógica de revisión, el perro puede quedar pendiente de aprobación por admin.

### Caso de uso 5: Administrador revisa perros y solicitudes de adopción

1. Un usuario con rol administrador o reviewer entra a `/admin`.
2. El router comprueba el rol a través del store `auth.js`.
3. El backend expone endpoints de administración (`AdminReviewerPerrosController`, `AdminReviewerAdopcionController`).
4. El frontend muestra tablas (`AdminDogsTable`, `AdminUsersTable`) obtenidas de `adminApi.js`.
5. El admin puede cambiar estados:
   - Aprobar/rechazar perros.
   - Revisar solicitudes de adopción y documentos (`DtoSolicitudAdopcion`, `DocumentoSolicitud`).
6. Los cambios se reflejan en el catálogo público y vistas de usuarios.

### Caso de uso 6: Recuperación de contraseña

1. El usuario va a `/forgot`.
2. Rellena su email y envía.
3. El frontend llama a los endpoints de recuperación en `RecoveryController`.
4. El backend genera un `ResetToken` y lo envía por email.
5. El usuario navega a `/reset` con token.
6. El frontend envía nueva contraseña y token a `RecoveryController`.
7. El backend valida token y actualiza la contraseña del usuario.

---

## 7. Cómo ejecutar el proyecto

### 7.1 Backend

**Prerrequisitos**

- Java 17
- Maven
- MySQL 8.x (o Docker)

**Variables de entorno mínimas (ejemplo)**

- `SPRING_DATASOURCE_URL=jdbc:mysql://HOST:3306/cutdb?useSSL=false&serverTimezone=UTC`
- `SPRING_DATASOURCE_USERNAME=usuario`
- `SPRING_DATASOURCE_PASSWORD=clave`
- `JWT_SECRET=secreto_fuerte`

Opcionales:

- `SPRING_PROFILES_ACTIVE=dev`
- `APP_CORS_ALLOWED_ORIGINS=http://localhost:5173` (o URL del front)

**Ejecutar local**

```bash
./mvnw spring-boot:run
```

El backend queda expuesto normalmente en `http://localhost:8080`.

### 7.2 Frontend

Ubicación: `front/`

**Prerrequisitos**

- Node 18+
- npm

**Configurar API base**

Crear `.env.local` (o similar) con:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

**Instalar dependencias y arrancar**

```bash
cd front
npm install
npm run dev
```

La aplicación se servirá típicamente en `http://localhost:5173`.

### 7.3 Docker / Docker Compose

En la raíz del proyecto hay:

- `Dockerfile`
- `docker-compose.yml`

Ejemplo básico:

```bash
docker compose up -d
```

Esto levanta:

- API backend (Spring Boot)
- MySQL

---

## 8. Punto de entrada 

Un posible orden para entenderlo es:

1. **Leer esta documentación** para tener visión general.
2. **Revisar el flujo de auth**:
   - Backend: `api.controller.auth`, `security`, `service.auth`.
   - Frontend: `front/src/stores/auth.js`, `front/src/views/auth/`.
3. **Revisar el dominio de perros**:
   - Backend: `modelo.perros`, `modelo.dto.perros`, `PerroController`, `PerroService`.
   - Frontend: `front/src/stores/perros.js`, vistas `perros/`, componentes `perros/`.
4. **Explorar panel admin**:
   - Backend: `api.controller.admin`, `api.controller.perros.AdminReviewerPerrosController`, `service.admin`.
   - Frontend: `front/src/views/admin/AdminDashboard.vue`, `front/src/components/admin/`.
5. **Consultas específicas**:
   - Manejo de imágenes → `service.imagenes`, `service.infra.storage`, `ImagenController`, componentes `DogImages*`.
   - Seguridad y JWT → `security` (especialmente `SecurityConfiguration`, `SecurityFilter`, `TokenService`, `CustomAutenticationService`).
   - Errores → `errores` y cómo el frontend los presenta (`Alert.vue`, `FieldError.vue`).

---

