# Flujo de imágenes de perros (Frontend y Backend)

Este documento explica cómo funciona el manejo de imágenes de perros en la app, por qué antes se subían de inmediato al arrastrar archivos y qué cambió para evitarlo. Incluye contratos de datos, endpoints, componentes y consejos de depuración.

---

## Objetivo
- Permitir que el usuario seleccione imágenes (drag&drop o file picker), las previsualice y solo las suba cuando confirme el formulario de creación/edición del perro.
- Servir las imágenes de forma segura evitando errores CORS, incluso usando cookies o Authorization en el frontend.

---

## Resumen del flujo actual

1) El usuario arrastra o selecciona imágenes en el formulario del perro.
2) El Frontend genera previews locales (Object URL) y NO sube todavía.
3) Al pulsar “Guardar”, el Frontend sube primero las imágenes pendientes (sin id) al backend y obtiene sus IDs.
4) El Frontend envía el payload de creación del perro con la lista de imágenes (id + metadatos).
5) Cuando el catálogo o las tarjetas necesitan mostrar imágenes, hacen GET a `/api/imagenes/perritos/{id}` y el backend devuelve la imagen (proxy) con 200 OK.

---

## Por qué antes se subían “solas” al arrastrar

- El componente `DogImagesEditor.vue` tenía una subida inmediata dentro del manejador de drag&drop y change del input (`uploadFiles(files)` -> POST `/imagenes/perritos`).
- Eso hacía que, al solo arrastrar, ya hubiera un POST a Cloudinary (vía backend) y luego, para construir la preview, un GET a `/api/imagenes/perritos/{id}` que respondía 302 (redirect a la CDN). Como el cliente usa `withCredentials: true`, el navegador bloqueaba la redirección cross-origin con `Access-Control-Allow-Origin: *` y la página podía fallar.

### Cambios realizados
- Frontend: ahora hay “staging” local (solo preview) y no se sube nada hasta que el usuario pulsa Guardar.
- Backend: el endpoint de obtención de imágenes ahora actúa como proxy (200 OK), evitando la redirección 302 y el problema CORS con `withCredentials`.

---

## Frontend

### Librería HTTP
- Archivo: `front/src/utils/api.js`
- `axios.create({ baseURL, withCredentials: true })`
- Con `withCredentials` activo, las redirecciones 302 hacia otros orígenes pueden ser bloqueadas por CORS. Por eso se usa proxy en el backend.

### Componentes clave

1) `DogImagesEditor.vue`
   - Archivo: `front/src/components/perros/DogImagesEditor.vue`
   - Antes: subía cada archivo justo al arrastrarlo/seleccionarlo.
   - Ahora: 
     - Añade cada archivo a una lista local con `objectUrl` (preview) y marca `file` pendiente.
     - Expone `ensureUploaded()` para subir en diferido todas las imágenes pendientes (las que aún no tienen `id`).
     - Emite el `v-model` con los campos relevantes: `{ id, descripcion, principal, file?, objectUrl?, tempId? }`.

2) `DogForm.vue`
   - Archivo: `front/src/components/perros/DogForm.vue`
   - En `onSubmit()` llama a `imagesEditorRef.ensureUploaded()` antes de construir el payload.
   - Solo envía al backend `imagenes` con IDs válidos: `[ { id, descripcion, principal } ]`.
   - Valida que exista exactamente una imagen marcada como principal.

3) Lectura/visualización
   - `DogCard.vue`, `DogReadModal.vue` piden la imagen como blob a `/api/imagenes/perritos/{id}`.
   - Al ser proxy (200 OK) ya no hay redirecciones que el navegador bloquee.

### Contrato de payload desde Frontend
- Endpoint: `POST /api/perros`
- Cuerpo (ejemplo):
```json
{
  "nombre": "Toby",
  "edad": 3,
  "sexo": "Macho",
  "tamano": "Mediano",
  "raza": "Mestizo",
  "descripcion": "Juguetón y sociable",
  "ubicacion": "CDMX",
  "imagenes": [
    { "id": "<uuid-subido>", "descripcion": "Jugando", "principal": true },
    { "id": "<uuid-subido>", "descripcion": "Dormido", "principal": false }
  ]
}
```

### Errores comunes y consejos
- “Debes subir al menos una imagen válida”: ocurre si el usuario no seleccionó imágenes o si no logró subirse ninguna al confirmar.
- Tamaño > 5MB o tipo no permitido: se valida en Frontend y se vuelve a validar en Backend.
- Si falla `ensureUploaded()`, el formulario muestra un error y no envía el perro hasta que se resuelva.

---

## Backend

