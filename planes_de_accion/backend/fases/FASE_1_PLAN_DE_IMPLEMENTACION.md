# Plan de Implementación — Fase 1 (Backend/Seguridad/API)

Objetivo
- Alinear el dominio Java con los ENUMs de BD, incorporar bloqueo optimista, validar reglas de negocio en servicios, y exponer catálogo con filtros/paginación. Asegurar gating por rol para revisión y claridad en refresh.

Alcance Fase 1
- Dominio: enums fuertes para estados; @Version en entidades críticas; validaciones previas en servicios.
- API: catálogo público con filtros/paginación y orden por fecha_publicacion; respuestas de error consistentes.
- Seguridad: ROLE_REVIEWER; autorización a endpoints de revisión; permitir /api/refresh explícitamente.
- DB: migración para columnas de versión (idempotente). Índices avanzados y fulltext quedan para Fase 4.

Estado actual (según repo)
- Hecho:
  - Enums fuertes: PerroEstadoAdopcion, PerroEstadoRevision, SolicitudEstado + converters JPA.
  - @Version en Perro y SolicitudAdopcion. Flyway V6 añade columnas version (idempotente). V3 actualizado para instalaciones nuevas.
  - Servicios: validaciones previas clave (imagen principal al aprobar; DISPONIBLE solo si APROBADO; reglas básicas de solicitud y transiciones).
  - Catálogo: filtros (sexo, tamaño, ubicación) + paginación (page/size) y orden por fecha_publicacion desc.
  - Seguridad: agregado ROLE_REVIEWER; @PreAuthorize en revisión/aprobación; /api/refresh permitido explícitamente.
  - DTOs exponen etiquetas legibles de enums.

Checklist de estado (F1)
- [x] Enums fuertes y mapeos @Enumerated/@Converter.
- [x] Bloqueo optimista con @Version en entidades críticas y Flyway V6 aplicado.
- [x] Catálogo público GET /api/perros/catalogo con filtros y paginación (permitAll).
- [x] Validaciones Bean Validation en DTOs y filtros de catálogo.
- [x] Excepciones de dominio (409/422) y GlobalExceptionHandler mapeando códigos.
- [x] Seguridad por roles (ADMIN/REVIEWER) en flujos de revisión.
- [x] Swagger habilitado (/v3/api-docs, /swagger-ui/**).

Pendiente inmediato (ajustado)
- [ ] Ampliar cobertura de tests unitarios e integración (servicios, queries, seguridad en endpoints de revisión y catálogo).
- [ ] Documentación OpenAPI: ejemplos de request/response para catálogo y estados; sincronizar con JSON_EXAMPLES/Insomnia.

Backlog (Fases futuras)
- DB: índice compuesto perros(estado_revision, estado_adopcion, fecha_publicacion); FULLTEXT(descripcion, raza).
- Auditoría created_by/updated_by y eventos/notificaciones.
- Storage robusto (servicio dedicado, antivirus, thumbnails, pre-signed URLs).
- Recuperación de contraseña con tabla tokens_reset y rotación de refresh tokens.

Tareas detalladas — Fase 1
1) Bean Validation en DTOs y filtros
- CrearPerroRequest: nombre @NotBlank; edad @Min(0) @Max(25); sexo/tamano @Pattern o validador in-list; ubicacion @Size(max=255); imagenes @NotEmpty con al menos una principal.
- CrearSolicitudRequest: perroId @NotBlank; mensaje @Size(max=2000).
- DocumentoRequest: tipoDocumento @NotBlank; urlDocumento @NotBlank @Size(max=500); tipoMime @Size(max=100); tamanoBytes @PositiveOrZero.
- Filtros catálogo: page>=0, size en [1..100]; sexo/tamano/ubicacion opcionales con tamaños máximos.
- Añadir mensajes a ValidationMessages.properties.

2) Errores y excepciones
- Crear excepciones: DomainConflictException (409), UnprocessableEntityException (422).
- GlobalExceptionHandler: mapear 409/422 con payload consistente.
- Reemplazar IllegalArgumentException en reglas que correspondan a 409 o 422 según el caso.

3) Servicios — validaciones adicionales (ligeras)
- PerroService: asegurar una y solo una imagen principal al crear (ya se fuerza en persistencia; mantener check).
- AdopcionService: reforzar validación de transición no válida con 409; validar documentos requeridos (ya se exige 2 tipos al EN_REVISION/ACEPTADA).

4) API y documentación
- PerroController.catalogo: documentar parámetros sexo/tamano/ubicacion/page/size en OpenAPI; ejemplos de respuesta.
- AdopcionController: documentar estados permitidos en PATCH.
- Alinear ejemplos en JSON_EXAMPLES.md e insomnia-api-collection.json.

5) Seguridad
- Verificar mapeo de authorities para ROLE_REVIEWER y endpoints anotados con hasAnyRole('ADMIN','REVIEWER').
- Confirmar RateLimitFilter registrado en el chain (si no, añadirlo).
- Revisar estrategia de refresh tokens (solo permitir en /api/refresh; rotación/revocación queda fuera de Fase 1).

6) Datos y migraciones
- Confirmar que Flyway V6 ejecuta en entornos existentes sin impacto (idempotente).
- Validar que no se requieren backfills adicionales (no aplican al añadir version con default 0).

7) Pruebas
- Unitarias: converters de enums; validadores de DTO; helpers de repos (catálogo filtrado/paginado); reglas de servicios (aprobar sin principal -> 400/422; DISPONIBLE sin APROBADO -> 422; aceptar con perro no disponible -> 409).
- Integración: flujo de creación -> revisión -> publicación -> solicitud -> documentos -> transición aceptada; seguridad por roles en endpoints admin/reviewer; /api/refresh accesible sin autenticación previa.
- Añadir perfiles de test y usar Testcontainers si existe configuración (pom ya lo incluye).

8) Despliegue y rollback
- Requiere: aplicar Flyway V6 automáticamente al iniciar; no hay pasos manuales adicionales.
- Rollback: deshabilitar feature flags (no aplica); V6 es no destructiva.

Checklist de inicio (PR F1)
- [x] Validaciones Bean Validation en DTOs y filtros (existentes).
- [x] Excepciones DomainConflict/Unprocessable + GlobalExceptionHandler (existentes).
- [x] PerroController.catalogo con filtros/paginación + permitAll (existente).
- [x] RateLimitFilter registrado (existente).
- [ ] OpenAPI actualizado con ejemplos.
- [ ] Tests unitarios e integración mínimos descritos, verdes.

Criterios de aceptación (DoD)
- Catálogo filtra por sexo/tamaño/ubicación, pagina y ordena por fecha; documentado en OpenAPI.
- Reglas de negocio clave validadas en servicio con códigos apropiados (400/409/422) y mensajes claros.
- ROLE_REVIEWER con acceso a revisión/aprobación; /api/refresh permitido sin autenticación previa; RateLimitFilter verificado.
- Migración Flyway V6 aplicada sin errores.
- Suite de tests mínima verde (unit + integración clave).

Notas rápidas de ejecución local
- Compilar: mvnw -DskipTests package
- Pruebas: mvnw test
- Variables DB: usar application.properties/application-dev.properties según entorno.
