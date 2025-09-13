# API Documentation — Perritos Cut

Base URL: http://localhost:8080/api
Autenticación: JWT Bearer Token (Authorization: Bearer <token>)
Content-Type: application/json

1) Autenticación
- POST /login
  Request:
  {
    "userName": "johndoe",
    "password": "Password123!"
  }
  Response 200: { token, refreshToken, expiresIn, usuario{ id, userName, email, rol } }

- POST /refresh
  Header: Authorization: Bearer <refresh_token>
  Response 200: { token, expiresIn }

- POST /logout
  Header: Authorization: Bearer <token>
  Response 200: vacío

2) Imágenes
- POST /imagenes/perritos (multipart/form-data)
  Sube una imagen de perro y devuelve su id (UUID) para asociarla.
  Request (form-data): file=<archivo imagen>
  Response 200:
  { "id":"<uuid>", "filename":"<uuid>.jpg", "url":"/api/imagenes/perritos/<uuid>", "contentType":"image/jpeg", "size": 12345 }

- GET /imagenes/perritos/{id}
  Devuelve la imagen por id.

- HEAD /imagenes/perritos/{id}
  Verifica existencia sin descargar.

- DELETE /imagenes/perritos/{id}
  Elimina imagen suelta no asociada a un perro (idempotente). Respuestas:
  200: eliminada o inexistente
  422: asociada a un perro
  400: UUID inválido

- GET /imagenes/perfil/{filename}
- HEAD /imagenes/perfil/{filename}
  Devuelve / verifica imagen de perfil activa (autorizado dueño o ADMIN). Redirige a URL pública.

Nota: Las imágenes de perros se sirven por transformación Cloudinary (quality=auto, format=auto) en modo cloud; local usa disco.

3) Perros
- GET /perros/catalogo (público)
  Query params: sexo, tamano, ubicacion, page, size
  Response 200: [DtoPerro]
  DtoPerro:
  {
    id, nombre, edad, sexo, tamano, raza, descripcion, ubicacion,
    estadoAdopcion, estadoRevision, usuarioId,
    imagenPrincipalId,            // id de imagen principal
    imagenIds                     // lista de ids de imágenes
  }

- GET /perros/mis (auth USER/ADMIN/REVIEWER)
  Response 200: [DtoPerro]

- GET /perros/{id} (público si aprobado y disponible, actualmente sin restricción adicional)
  Response 200: DtoPerro, 404 si no existe

- POST /perros (rol: USER)
  Body:
  {
    "nombre":"Firulais",
    "edad":3,
    "sexo":"Macho",
    "tamano":"Mediano",
    "raza":"Mestizo",
    "descripcion":"Juguetón y cariñoso",
    "ubicacion":"CDMX",
    "imagenes":[
      {"id":"<uuid-subido>", "descripcion":"principal", "principal":true}
    ]
  }
  Respuestas: 201 OK, 422 validación negocio, 400 validación datos.

- PATCH /perros/{id} (owner o ADMIN/REVIEWER)
  Body (parcial, pero siempre debe incluir lista completa de imágenes resultantes):
  {
    "nombre":"Opcional",
    "imagenes":[
      {"id":"<uuid-existente-o-nuevo>", "principal":true, "descripcion":"opcional"},
      {"id":"<uuid2>", "principal":false}
    ]
  }
  Reglas:
   * Máx 5 imágenes
   * Exactamente 1 principal
   * Nuevas imágenes deben existir previamente (subidas)
   * Imágenes omitidas se eliminan (borrado diferido en Cloudinary after commit)
  Respuestas: 200 OK, 422 reglas, 403 no autorizado, 404 no encontrado.

- DELETE /perros/{id} (owner o ADMIN/REVIEWER)
  Borrado total del registro + imágenes (cascade JPA) y borrado diferido en Cloudinary.
  Respuestas: 200 OK, 403, 404.

- POST /admin/perros/{id}/aprobar (ADMIN/REVIEWER)
- POST /admin/perros/{id}/rechazar (ADMIN/REVIEWER)
- PATCH /admin/perros/{id}/estado?estado=Disponible|Adoptado|Pendiente|No disponible (ADMIN/REVIEWER)
  Reglas: Disponible solo si estadoRevision = Aprobado

