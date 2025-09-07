# Plan de Acción Integral — Perritos Cut

Objetivo: Implementar mejoras de backend, base de datos, frontend y DevOps para soportar el flujo completo de publicación, revisión y adopción de perros, con seguridad, integridad, observabilidad y una UX sólida.


## Alineación Front-Back (estado actualizado)
- Backend vigente:
  - F1: Dominio y catálogo público con filtros/paginación. [Hecho]
  - F2: Autenticación avanzada: login/refresh/logout, verificación de email y recuperación (forgot/reset). [Hecho]
- Frontend vigente:
  - F0: Cimientos (Axios central, interceptores, guards, Header con sesión). [Hecho]
  - F1: Catálogo público (filtros/paginación). [Hecho]
  - F2: Autenticación + verificación + recuperación. [Hecho]

Mapa de fases sincronizado
- En curso: Front F3 (Mis Perros + Alta) sobre Back F1/F2 existentes.
- Próximo: Back F3 (storage e imágenes) en preparación; Back F4 (migraciones) planificado.
- Luego: Front F4 (solicitudes de adopción) y siguientes módulos (admin, perfil, E2E).

Siguientes pasos coordinados (DoR/DoD compartidos)
- Front F3 (en curso):
  - Completar MisPerros y NuevoPerro; validaciones e imágenes con “una principal”.
- Back F3 (preparación):
  - Abstracción de storage (local/minio) y regla de imagen principal.
- Back F4 (planificado):
  - Migraciones Flyway (checks, índices, auditoría, tokens reset).

---

Estado actual asumido
- Stack backend: Spring Boot + MySQL + Flyway, artefactos Maven.
- Frontend: Vue 3 + Vite + Tailwind (carpeta front/), router y stores presentes.
- Docker Compose disponible; documentos de guía y seguridad existentes.

Principios
- Seguridad por defecto; validación en capas (frontend, servicio y DB) con la base de datos como última barrera.
- Cambios desplegables por fases, con migraciones reversibles y feature flags cuando aplique.
- DoD: Build + Lint + Tests verdes; migraciones aplicadas; documentación y monitoreo activos.

Roadmap por fases (estado)
- Fase 0 — Diagnóstico y preparación [Hecho]
- Fase 1 — Dominio y validación backend [Hecho]
- Fase 2 — Seguridad y recuperación de contraseña [Hecho]
- Fase 3 — Almacenamiento de archivos e imágenes [En preparación]
- Fase 4 — Migraciones de base de datos e índices [Planificado]
- Fase 5 — Notificaciones y eventos [Planificado]
- Fase 6 — Frontend: catálogo, detalle y wizard de adopción [Parcial: catálogo listo]
- Fase 7 — Panel revisor/admin [Planificado]
- Fase 8 — Observabilidad y CI/CD [Planificado]
- Fase 9 — Pruebas E2E, smoke tests y rollout [Planificado]

Dependencias clave
- Fase 4 depende de definición de enums/estados (F1).
- Wizard de adopción (F6) depende de estados y validaciones (F1/F4).
- Panel revisor (F7) depende de colas y estados (F1/F4/F5).

---

Fase 0 — Diagnóstico y preparación
Checklist
- [x] Inventariar entidades clave.
- [x] Exportar esquema actual y revisar collation/charset.
- [x] Revisar triggers existentes y reglas de negocio en DB.
- [x] Identificar endpoints y roles actuales.
Entrega/DoD
- Documento de diagnóstico breve y confirmación de supuestos.

Fase 1 — Dominio y validación backend
Tareas
- [ ] Crear enums fuertes: PerroEstadoAdopcion, PerroEstadoRevision, SolicitudEstado; mapear con @Enumerated(EnumType.STRING).
- [ ] Validaciones Bean Validation en DTOs/entidades (estados válidos, tamaños, mime, etc.).
- [ ] Añadir @Version a Perro y SolicitudAdopcion para bloqueo optimista.
- [ ] Replicar en servicios la lógica de triggers:
  - [ ] Una imagen principal por perro (transacción: al marcar una, desmarcar las demás).
  - [ ] Una solicitud activa por perro/usuario; sincronizar estados al aceptar.
