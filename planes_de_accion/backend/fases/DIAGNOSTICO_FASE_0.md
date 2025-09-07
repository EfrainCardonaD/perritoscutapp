# Diagnóstico Fase 0 — Perritos Cut

Objetivo de la fase: Inventariar entidades, endpoints, seguridad, migraciones y reglas en BD; confirmar supuestos y detectar brechas frente al plan de mejoras.

Resumen ejecutivo
- Backend y DB ya cubren gran parte del flujo de adopción con integridad fuerte (FKs, índices y triggers).
- Estados y varios campos se manejan como String en entidades; en BD hay ENUMs. No hay @Version.
- Seguridad: JWT con filtro propio; roles presentes: ROLE_USER y ROLE_ADMIN. Falta ROLE_REVIEWER.
- Existen migraciones Flyway V1–V5 con tablas, índices y triggers de negocio. Falta auditoría y fulltext.
- Frontend: no evaluado en esta fase; endpoints públicos y protegidos identificados.

1) Inventario — Dominio y persistencia
Entidades (@Entity)
- usuarios.Usuario (UserDetails), roles enum Roles { ROLE_ADMIN, ROLE_USER }
- perfil.PerfilUsuario, imagenes.ImagenPerfil, imagenes.Histograma
- perros.Perro, perros.ImagenPerro
- adopcion.SolicitudAdopcion, adopcion.DocumentoSolicitud

Estados en entidades (Java)
- Perro.sexo, Perro.tamano, Perro.estadoAdopcion, Perro.estadoRevision: String
- SolicitudAdopcion.estado: String
- Usuario.rol: Enum (Roles)

Estados en BD (MySQL)
- perros: sexo ENUM('Macho','Hembra'), tamano ENUM('Pequeño','Mediano','Grande'), estado_adopcion ENUM('Disponible','Adoptado','Pendiente','No disponible'), estado_revision ENUM('Pendiente','Aprobado','Rechazado')
- solicitudes_adopcion.estado ENUM('Pendiente','En revisión','Aceptada','Rechazada','Cancelada')
- documentos_solicitud.tipo_documento ENUM('Identificacion','CartaResponsiva')

Integridad y rendimiento en BD
- Charset/Collation: utf8mb4_unicode_ci consistente en tablas.
- Índices:
  - perros: (estado_revision, estado_adopcion), usuario_id, fecha_publicacion, sexo, tamano, ubicacion.
  - imagenes_perros: perro_id, (perro_id, principal), UNIQUE (perro_id, principal_flag) para 1 principal.
  - solicitudes_adopcion: perro_id, solicitante_id, estado, fecha_solicitud, UNIQUE (perro_id, solicitante_id, activo_flag) y UNIQUE (perro_id, accepted_flag).
  - documentos_solicitud: UNIQUE (solicitud_id, tipo_documento).
- Fulltext: no presente en perros.descripcion/raza.
- Auditoría: fechas creadas; no hay created_by/updated_by ni triggers de auditoría.

Triggers de negocio (V4)
- perros BEFORE UPDATE: validar imagen al aprobar; impedir Disponible si no Aprobado; impedir Adoptado sin solicitud aceptada.
- solicitudes_adopcion BEFORE INSERT: perro aprobado y disponible; evitar auto-solicitud del dueño.
- solicitudes_adopcion BEFORE UPDATE: validar documentos al pasar a En revisión/Aceptada; validar disponibilidad al Aceptar.
- solicitudes_adopcion AFTER UPDATE: al Aceptar, marcar perro Adoptado y rechazar otras solicitudes del mismo perro.

2) Inventario — Endpoints y servicios
Controladores
- AuthenticationController: POST /api/login, POST /api/refresh, POST /api/logout
- PerroController: GET /api/perros/catalogo (público), GET /api/perros/mis, POST /api/perros, POST /api/admin/perros/{id}/aprobar, POST /api/admin/perros/{id}/rechazar, PATCH /api/admin/perros/{id}/estado
- AdopcionController: POST /api/solicitudes, POST /api/solicitudes/{id}/documentos, GET /api/solicitudes/mis, GET /api/admin/solicitudes/pendientes, PATCH /api/admin/solicitudes/{id}/estado
- ImagenController: GET/HEAD /api/imagenes/perfil/{filename} (público)

Servicios con reglas
- PerroService: crea perro con al menos una imagen, evita múltiples “principal” en la misma petición; aprobar/rechazar/cambiar estado (sin validaciones de consistencia de estados en servicio).
- AdopcionService: crear solicitud (estado "Pendiente"), subir documento, listar propias; actualización de estado libre (sin validar transiciones ni requisitos de documentos/disp. más allá de triggers).

