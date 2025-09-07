# Fase 4 — Plan de Implementación Backend (Base de datos y Migraciones)

Objetivo
- Fortalecer integridad y performance, y soportar recuperación de contraseña.

Tareas Flyway (borradores)
- [ ] Charset/collation utf8mb4_unicode_ci en todo el esquema.
- [ ] CHECK constraints (MySQL 8+): perros.estado_revision, perros.estado_adopcion, solicitudes_adopcion.estado.
- [ ] Índices:
  - [ ] perros(estado_revision, estado_adopcion, fecha_publicacion)
  - [ ] solicitudes_adopcion(solicitante_id, estado, fecha_solicitud)
- [ ] Fulltext index: perros.descripcion, perros.raza
- [ ] Auditoría: created_by, updated_by, fecha_actualizacion (default/trigger)
- [ ] Tabla tokens_reset(id, usuario_id, token, expiracion, usado)

Archivos Flyway sugeridos
- Vxxx__enums_y_checks.sql
- Vxxx__indices_busqueda.sql
- Vxxx__auditoria.sql
- Vxxx__tokens_reset.sql

Criterios de aceptación
- flyway migrate corre en local/CI sin errores (Testcontainers si aplica).
- Integridad y performance mejoradas sin romper contratos API.

Riesgos
- Cambios de esquema → ventana baja, backups y plan de rollback.