- [ ] Paginación y filtros (sexo, tamaño, ubicación, estado) en endpoints de perros.
- [ ] DTOs con MapStruct; evitar exponer entidades.
- [ ] @ControllerAdvice global para errores; códigos y mensajes consistentes.
- [ ] OpenAPI/Swagger auto-generado y accesible en dev.
Criterios de aceptación
- Endpoints responden con 200/4xx/5xx consistentes; swagger documenta filtros y paginación.
- Concurrencia controlada por @Version; conflictos devuelven 409.
- Reglas de negocio fallidas devuelven 422 con detalle.

Fase 2 — Seguridad y recuperación de contraseña
Tareas
- [ ] JWT access + refresh tokens; expiraciones configurables.
- [ ] Roles: ROLE_USER, ROLE_REVIEWER, ROLE_ADMIN; configuración de métodos/endpoints con @PreAuthorize.
- [ ] Recuperación password: tokens dedicados en tabla propia con expiración/uso único.
- [ ] Endpoints: login, refresh, forgot, reset; rate limiting y CORS.
Criterios
- Flujos de login/refresh/reset auditados; tokens revocados correctamente.

Fase 3 — Almacenamiento de archivos e imágenes
Tareas
- [ ] Abstracción StorageService (S3/MinIO/Azure Blob) con interfaz y adaptadores.
- [ ] Validar tipo_mime y tamaño; escaneo antivirus (ClamAV) opcional configurable.
- [ ] Pre-signed URLs para carga/descarga; expiración corta.
- [ ] Generación de thumbnails; persistir url_publica y ruta_archivo.
- [ ] Garantizar exactamente una imagen principal por perro (servicio + DB).
Criterios
- Subidas seguras sin exponer credenciales; miniaturas servidas en detalle de perro.

Fase 4 — Base de datos y migraciones
Tareas
- [ ] Unificar charset/collation utf8mb4_unicode_ci.
- [ ] CHECK constraints para estados (MySQL 8): perros.estado_* y solicitudes_adopcion.estado.
- [ ] Índices compuestos:
  - [ ] perros(estado_revision, estado_adopcion, fecha_publicacion)
  - [ ] solicitudes_adopcion(solicitante_id, estado, fecha_solicitud)
- [ ] Fulltext index en perros.descripcion, perros.raza.
- [ ] Auditoría: created_by, updated_by y fecha_actualizacion con default/trigger.
- [ ] Tokens reset: tabla dedicada con FK a usuarios.
- [ ] Opcional: evaluar BINARY(16) para IDs; plan de migración segura si aplica.
Archivos Flyway
- Vxxx__enums_y_checks.sql: enums lógicos + CHECKs.
- Vxxx__indices_busqueda.sql: índices y fulltext.
- Vxxx__auditoria.sql: columnas y triggers/defaults de auditoría.
- Vxxx__tokens_reset.sql: tabla de tokens de recuperación.
Criterios
- flyway migrate aplica sin errores en local y CI con Testcontainers.

Fase 5 — Notificaciones y eventos
Tareas
- [ ] Publicar eventos de dominio: SolicitudCreada, SolicitudRevisada, SolicitudAceptada.
- [ ] Canalizaciones de notificación: Email (y opcional Push/WebSocket para tiempo real).
- [ ] Plantillas de email transaccionales.
Criterios
- Notificaciones emitidas en creación, revisión y aceptación; idempotencia garantizada.

Fase 6 — Frontend: catálogo, detalle y wizard
Catálogo
- [ ] Listado con filtros (sexo, tamaño, ubicación, estado) y orden por fecha.
- [ ] Paginación; skeleton loaders.
Detalle
- [ ] Carrusel respetando imagen principal.
- [ ] Badge de estado de adopción y revisión.
Wizard de adopción
- [ ] Paso a paso con validaciones de datos.
- [ ] Carga de documentos con pre-signed URLs.
- [ ] Barra de progreso y estado en tiempo real (WebSocket si disponible; fallback polling).
Perfil
- [ ] Edición de perfil; cambio de imagen con una sola "activa".
Criterios
- UX accesible y responsiva; errores mostrados contextual y traducidos.