3) Seguridad
- Configuración HTTP: /api/login, registro y swagger/actuator permitAll; catálogo de perros público; resto requiere autenticación.
- Filtro JWT: SecurityFilter usa TokenService.getSubject(token) y carga el usuario; Authorization: Bearer ... requerido para refresh/logout (no hay cookies).
- Roles efectivos: hasRole('USER') y hasRole('ADMIN') en servicios. No existe ROLE_REVIEWER.
- Refresh: endpoint /api/refresh existe; en config no está explícitamente en permitAll. El filtro autentica si el token es válido (incluye refresh) antes del matcher, por lo que funcionaría, pero conviene permitir explícitamente si se valida como refresh token.
- Rate limiting: existe RateLimitFilter (clase presente); no revisado su wiring en SecurityConfiguration.

4) Brechas frente al plan
Dominio/Validación
- Falta migrar a enums fuertes en Java para estados de perro y solicitud con @Enumerated(EnumType.STRING).
- No hay @Version en Perro ni SolicitudAdopcion (sin bloqueo optimista).
- Validaciones Bean Validation ausentes en DTOs para filtros y creación.
- Reglas espejo de triggers parcialmente implementadas en servicio; faltan validaciones previas para feedback temprano (p. ej. transiciones de estado y requisitos de documentos/disp.).
- MapStruct no se usa; DTOs creados manualmente.

Seguridad
- Roles: falta ROLE_REVIEWER y gating fino para revisar/aprobar.
- Recuperación de contraseña: tokens están en usuarios (token, fecha_expiracion_token); falta tabla dedicada y endpoints/flows.
- /api/refresh no declarado permitAll; revisar estrategia de refresh tokens (almacenamiento/rotación/revocación).

Almacenamiento de archivos
- Se sirve desde filesystem local uploads/perfiles; no hay StorageService ni pre-signed URLs ni antivirus ni thumbnails.

Base de datos
- No hay índices fulltext en perros.descripcion/raza.
- Falta índice compuesto (estado_revision, estado_adopcion, fecha_publicacion) para ordenar por fecha en catálogos.
- No hay columnas created_by/updated_by ni triggers de auditoría.
- Tokens reset: no existe tabla dedicada.

Notificaciones y eventos
- No hay publicación de eventos de dominio ni notificaciones (Email/Push/WebSocket).

Frontend (contexto rápido)
- Endpoints necesarios existen; faltan filtros server-side documentados (sexo, tamaño, ubicación, estado) y paginación estandarizada.

5) Supuestos confirmados
- Integridad fuerte vía FKs, unicidades y triggers: Confirmado (V3, V4).
- Estados canónicos: Confirmados en BD; faltan enums en Java.
- Collation/charset: Unificados a utf8mb4_unicode_ci.
- Swagger/OpenAPI: Configurado con springdoc.
- Testcontainers: Dependencias presentes en pom; reportes de tests existen.

6) Recomendaciones inmediatas (Fase 1 ready)
- Crear enums: PerroEstadoAdopcion, PerroEstadoRevision, SolicitudEstado; mapear en entidades con @Enumerated(EnumType.STRING) y adaptar DTOs/servicios.
- Añadir @Version a Perro y SolicitudAdopcion.
- Validar en servicios reglas clave de triggers para feedback temprano; estandarizar respuestas 400/409/422.
- Exponer catálogo con paginación y filtros server-side (sexo,tamaño,ubicación,estado + orden fecha_publicacion).
- Seguridad: agregar ROLE_REVIEWER; definir @PreAuthorize para revisar/aprobar; decidir si /api/refresh es permitAll y validar refresh token específicamente.

7) Recomendaciones para Fase 4 (DB)
- Agregar índice compuesto perros(estado_revision, estado_adopcion, fecha_publicacion).
- Agregar FULLTEXT en perros(descripcion, raza) y endpoint de búsqueda.
- Crear tabla tokens_reset (id, usuario_id, token, expira_en, usado_en) con FK y limpieza.
- Auditoría: created_by, updated_by y trigger/DEFAULT para fecha_actualizacion.

8) Riesgos detectados
- Desalineación Java<->DB en estados (String vs ENUM) puede producir errores sutiles.
- Falta de @Version: riesgo de sobrescrituras concurrentes.
- Refresh token sin política explícita de rotación/revocación.
- Storage local sin validador de MIME/tamaño ni AV.

9) Cierre Fase 0 — DoD
- Inventario de entidades, endpoints y reglas: Completo.
- Confirmación de supuestos: Completo.
- Brechas documentadas y plan de próximos pasos para Fase 1 y Fase 4: Completo.

Anexos (fuentes clave revisadas)
- Flyway: V1__create_table-usuarios.sql, V2__create_perfil_usuario_tables.sql, V3__create_adopcion_perros_tables.sql, V4__triggers_y_restricciones_negocio.sql, V5__add_rol_to_usuarios.sql
- Config: application.properties, SecurityConfiguration, SecurityFilter
- Servicios: PerroService, AdopcionService
- Controladores: AuthenticationController, PerroController, AdopcionController, ImagenController