4) Perfil de Usuario
- GET /perfil/completo (auth)
  Devuelve DtoPerfilCompleto del usuario autenticado.

- PATCH /perfil (auth)
  Actualiza campos: nombreReal, telefono, idioma, zonaHoraria, fechaNacimiento.
  Reglas: teléfono único; fechaNacimiento >= 15 años.
  Respuestas: 200 OK, 400 validación, 401 no autenticado.

- POST /perfil/imagen (multipart/form-data, auth)
  Sube o reemplaza imagen de perfil. Desactiva la anterior y la borra en Cloudinary (best-effort). Campos form: imagen=<archivo>.
  Respuestas: 200 OK, 400 validación, 401, 500 error.

- GET /perfil/usuario/{usuarioId} (ADMIN) — Obtener perfil de otro usuario.

5) Usuarios
- GET /usuarios/me/resumen (auth) — Resumen básico.
- DELETE /usuarios/admin/{id} (ADMIN)
  Soft delete: usuario.activo=false. No se elimina imagen ni datos históricos.
  Respuestas: 200 OK (Usuario desactivado / ya inactivo), 404 si no existe.

6) Adopciones
- POST /solicitudes (rol: USER)
- POST /solicitudes/{id}/documentos (rol: USER)
- GET /solicitudes/mis (rol: USER)
- GET /admin/solicitudes/pendientes (ADMIN/REVIEWER)
- PATCH /admin/solicitudes/{id}/estado?estado=En revisión|Aceptada|Rechazada|Cancelada (ADMIN/REVIEWER)

- GET /solicitudes/{id} (rol: USER o ADMIN/REVIEWER — propietario o roles con privilegios)
  Response 200: DtoSolicitud

- DELETE /solicitudes/{id} (propietario o ADMIN)
  Response 200: eliminado / 403 no autorizado / 404 no encontrado

(Pendientes opcionales: Job purga de huérfanas / diagnóstico)

7) Estados (enums)
- Perro.estadoRevision: Pendiente | Aprobado | Rechazado
- Perro.estadoAdopcion: Disponible | Adoptado | Pendiente | No disponible
- Solicitud.estado: Pendiente | En revisión | Aceptada | Rechazada | Cancelada

8) Validaciones principales (400 / 422)
- CrearPerroRequest: nombre; edad 0..25; sexo/tamano válidos; ubicacion <=255; imágenes 1..5; 1 principal.
- ActualizarPerroRequest: misma regla de imágenes (lista completa resultante).
- Imagen subida: <=15MB entrada; Cloudinary comprimido a ~9MB objetivo.
- Perfil: teléfono único; fechaNacimiento >= 15 años.

9) Seguridad y roles
- Público: /login, /refresh, registro, GET/HEAD imágenes, catálogo perros.
- Protegido: resto (JWT).
- Roles: ADMIN, REVIEWER, USER (aplicadas con @PreAuthorize).

10) Notas Técnicas
- Borrado diferido: imágenes eliminadas en PATCH/DELETE perro se borran en Cloudinary after commit (TransactionSynchronization.afterCommit).
- Eliminación idempotente de imágenes sueltas: DELETE /imagenes/perritos/{id} no falla si ya no existe.
- Soft delete usuario: isEnabled() refleja activo=false -> autenticación futura bloqueada.
- Transformaciones Cloudinary: quality=auto, format=auto; URL derivada con public_id = <folder>/<uuid>.
- Evitar almacenar transformaciones en BD; se guarda URL resoluble por consistencia histórica.

11) Errores comunes
- 400: Formato de UUID inválido, validaciones Bean Validation.
- 401: Token ausente/expirado.
- 403: Falta de rol o no owner.
- 404: Recurso inexistente.
- 422: Reglas de negocio (límite imágenes, falta principal, estado inválido, imagen asociada en borrado suelto).

12) Ejemplos CURL
- Crear perro:
  curl -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
       -d '{"nombre":"Firulais","sexo":"Macho","tamano":"Mediano","imagenes":[{"id":"UUID","principal":true}]}' \
       http://localhost:8080/api/perros

