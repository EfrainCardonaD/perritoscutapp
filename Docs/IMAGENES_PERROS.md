# Flujo de imágenes de perros (Frontend y Backend)

Este documento describe el flujo completo y actualizado del manejo de imágenes de perros: selección, previsualización, subida diferida, compresión automática, entrega vía proxy y resolución con fallback en Cloudinary.

---
## Objetivo
- Permitir que el usuario seleccione imágenes (drag&drop o file picker), las previsualice y solo las suba al confirmar el formulario.
- Aceptar archivos grandes (hasta 15MB) pero adaptarlos al límite del plan de Cloudinary (≈10MB) mediante compresión/reescalado automático a ~9MB.
- Servir las imágenes vía proxy evitando problemas CORS y controlando cabeceras de caché.
- Organizar los assets en carpetas Cloudinary usando `folder` y `asset_folder` y soportar fallback de resolución.

---
## Resumen del flujo
1. Usuario agrega imágenes (no se suben aún).  
2. Frontend genera previews locales (Object URL).  
3. Al guardar: sube cada imagen pendiente (POST `/api/imagenes/perritos`).  
4. El backend valida, preprocesa (si >9MB) y sube a Cloudinary.  
5. Devuelve `{ id, filename, size (post-compresión), url }`.  
6. Frontend crea el perro enviando los IDs.  
7. Visualización: GET `/api/imagenes/perritos/{id}` -> proxy con fallback (200 OK + bytes).  

---
## Preprocesamiento en backend
- Límite de entrada: 15MB (rechazo 422 si excede).  
- Umbral de compresión: si el archivo >9MB y es JPEG/PNG/WEBP se convierte a JPEG optimizado.  
- Estrategia: primero baja calidad (0.85→0.5), luego reduce escala (multiplicando 0.85) hasta aproximar ≤9MB (máx. 12 iteraciones, no baja de ~800px en ancho/alto).  
- Transparencia: se rasteriza a fondo blanco antes de JPEG.  
- GIF >9MB: rechazado (422) para no perder animación.  
- Resultado: garantiza que Cloudinary (plan con límite 10MB) acepte la imagen.  

---
## Endpoints relevantes
### Subir imagen de perro
POST `/api/imagenes/perritos` (multipart, campo `file`)
- Headers opcional: `X-Dog-Images-Count` (para cortar si ya está en el límite).  
- Validaciones:  
  - Archivo vacío -> 400.  
  - `size > 15MB` -> 422.  
  - Tipo no permitido -> 422.  
- Tipos permitidos: `image/jpeg, image/png, image/webp, image/gif` (GIF grande >9MB rechazado).  
- Respuesta OK: `{ id, filename, contentType, size, url }` (size tras compresión si aplicó).  
- Errores de Cloudinary `File size too large` -> 422 (mensaje amigable).  

### Obtener imagen
GET `/api/imagenes/perritos/{id}`  
- Si storage local: lee archivo físico (busca extensiones conocidas).  
- Si Cloudinary: genera lista de URLs candidatas:  
  1. `<folder>/<id>`  
  2. `<id>` (fallback por si el asset quedó sin carpeta)  
- Prueba secuencial; si todas fallan -> 404.  
- Cabeceras: `Cache-Control: public, max-age=31536000, immutable`.  

HEAD igual lógica sin cuerpo.

### Perfiles
GET/HEAD `/api/imagenes/perfil/{filename}` (redirige 302; control de autorización).  

---
## Frontend (puntos clave)
- Subida diferida: `ensureUploaded()` antes de crear/editar el perro.  
- Una request por imagen (evita sumar tamaños en multipart).  
- Validar localmente: tipo y tamaño ≤15MB.  
- Mostrar progreso y manejar 422 (tamaño, tipo, GIF grande).  
- Persistir solo IDs en el payload del perro:  
```json
{
  "imagenes": [
    { "id": "<uuid>", "descripcion": "...", "principal": true }
  ]
}
```

---
## Cloudinary (implementación actual)
- Uso de parámetros: `public_id = <uuid>`, `folder`, `asset_folder`, `overwrite = true`, `resource_type = image`.  
- La URL pública se genera siempre apuntando a `<folder>/<id>` con transformación `quality=auto, fetchFormat=auto`.  
- Fallback de resolución contempla que algunos assets pudieran haberse almacenado sin carpeta (migraciones previas o inconsistencia).  

### Variables de entorno sugeridas
- `CLOUDINARY_URL`  
- `CLOUDINARY_PERROS_FOLDER=perros` (o `perritos` según tu cuenta)  
- `CLOUDINARY_PERFILES_FOLDER=perfiles`  

---
## Reglas de negocio
- Máx. 5 imágenes por perro (validar cliente + servidor).  
- Exactamente una `principal=true`.  
- Eliminación de perro debe limpiar (opcionalmente) imágenes asociadas.  
- GIF grandes: sugerir conversión a MP4 / WebP fuera de alcance actual.  

---
## Códigos de estado (subida)
- 200: éxito.  
- 400: archivo vacío.  
- 422: tamaño >15MB, tipo no permitido, GIF >9MB, límite de imágenes, excede límite proveedor tras compresión.  
- 500: error inesperado (Cloudinary u otro no clasificable).  

---
## Errores y diagnóstico
| Situación | Síntoma | Acción |
|-----------|---------|--------|
| 413 en navegador + CORS | Nginx cortó antes (sin headers CORS) | Aumentar `client_max_body_size` (20m) y añadir headers `always` |
| 422 tamaño | Mensaje claro “supera el tamaño” | Reducir antes de subir |
| 422 GIF grande | GIF >9MB | Convertir a MP4/WebP o reducir |
| 404 al ver imagen | Proxy intenta URLs y falla | Revisar logs: carpeta vs public_id, variable `CLOUDINARY_PERROS_FOLDER` |
| 500 File size too large (viejo) | Cloudinary rechazaba >10MB | Ahora mitigado con preprocesado (verificar compresión) |

---
## Logs útiles (backend)
- Subida: `Cloudinary upload dog -> public_id=..., folder=..., asset_folder=..., url=...`  
- Proxy fallido: `Proxy imagen perro id=... url=... -> status=...`  
- fetchBinary error: `fetchBinary fallo code=... url=...`  

---
## Depuración rápida (actualizado)
1. ¿422 inesperado? Confirmar tamaño real (MB ≠ MiB).  
2. ¿404 tras subir? Ver carpeta y public_id en el log de subida + variable de entorno de folder.  
3. ¿413? Revisar Nginx (`client_max_body_size`) antes de backend.  
4. ¿Calidad baja? Imagen original era muy grande y pasó por varias iteraciones de compresión; ajustar umbral o permitir mayor calidad en código.  
5. ¿GIF grande rechazado? Convertir a otro formato antes.  

---
## Posibles mejoras futuras
- Generar miniaturas (p.ej. 400px) y servir según contexto.  
- Conversión automática de GIF a MP4/WebP animado (pipeline adicional).  
- Firma de URLs temporales si se hace acceso directo (sin proxy).  
- Reintentos exponenciales en el frontend para subidas intermitentes.  

---
## TL;DR
- Entrada hasta 15MB; si >9MB se recomprime/escala a ~9MB y se sube.  
- Cloudinary con folder + asset_folder, fallback de resolución (con y sin carpeta).  
- Proxy entrega bytes (no redirects) con caché larga y sin CORS problems.  
- Errores claros 422 para límites y tipos; 404 solo si no existe en ninguna URL candidata.
