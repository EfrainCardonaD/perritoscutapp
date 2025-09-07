# Integración de Cloudinary para gestión de imágenes

Este documento explica qué se implementó, por qué, y cómo usar la nueva integración de imágenes con Cloudinary. Además, incluye cómo forzar el uso de Cloudinary al 100% y retirar cualquier dependencia operativa de almacenamiento local.

## Qué se implementó

- Factory/servicio único de almacenamiento:
  - `StorageConfig` produce el bean `ImageStorageService`.
  - Proveedor por defecto: Cloudinary.
- Implementación Cloudinary:
  - `CloudinaryImageStorageService` sube imágenes con recorte cuadrado (gravity=auto, crop=fill, aspectRatio=1:1).
  - Validaciones de tamaño y tipo:
    - Perfil: ≤ 15 MB.
    - Perros: ≤ 5 MB.
    - Tipos: JPEG, PNG, WEBP, GIF.
  - Métodos para resolver URL pública de imágenes de perro y perfil.
- Refactor de controladores/servicios:
  - `ImagenController`: deja de devolver archivos locales; ahora siempre redirige a la URL pública (CDN de Cloudinary). Endpoint para subir imágenes de perro.
  - `PerfilUsuarioService`: sube a Cloudinary, guarda `urlPublica` y usa esa URL como `rutaArchivo` (para cumplir columnas NOT NULL existentes). Dejó de escribir en disco.
  - `PerroService`: elimina verificación de archivos locales, guarda URL pública de Cloudinary y limita a máx. 5 imágenes por perro.
- Configuración por propiedades:
  - `application.properties` define Cloudinary y lo usa por defecto.

Archivos clave modificados/añadidos:
- `src/main/java/com/cut/cardona/infra/storage/StorageConfig.java`
- `src/main/java/com/cut/cardona/infra/storage/ImageStorageService.java`
- `src/main/java/com/cut/cardona/infra/storage/cloudinary/CloudinaryImageStorageService.java`
- `src/main/java/com/cut/cardona/controllers/api/ImagenController.java`
- `src/main/java/com/cut/cardona/controllers/service/PerfilUsuarioService.java`
- `src/main/java/com/cut/cardona/controllers/service/PerroService.java`
- `src/main/resources/application.properties`

## Por qué

- Centralizar la gestión de imágenes en un proveedor cloud (CDN) para:
  - Mejor latencia y disponibilidad de imágenes.
  - Transformaciones en tiempo de entrega (recorte cuadrado) y URLs firmadas si se requiere.
  - Eliminar mantenimiento y riesgo de almacenamiento local.

## Cómo usarlo

1) Variables de entorno (recomendado en todos los entornos):
- `APP_STORAGE_PROVIDER=cloudinary` (por defecto ya es cloudinary)
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`
- Opcionales:
  - `CLOUDINARY_PERROS_FOLDER` (default: `perritos`)
  - `CLOUDINARY_PERFILES_FOLDER` (default: `perfiles`)

2) Endpoints disponibles:
- Subir imagen de perro:
  - `POST /api/imagenes/perritos` (multipart/form-data, key: `file`)
  - Respuesta contiene: `id`, `filename`, `url`, `contentType`, `size`.
- Consumir imagen de perro por id (redirige 302 a CDN):
  - `GET /api/imagenes/perritos/{id}`
  - `HEAD /api/imagenes/perritos/{id}`
- Consumir imagen de perfil por nombre de archivo (compatibilidad, redirige a CDN):
  - `GET /api/imagenes/perfil/{filename}`
  - `HEAD /api/imagenes/perfil/{filename}`
- Actualizar imagen de perfil de usuario autenticado:
  - `POST /api/perfil/imagen` (multipart/form-data, key: `imagen`)

3) Reglas y límites:
- Recorte cuadrado automático para todas las imágenes (gravity=auto, crop=fill, 1:1).
- Tamaño máximo:
  - Perfil: ≤ 15 MB.
  - Perros: ≤ 5 MB por imagen.
- Cantidad máxima de imágenes por perro: 5.

4) Asociación de imágenes a perros:
- El flujo es de dos pasos:
  - Subir imagen con `POST /api/imagenes/perritos` y obtener `id`.
  - En `CrearPerroRequest.imagenes[]` enviar los `id` devueltos (y marcar una como principal true).
- El sistema resolverá y guardará la URL pública en BD.

## Cómo forzar “al 100%” el uso de Cloudinary

“Forzar al 100%” significa que todas las rutas de lectura/escritura de imágenes usan Cloudinary, y no queda dependencia operativa del disco local.

Pasos:
- Dejar `APP_STORAGE_PROVIDER=cloudinary` (ya es el valor por defecto). Si se desea, eliminar del `application.properties` cualquier referencia de `app.storage.perros-dir` o similares.
- No utilizar ni referenciar endpoints/paths locales para servir archivos; el `ImagenController` ya redirige siempre a URLs de Cloudinary.
- (Opcional) Borrar el directorio `uploads/` si existía por versiones anteriores. No se escribe nada nuevo allí.
- (Opcional) Remover el proveedor local del classpath si se quiere endurecer aún más (no requerido). Actualmente no se activa salvo que se cambie la propiedad del provider.

## Migración y limpieza

- Las imágenes nuevas se alojan únicamente en Cloudinary.
- Rutas locales existentes (`uploads/`) ya no se usan. Se pueden conservar para auditoría o eliminar tras backup.

## Troubleshooting

- 404/errores al resolver URL pública:
  - Confirmar que el `id` exista en Cloudinary (el `id` es el UUID generado al subir, no el filename con extensión).
  - Confirmar credenciales y carpetas (`perros_folder`, `perfiles_folder`).
- 400 al subir:
  - Verificar tipos: JPEG/PNG/WEBP/GIF.
  - Verificar tamaños: perfil ≤ 15 MB; perro ≤ 5 MB.
- Límite de 5 imágenes por perro:
  - Si se envían más de 5 en la creación, se rechaza con error.

## Próximos pasos (opcionales)

- Borrado remoto en Cloudinary cuando se elimine un perro/perfil (lifecycle hooks).
- Usar URLs firmadas/transformaciones adicionales según contexto (thumbnails, retina, etc.).
- Métricas y logs específicos para la capa de almacenamiento.