- Patch perro (cambiar principal):
  curl -X PATCH -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
       -d '{"imagenes":[{"id":"UUID1","principal":false},{"id":"UUID2","principal":true}]}' \
       http://localhost:8080/api/perros/{id}

- Eliminar perro:
  curl -X DELETE -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/perros/{id}

- Patch perfil:
  curl -X PATCH -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
       -d '{"nombreReal":"Nuevo Nombre","telefono":"+5215550001111"}' \
       http://localhost:8080/api/perfil

- Soft delete usuario (ADMIN):
  curl -X DELETE -H "Authorization: Bearer $ADMIN_TOKEN" http://localhost:8080/api/usuarios/admin/{id}

13) Futuro / Opcional
- Job programado de purga de imágenes huérfanas >24h sin asociar
- Endpoint diagnóstico de orphans

---
## 14) Adopciones (Separación por Roles y Nuevos Endpoints)

Reorganización de endpoints de adopciones separando claramente responsabilidades de usuarios y administradores/revisores.

### 14.1 Endpoints Usuario (ROLE_USER)
Base: /api/usuario/adopciones/solicitudes
- POST /  Crear solicitud
- POST /{id}/documentos  Subir documento (Identificacion|CartaResponsiva)
- GET /mis  Listar solicitudes propias
- GET /{id}  Ver detalle (solo propietario o roles privilegiados)
- PATCH /{id}/mensaje  Actualizar mensaje si estado = Pendiente
- POST /{id}/cancelar  Cambiar estado a Cancelada (no permitido si ya Aceptada/Rechazada)
- DELETE /{id}  Eliminar (solo si Pendiente, salvo ADMIN)

### 14.2 Endpoints Admin/Reviewer (ROLE_ADMIN / ROLE_REVIEWER)
Base: /api/admin/adopciones/solicitudes
- GET /pendientes  Listar solicitudes en estado Pendiente
- GET ?estado=&perroId=&solicitanteId=  Búsqueda filtrada (todos los estados)
- PATCH /{id}/estado?estado=En revisión|Aceptada|Rechazada  Actualizar estado (validaciones documentos y disponibilidad)
- POST /{id}/revertir  Revertir adopción (solo si la solicitud estaba Aceptada) ->
  Efectos:
   * Solicitud pasa a Rechazada
   * Perro vuelve a Disponible (si estaba Adoptado y sigue Aprobado)

### 14.3 Flujo de Aceptación y Reversión
1. Aceptar una solicitud:
   - Valida perro Aprobado + Disponible
   - Requiere documentos (Identificacion y CartaResponsiva)
   - Marca perro como Adoptado
   - Rechaza automáticamente otras solicitudes Pendiente / En revisión del mismo perro
2. Revertir adopción:
   - Solo ADMIN/REVIEWER
   - Solicitud Aceptada -> Rechazada
   - Perro Adoptado -> Disponible (si permanece Aprobado)

### 14.4 Reglas de Negocio Relevantes
- No se puede aceptar si ya existe otra Aceptada
- No se puede volver a Disponible un perro no Aprobado
- Cancelar por usuario inválido si ya está Aceptada o Rechazada
- No se puede revertir una solicitud que no esté Aceptada

### 14.5 Ejemplos CURL Adopciones Nuevos
- Crear solicitud (usuario):
  curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
       -d '{"perroId":"<perro_uuid>","mensaje":"Quiero adotarlo"}' \
       http://localhost:8080/api/usuario/adopciones/solicitudes

- Subir documento:
  curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
       -d '{"tipoDocumento":"Identificacion","urlDocumento":"https://...","nombreArchivo":"id.pdf"}' \
       http://localhost:8080/api/usuario/adopciones/solicitudes/{solicitudId}/documentos

- Aceptar solicitud (admin/reviewer):
  curl -X PATCH -H "Authorization: Bearer $ADMIN" \
       "http://localhost:8080/api/admin/adopciones/solicitudes/{solicitudId}/estado?estado=Aceptada"

- Revertir adopción:
  curl -X POST -H "Authorization: Bearer $ADMIN" \
       http://localhost:8080/api/admin/adopciones/solicitudes/{solicitudId}/revertir

---
Última actualización: Añadida separación de controladores adopciones y endpoint de reversión.