### Endpoints
- `POST /api/imagenes/perritos` (multipart)
  - Sube la imagen a Cloudinary (o al storage configurado) y devuelve: `{ id, filename, contentType, size, url }`.
  - Valida tipo y tamaño (permitidos: JPEG, PNG, WEBP, GIF; hasta 5MB para perros).

- `GET /api/imagenes/perritos/{id}`
  - ANTES: 302 redirect a la CDN.
  - AHORA: Proxy a la URL pública, respondiendo 200 OK con los bytes. Incluye `Cache-Control: public, max-age=31536000, immutable`.
  - Beneficio: evita CORS con `withCredentials`.

- `HEAD /api/imagenes/perritos/{id}`
  - Proxy HEAD para verificar existencia/headers sin descargar el cuerpo.

- `GET /api/imagenes/perfil/{filename}` (perfil de usuario)
  - Mantiene 302 (redirige a la CDN). Las imágenes de perfil se muestran típicamente en `<img>` y no con XHR; si se requiriera blob + credenciales, aplicar el mismo enfoque de proxy.

### Seguridad y CORS
- Configuración en `SecurityConfiguration.java`:
  - Rutas públicas/seguras ajustadas; imágenes de perritos requieren autenticación según la necesidad del catálogo.
  - CORS: orígenes permitidos definidos en `app.cors.allowed-origins` (sin barra final), `allowCredentials=true`.
- Con `withCredentials=true` en Frontend, las redirecciones 302 a dominios externos generan bloqueos si el servidor externo usa `*` en `Access-Control-Allow-Origin`. El proxy elimina esa situación.

### Storage
- `StorageConfig` permite usar proveedor `cloudinary` o `local`.
- Cloudinary: 
  - Subidas con recorte cuadrado (`aspectRatio 1:1`) y `gravity auto`.
  - Generación de URL pública segura (https) para cada imagen por `id`.
- Local: útil para tests/desarrollo (escribe en `uploads/perritos/`).

### Rendimiento
- Proxy de imágenes hace que el backend transfiera bytes; a cambio elimina CORS y simplifica seguridad.
- Mitigación: cache robusto en el cliente (`Cache-Control`), y Cloudinary también sirve caché aguas arriba.
- Alternativa (si se desea volver a 302): usar `<img src="https://res.cloudinary.com/...">` directamente o desactivar `withCredentials` en las peticiones de imagen. Requiere revisar permisos/privacidad.

---

## Secuencia (actual)

1) Usuario arrastra dos imágenes.
2) Frontend crea 2 `objectUrl` locales y marca una como principal.
3) Usuario pulsa Guardar.
4) Frontend hace `ensureUploaded()`:
   - POST `/api/imagenes/perritos` por cada imagen sin `id`.
   - Sustituye `file` por `id` recibido.
5) Frontend arma payload con `[ { id, descripcion, principal } ]` y hace `POST /api/perros`.
6) Para mostrar, `GET /api/imagenes/perritos/{id}` -> Backend proxy -> bytes 200 OK.

---

## Depuración rápida
- ¿Se suben antes de guardar? Ver `DogImagesEditor.vue`: no debe llamarse a POST en drag&drop, solo en `ensureUploaded()`.
- ¿CORS en consola del navegador? Asegurarse de que el GET a `/api/imagenes/perritos/{id}` responde 200 (no 302) y que axios usa la baseURL del backend.
- ¿Imágenes demasiado grandes? Revisar mensajes y restricciones de 5MB.
- ¿Sin “principal”? El formulario obliga a exactamente una imagen principal.

---

## Siguientes mejoras (opcionales)
- Botón “Subir ahora” para usuarios con red lenta (subidas escalonadas y feedback de progreso).
- Reintentos y cancelación de subidas.
- Miniaturas generadas en el backend para acelerar catálogos (transformaciones de Cloudinary con tamaños fijos).
- Límite de número de imágenes por perro.

---

## Archivos implicados
- Frontend
  - `front/src/components/perros/DogImagesEditor.vue`
  - `front/src/components/perros/DogForm.vue`
  - `front/src/components/DogCard.vue`
  - `front/src/components/DogReadModal.vue`
  - `front/src/utils/api.js`
- Backend
  - `src/main/java/com/cut/cardona/controllers/api/ImagenController.java`
  - `src/main/java/com/cut/cardona/security/SecurityConfiguration.java`
  - `src/main/java/com/cut/cardona/infra/storage/**`
  - `src/main/resources/application.properties` (CORS, storage)

---

## TL;DR
- Antes: subida inmediata al arrastrar + 302 a CDN => CORS con `withCredentials` y crash.
- Ahora: subida diferida al Guardar + GET proxy 200 OK => sin CORS y mejor UX.

