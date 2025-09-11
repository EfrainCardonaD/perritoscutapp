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
  Sube una imagen local de perro y devuelve su id (UUID) para asociarla.
  Request (form-data): file=<archivo imagen>
  Response 200:
  { "id":"<uuid>", "filename":"<uuid>.jpg", "url":"/api/imagenes/perritos/<uuid>", "contentType":"image/jpeg", "size": 12345 }

- GET /imagenes/perritos/{id}
  Devuelve la imagen por id (sirve el archivo de uploads/perritos/<id>.*)

- HEAD /imagenes/perritos/{id}
  Verifica existencia sin descargar.

- GET /imagenes/perfil/{filename}
- HEAD /imagenes/perfil/{filename}
  ...existing code...

3) Perros
- GET /perros/catalogo (público)
  Query params: sexo, tamano, ubicacion, page, size
  Response 200: [DtoPerro]
  DtoPerro:
  {
    id, nombre, edad, sexo, tamano, raza, descripcion, ubicacion,
    estadoAdopcion, estadoRevision, usuarioId,
    imagenPrincipalId,            // nuevo: id de imagen principal si existe
    imagenIds                     // nuevo: lista de ids de imágenes
  }

- GET /perros/mis (auth)
  Response 200: [DtoPerro]

- POST /perros (rol: USER)
  Body (validado):
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
  Response 200: DtoPerro
  Errores: 422 si el id no existe en almacenamiento local; 400 validación.

- POST /admin/perros/{id}/aprobar (roles: ADMIN, REVIEWER)
  Regla: requiere 1 imagen principal
  Respuestas: 200 OK, 422 si sin imagen principal

- POST /admin/perros/{id}/rechazar (roles: ADMIN, REVIEWER)
  200 OK

- PATCH /admin/perros/{id}/estado?estado=Disponible|Adoptado|Pendiente|No disponible (roles: ADMIN, REVIEWER)
  Regla: "Disponible" solo si estadoRevision = "Aprobado"
  Respuestas: 200 OK, 422 si regla incumplida

4) Adopciones
- POST /solicitudes (rol: USER)
  Body (validado): { "perroId":"<uuid>", "mensaje":"opcional" }
  Reglas: no auto-solicitar; perro APROBADO + DISPONIBLE
  Respuestas: 200 OK, 422 si reglas incumplidas

- POST /solicitudes/{id}/documentos (rol: USER)
  Body (validado): { "tipoDocumento":"Identificacion|CartaResponsiva", "urlDocumento":"...", "nombreArchivo":"...", "tipoMime":"...", "tamanoBytes": 123 }
  Respuestas: 200 OK, 400 validación

- GET /solicitudes/mis (rol: USER)
  200 OK: [DtoSolicitud]
  DtoSolicitud: { id, perroId, solicitanteId, estado, mensaje, fechaSolicitud, fechaRespuesta }

- GET /admin/solicitudes/pendientes (roles: ADMIN, REVIEWER)
  200 OK: [DtoSolicitud] (estado = "Pendiente")

- PATCH /admin/solicitudes/{id}/estado?estado=En revisión|Aceptada|Rechazada|Cancelada (roles: ADMIN, REVIEWER)
  Reglas:
  - En revisión/Aceptada: requiere 2 tipos de documentos cargados
  - Aceptada: perro debe estar APROBADO + DISPONIBLE y no debe existir otra aceptada del mismo perro
  Respuestas: 200 OK, 422 si faltan documentos o perro no disponible, 409 si ya existe aceptada

5) Estados (enums)
- Perro.estadoRevision: Pendiente | Aprobado | Rechazado
- Perro.estadoAdopcion: Disponible | Adoptado | Pendiente | No disponible
- Solicitud.estado: Pendiente | En revisión | Aceptada | Rechazada | Cancelada

6) Validaciones principales (400 Bad Request)
- CrearPerroRequest: nombre required; edad 0..25; sexo/tamano en lista; ubicacion <=255; imagenes not empty; Imagen.url required <=500
- CrearSolicitudRequest: perroId required UUID; mensaje <=2000
- DocumentoRequest: tipoDocumento/urlDocumento required; límites de longitud y tamaño >=0
- Filtros de catálogo: sexo/tamano valores válidos; page>=0; size 1..100

7) Seguridad y roles
- Público: POST /login, POST /refresh, registro, GET/HEAD /imagenes/perfil/**, GET/HEAD /imagenes/perritos/**, GET /perros/catalogo, swagger y actuator.
- Protegido: resto.

8) Notas
- Almacén local de imágenes de perros: uploads/perritos/{uuid}.{ext}. Use POST /imagenes/perritos para obtener el uuid.
- Front construye la URL de imagen como `${VITE_API_BASE_URL}/imagenes/perritos/{id}`.
