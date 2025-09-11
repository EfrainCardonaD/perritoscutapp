# Proceso completo para crear un perro y subir su(s) imagen(es)

Este documento describe el flujo integral para crear un perro en la aplicación, incluyendo la subida y asociación de imágenes, tanto desde el frontend como el backend. Está orientado a desarrolladores que implementan el uploader y el formulario de alta de perros.

---

## Objetivo
- Permitir que el usuario seleccione imágenes (drag&drop o file picker), las previsualice y solo las suba cuando confirme el formulario de creación/edición del perro.
- Asociar las imágenes subidas al nuevo perro en el backend.
- Servir las imágenes de forma segura y eficiente, evitando problemas de CORS y garantizando la organización en el storage (Cloudinary o local).

---

## Flujo resumido

1. El usuario selecciona imágenes en el formulario de alta de perro.
2. El frontend genera previews locales (Object URL) y NO sube todavía.
3. Al pulsar “Guardar”, el frontend sube primero las imágenes pendientes (sin id) al backend y obtiene sus IDs.
4. El frontend envía el payload de creación del perro con la lista de imágenes (id + metadatos).
5. El backend crea el perro y asocia las imágenes por id.
6. Para mostrar imágenes, el frontend pide `/api/imagenes/perritos/{id}` y el backend responde con la imagen (proxy 200 OK).

---

## Detalle del proceso

### 1. Selección y previsualización de imágenes (Frontend)
- El usuario arrastra o selecciona archivos en el formulario.
- El componente (`DogImagesEditor.vue`) añade cada archivo a una lista local con `objectUrl` (preview) y marca el archivo como pendiente de subida.
- El usuario puede marcar una imagen como principal y añadir descripciones.

### 2. Subida diferida de imágenes (Frontend)
- Al pulsar “Guardar”, el frontend llama a `ensureUploaded()`:
  - Por cada imagen sin id, hace POST `/api/imagenes/perritos` (multipart/form-data, campo `file`).
  - Recibe en la respuesta: `{ id, filename, contentType, size, url }`.
  - Sustituye el campo `file` por el `id` recibido.

### 3. Creación del perro y asociación de imágenes (Frontend)
- El frontend arma el payload:
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
- Envía POST `/api/perros` con el payload.

### 4. Persistencia y reglas de negocio (Backend)
- El backend valida:
  - Que los ids de imagen existan y no excedan el límite (máx. 5 por perro).
  - Que haya una imagen principal.
  - Que los tipos y tamaños sean válidos (máx. 15MB, tipos permitidos: JPEG, PNG, WEBP, GIF).
- Persiste el perro y asocia las imágenes.
- Si alguna imagen no se puede asociar, puede eliminarla del storage para evitar huérfanos.

### 5. Visualización de imágenes (Frontend y Backend)
- Para mostrar imágenes, el frontend pide `/api/imagenes/perritos/{id}`.
- El backend responde con la imagen (proxy 200 OK, no 302), evitando problemas de CORS.
- Se añaden headers de caché: `Cache-Control: public, max-age=31536000, immutable`.

---

## Endpoints involucrados

- `POST /api/imagenes/perritos` (subida de imagen)
  - multipart/form-data, campo `file`.
  - Responde: `{ id, filename, contentType, size, url }`.
- `POST /api/perros` (creación de perro)
  - JSON con datos del perro y array de imágenes (ids).
- `GET /api/imagenes/perritos/{id}` (visualización)
  - Devuelve la imagen como blob (200 OK).

---

## Ejemplo de integración (Frontend)

```js
// Subir imagen
const fd = new FormData();
fd.append('file', file);
fetch('/api/imagenes/perritos', { method: 'POST', body: fd })
  .then(r => r.json())
  .then(resp => { /* usar resp.data.id para asociar al perro */ });

// Crear perro
fetch('/api/perros', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(payload)
});
```

---

## Errores comunes y consejos
- “Debes subir al menos una imagen válida”: ocurre si el usuario no seleccionó imágenes o si no logró subirse ninguna al confirmar.
- Tamaño > 15MB o tipo no permitido: se valida en Frontend y Backend.
- Si falla `ensureUploaded()`, el formulario muestra un error y no envía el perro hasta que se resuelva.
- El backend valida que no se exceda el límite de imágenes y que haya una principal.

---

## Seguridad y CORS
- El backend actúa como proxy para imágenes, evitando redirecciones 302 y problemas con `withCredentials`.
- CORS configurado en backend para orígenes permitidos.

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
- El usuario selecciona imágenes, las previsualiza y solo las sube al guardar.
- El frontend sube las imágenes, recibe los ids y crea el perro asociando esos ids.
- El backend valida, persiste y sirve las imágenes vía proxy.
- Sin CORS, sin redirecciones, con organización en el storage.

