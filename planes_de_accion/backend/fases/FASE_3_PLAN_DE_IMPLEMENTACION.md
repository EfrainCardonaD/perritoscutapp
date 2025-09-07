# Fase 3 — Plan de Implementación Backend (Almacenamiento e Imágenes)

Objetivo
- Preparar la abstracción de almacenamiento (S3/MinIO/local) y robustecer reglas de imágenes sin bloquear el front.

Tareas
- [ ] Interfaz StorageService (putObject, getObjectUrl, deleteObject) y DTO (urlPublica, rutaInterna, mime, size).
- [ ] Implementaciones: MinIOAdapter y StubLocalAdapter; selección por propiedad/flag.
- [ ] Validaciones: mime permitido (jpeg/png/webp), tamaño máximo configurable.
- [ ] Pre-signed URLs (opcional, por defecto off) con expiración corta.
- [ ] Servicio de imágenes de perro: garantizar exactamente una principal de forma transaccional.
- [ ] Documentación: ejemplos en JSON_EXAMPLES y sección en API_DOCUMENTATION.

Criterios de aceptación
- Con storage=local o minio activo, se obtienen URLs públicas seguras.
- Regla de una imagen principal aplicada desde servicio (idempotente).
- Configuración por variables/env sin exponer credenciales.

Riesgos
- Dependencias nativas/compatibilidad → cliente oficial S3/MinIO, detrás de interfaz.
- Coste/tiempo → comenzar con StubLocalAdapter y pruebas unitarias.