Fase 7 — Panel revisor/admin
Tareas
- [ ] Cola de revisión de perros con acciones aprobar/rechazar.
- [ ] Vista de solicitudes pendientes/aceptadas.
- [ ] Métricas básicas (por periodo, por ubicación).
Criterios
- Acceso gated por roles; acciones auditable; filtros eficientes.

Fase 8 — Observabilidad y CI/CD
Observabilidad
- [ ] Spring Boot Actuator habilitado (health, metrics, info).
- [ ] Micrometer + Prometheus; tableros básicos.
- [ ] OpenTelemetry para trazas.
- [ ] Logs estructurados JSON.
CI/CD (GitHub Actions)
- [ ] Backend: mvn -B verify con Testcontainers.
- [ ] Frontend: npm ci && npm run build.
- [ ] Publicar artefactos; construir imágenes Docker.
- [ ] Despliegue: ejecutar Flyway; variables y secretos seguros.
Criterios
- Pipeline verde; despliegue reproducible y auditable.

Fase 9 — Pruebas E2E, smoke y rollout
Tareas
- [ ] E2E: flujos publicar, solicitar, revisar, aceptar.
- [ ] Smoke post-deploy; alarmas y rollback plan.
- [ ] Monitoreo de métricas claves (tasa de error, latencias, uso de recursos).
Criterios
- KPIs dentro de umbrales; cero incidentes críticos tras rollout.

---

Detalles por área (contratos y edge cases)
Backend: Contratos mínimos
- Entradas: filtros (sexo, tamaño, ubicación, estado), datos de solicitud, archivos.
- Salidas: páginas tipadas, DTOs limpios, errores con códigos consistentes.
- Errores: 400 validación, 401/403 authz, 404 no encontrado, 409 conflicto (@Version), 422 reglas de dominio.
Edge cases
- Concurrencia al marcar imagen principal.
- Doble solicitud del mismo usuario/perro.
- Archivos inválidos o demasiado grandes; tiempo de subida expirado.
- Reintentos de notificación.

Base de datos
- Reforzar FKs, UNIQUE y CHECKs; documentar triggers actuales.
- Verificar cardinalidad de imágenes y solicitudes por perro/usuario.

Frontend
- Estados vacíos/sin resultados; páginas largas; reconexión de WebSocket.
- Accesibilidad (foco, aria), i18n con zona horaria usuario.

---

Plan de pruebas
- Unitarias: servicios y validadores (feliz y 2 edge cases).
- Repositorio: integridad y consultas con Testcontainers MySQL.
- Integración: endpoints con seguridad y autorizaciones.
- Frontend: pruebas de componentes clave y flujos críticos.
- E2E: Cypress/Playwright contra entorno de staging.

Métricas de éxito
- Tasa de errores < 1%; p95 latencia < 300ms en listing; 0 inconsistencias de estados.
- Tiempo de subida de imagen p95 < 2s; adopciones completas sin intervención manual.

Riesgos y mitigación
- Cambios de esquema: aplicar en ventanas bajas y con backups.
- Complejidad de roles: pruebas exhaustivas de permisos.
- Almacenamiento externo: timeouts; usar retries exponenciales.

Siguientes pasos inmediatos (48h)
- [ ] Front F3: finalizar Mis Perros y Nuevo Perro (UX/validaciones/imágenes).
- [ ] Back F3: interfaz StorageService + Stub local y regla “una principal”.
- [ ] Back F4: borradores Flyway para checks/índices/auditoría/tokens.

Propiedad
- Backend: F1, F2, F3, F5, F8, F9.
- Base de datos: F4, soporte a F1/F2.
- Frontend: F6, F7.
- DevOps: F8, soporte a F3/F4.

Definición de Hecho (global)
- Build y tests verdes en CI; migraciones aplicadas.
- Documentación actualizada (Swagger/README/SECURITY/ACTUATOR).
- Observabilidad: métricas y health endpoints expuestos.
- Revisión cruzada y checklist de seguridad completados.
