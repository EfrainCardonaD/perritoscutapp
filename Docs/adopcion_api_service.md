# Servicio de Adopciones - Documentación API

Esta documentación describe el flujo funcional completo y los contratos de la API de adopciones después del refactor a `DtoSolicitudAdopcion` (DTO unificado). Está pensada para que el frontend pueda construir factories / stores con confianza.

---
## 1. Conceptos y Modelo

### 1.1. Solicitud de Adopción (DtoSolicitudAdopcion)
Representa una solicitud creada por un usuario para adoptar un perro.

Campos (JSON):
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | string (UUID) | Identificador de la solicitud |
| perroId | string | ID del perro solicitado |
| solicitanteId | string | ID del usuario que creó la solicitud |
| estado | string | Etiqueta legible del estado ("Pendiente", "En revisión", etc.) |
| estadoCodigo | string | Nombre enum: `PENDIENTE`, `EN_REVISION`, `ACEPTADA`, `RECHAZADA`, `CANCELADA` |
| mensaje | string/null | Mensaje opcional del solicitante |
| fechaSolicitud | string (ISO-8601) | Timestamp creación |
| fechaRespuesta | string/null (ISO-8601) | Timestamp última resolución (estado final o revisión) |
| revisadoPorId | string/null | ID del revisor (admin/reviewer) que cambió estado (cuando aplica) |
| documentos | array<Documento>/null | Lista de documentos asociados (incluida sólo en modo detalle o endpoints específicos) |

### 1.2. Documento (DtoSolicitudAdopcion.Documento)
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | string | ID (UUID o ID almacenamiento) del documento |
| tipoDocumento | string | Tipo lógico (ej: "Identificacion", "Comprobante domicilio") |
| urlDocumento | string | URL accesible del archivo |
| nombreArchivo | string/null | Nombre original del archivo |
| tipoMime | string/null | MIME type detectado |
| tamanoBytes | number/null | Tamaño en bytes |
| fechaSubida | string (ISO-8601) | Fecha de subida |

### 1.3. Estados y Reglas
Estados (enum => etiqueta):
- PENDIENTE => "Pendiente"
- EN_REVISION => "En revisión"
- ACEPTADA => "Aceptada"
- RECHAZADA => "Rechazada"
- CANCELADA => "Cancelada"

Reglas clave:
1. Crear solicitud: perro debe estar `APROBADO` y `DISPONIBLE`.
2. No se puede solicitar adopción del propio perro.
3. Actualizar mensaje: sólo si estado = `PENDIENTE`.
4. Cancelar: prohibido si estado es `ACEPTADA` o `RECHAZADA`.
5. Eliminar: usuario sólo si `PENDIENTE`; admin puede eliminar en cualquier estado (excepto restricciones lógicas internas).
6. Pasar a `EN_REVISION` o `ACEPTADA`: deben existir al menos 2 tipos de documentos distintos (regla de negocio `contarTiposPorSolicitud >= 2`).
7. Al aceptar (`ACEPTADA`): el perro pasa a `ADOPTADO` y las demás solicitudes no resueltas para el mismo perro se marcan `RECHAZADA`.
8. Revertir adopción: sólo si la solicitud estaba `ACEPTADA`; se cambia a `RECHAZADA` y el perro vuelve a `DISPONIBLE` (si sigue aprobado).

---
## 2. Endpoints Usuario (Autenticado ROLE_USER)
Base: `/api/usuario/adopciones/solicitudes`

### 2.1. Crear Solicitud
`POST /api/usuario/adopciones/solicitudes`
- Content-Type: `multipart/form-data`
- Parámetros form-data:
  - perroId (string) [requerido]
  - mensaje (string) [opcional]
  - tipoDocumento (string) [opcional, default: "Documento"]
  - file (archivo) [requerido]

Respuesta 200 (JSON detalle con 1 documento):
```json
{
  "id": "d9d7d5d2-2f0b-4f37-9d3a-a1b29ce54123",
  "perroId": "PERRO-123",
  "solicitanteId": "USR-456",
  "estado": "Pendiente",
  "estadoCodigo": "PENDIENTE",
  "mensaje": "Quiero darle un hogar responsable",
  "fechaSolicitud": "2025-09-13T12:45:31.123Z",
  "fechaRespuesta": null,
  "revisadoPorId": null,
  "documentos": [
    {
      "id": "3f2f8a63-92bd-4e99-a2ab-5b0f6d2e91aa",
      "tipoDocumento": "Identificacion",
      "urlDocumento": "https://cdn.example.com/docs/3f2f8a63-92bd.png",
      "nombreArchivo": "ine_frente.png",
      "tipoMime": "image/png",
      "tamanoBytes": 348723,
      "fechaSubida": "2025-09-13T12:45:31.200Z"
    }
  ]
}
```

Errores posibles:
- 422: perro no disponible, mismo dueño, o reglas incumplidas.
- 401/403: no autenticado / rol inválido.

### 2.2. Listar Mis Solicitudes (resumen)
`GET /api/usuario/adopciones/solicitudes/mis`

Respuesta 200 (array, sin `documentos` o con `documentos: null` según implementación):
```json
[
  {
    "id": "...",
    "perroId": "PERRO-123",
    "solicitanteId": "USR-456",
    "estado": "Pendiente",
    "estadoCodigo": "PENDIENTE",
    "mensaje": "Texto",
    "fechaSolicitud": "2025-09-13T12:45:31.123Z",
    "fechaRespuesta": null,
    "revisadoPorId": null,
    "documentos": null
  }
]
```

### 2.3. Detalle de Solicitud
`GET /api/usuario/adopciones/solicitudes/{id}`

Respuesta 200 (incluye `documentos`): igual formato que creación.

Errores: 403 si no es dueño ni tiene privilegios.

### 2.4. Actualizar Mensaje
`PATCH /api/usuario/adopciones/solicitudes/{id}/mensaje`
- Body JSON:
```json
{ "mensaje": "Nuevo mensaje actualizado" }
```

Respuesta 200 (resumen o detalle sin forzar documentos):
```json
{
  "id": "...",
  "perroId": "PERRO-123",
  "solicitanteId": "USR-456",
  "estado": "Pendiente",
  "estadoCodigo": "PENDIENTE",
  "mensaje": "Nuevo mensaje actualizado",
  "fechaSolicitud": "2025-09-13T12:45:31.123Z",
  "fechaRespuesta": null,
  "revisadoPorId": null,
  "documentos": null
}
```

Errores: 422 si estado != PENDIENTE; 403 si no es dueño.

### 2.5. Cancelar Solicitud
`POST /api/usuario/adopciones/solicitudes/{id}/cancelar`

Respuesta 200:
```json
{
  "id": "...",
  "estado": "Cancelada",
  "estadoCodigo": "CANCELADA",
  "fechaRespuesta": "2025-09-13T13:10:02.500Z",
  "perroId": "PERRO-123",
  "solicitanteId": "USR-456",
  "mensaje": "...",
  "revisadoPorId": null,
  "documentos": null,
  "fechaSolicitud": "2025-09-13T12:45:31.123Z"
}
```
Errores: 422 si ya estaba aceptada o rechazada.

### 2.6. Eliminar Solicitud
`DELETE /api/usuario/adopciones/solicitudes/{id}`

Respuesta 200: cuerpo vacío.

Errores: 422 si no está PENDIENTE (para usuarios), 403 si no autorizado.

### 2.7. Listar Documentos de una Solicitud
`GET /api/usuario/adopciones/solicitudes/{id}/documentos`

Respuesta 200:
```json
[
  {
    "id": "3f2f8a63-92bd-4e99-a2ab-5b0f6d2e91aa",
    "tipoDocumento": "Identificacion",
    "urlDocumento": "https://cdn.example.com/docs/3f2f8a63-92bd.png",
    "nombreArchivo": "ine_frente.png",
    "tipoMime": "image/png",
    "tamanoBytes": 348723,
    "fechaSubida": "2025-09-13T12:45:31.200Z"
  }
]
```

### 2.8. Obtener Documento Específico
`GET /api/usuario/adopciones/solicitudes/{id}/documentos/{docId}`

Respuesta 200 (objeto Documento) igual a elemento de lista.

---
## 3. Endpoints Admin / Reviewer (ROLE_ADMIN o ROLE_REVIEWER)
Base: `/api/admin/adopciones/solicitudes`

### 3.1. Pendientes de Revisión
`GET /api/admin/adopciones/solicitudes/pendientes`

Respuesta 200 (array resumen):
```json
[
  {
    "id": "...",
    "perroId": "PERRO-123",
    "solicitanteId": "USR-456",
    "estado": "Pendiente",
    "estadoCodigo": "PENDIENTE",
    "mensaje": "...",
    "fechaSolicitud": "2025-09-13T12:45:31.123Z",
    "fechaRespuesta": null,
    "revisadoPorId": null,
    "documentos": null
  }
]
```

### 3.2. Búsqueda Filtrada
`GET /api/admin/adopciones/solicitudes?estado=Pendiente&perroId=PERRO-123&solicitanteId=USR-456`
- `estado` debe usar **etiqueta** (no el enum interno). Acepta vacío.

Respuesta: lista mismo formato que arriba.

### 3.3. Actualizar Estado
`PATCH /api/admin/adopciones/solicitudes/{id}/estado?estado=En%20revisión`
- El parámetro `estado` es la etiqueta destino (sensitivo a acentos y espacios exactos usados en labels).
- Validaciones: ver reglas de sección 1.3.

Respuesta 200 (resumen):
```json
{
  "id": "...",
  "estado": "En revisión",
  "estadoCodigo": "EN_REVISION",
  "fechaRespuesta": "2025-09-13T13:30:00.000Z",
  "perroId": "PERRO-123",
  "solicitanteId": "USR-456",
  "mensaje": "...",
  "revisadoPorId": "ADMIN-99",
  "documentos": null,
  "fechaSolicitud": "2025-09-13T12:45:31.123Z"
}
```
Errores: 422 transiciones inválidas / falta de documentos; 409 conflicto si ya hay aceptada; 403 si rol insuficiente.

### 3.4. Revertir Adopción
`POST /api/admin/adopciones/solicitudes/{id}/revertir`
- Sólo si la solicitud está `ACEPTADA`.

Respuesta 200 (resumen estado RECHAZADA): similar a actualizarEstado.

### 3.5. Listar Documentos
`GET /api/admin/adopciones/solicitudes/{id}/documentos`
Respuesta: array de Documento (idéntico al endpoint de usuario).

### 3.6. Obtener Documento
`GET /api/admin/adopciones/solicitudes/{id}/documentos/{docId}`
Respuesta: Documento.

---
## 4. Casos de Uso Front y Factories Sugeridas

### 4.1. Factory SolicitudResumen
```ts
interface SolicitudResumen {
  id: string;
  perroId: string;
  solicitanteId: string;
  estado: string;        // etiqueta
  estadoCodigo: string;  // enum interno
  mensaje: string | null;
  fechaSolicitud: string;
  fechaRespuesta: string | null;
  revisadoPorId: string | null;
}
```

### 4.2. Factory DocumentoSolicitud
```ts
interface DocumentoSolicitud {
  id: string;
  tipoDocumento: string;
  urlDocumento: string;
  nombreArchivo: string | null;
  tipoMime: string | null;
  tamanoBytes: number | null;
  fechaSubida: string;
}
```

### 4.3. Factory SolicitudDetalle
```ts
interface SolicitudDetalle extends SolicitudResumen {
  documentos: DocumentoSolicitud[]; // puede venir [] o null (normalizar a [])
}
```

### 4.4. Normalización Recomendada
- Si `documentos` es `null` => establecer `[]` en el store para evitar checks.
- Usar `estadoCodigo` para lógica de flujos (deshabilitar botones, etc.).
- Presentar `estado` directamente para etiquetas UI.

### 4.5. Transiciones UI
| Acción | Requiere Estado Actual | Estado Resultante |
|-------|------------------------|-------------------|
| Usuario edita mensaje | PENDIENTE | PENDIENTE |
| Usuario cancela | PENDIENTE / EN_REVISION | CANCELADA |
| Admin pasa a revisión | PENDIENTE | EN_REVISION |
| Admin acepta | EN_REVISION / PENDIENTE (con docs) | ACEPTADA |
| Admin rechaza | PENDIENTE / EN_REVISION | RECHAZADA |
| Admin revierte | ACEPTADA | RECHAZADA |

---
## 5. Errores y Manejo Front
Aunque el formato exacto del error dependerá del handler global, se recomienda capturar:

Ejemplo genérico (posible):
```json
{
  "timestamp": "2025-09-13T13:40:10.123Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Faltan documentos requeridos para continuar la revisión",
  "path": "/api/admin/adopciones/solicitudes/123/estado"
}
```

Sugerencias front:
- Mapear 422 a mensajes de validación de negocio.
- Mapear 403 a "No autorizado" y redireccionar o mostrar aviso.
- Mapear 409 a conflicto explícito (solicitud ya aceptada para el perro).

---
## 6. Estrategia de Caching / Refetch Front
| Evento | Acción Front Recomendada |
|--------|--------------------------|
| Crear solicitud | Refetch lista "mis" y detalle recién creado |
| Actualizar mensaje | Actualizar item en cache + opcional refetch detalle |
| Cancelar | Actualizar estado local + refetch detalle si pantalla abierta |
| Eliminar | Quitar de lista y navegar fuera del detalle |
| Admin cambia estado | Refetch búsqueda/pendientes + detalle afectado |
| Revertir adopción | Refetch detalle solicitud + listado perro (si existe) |

---
## 7. Ejemplos de Factories (TypeScript) Simplificadas
```ts
function buildSolicitudResumen(json: any): SolicitudResumen {
  return {
    id: json.id,
    perroId: json.perroId,
    solicitanteId: json.solicitanteId,
    estado: json.estado,
    estadoCodigo: json.estadoCodigo,
    mensaje: json.mensaje ?? null,
    fechaSolicitud: json.fechaSolicitud,
    fechaRespuesta: json.fechaRespuesta ?? null,
    revisadoPorId: json.revisadoPorId ?? null,
  };
}

function buildDocumento(json: any): DocumentoSolicitud {
  return {
    id: json.id,
    tipoDocumento: json.tipoDocumento,
    urlDocumento: json.urlDocumento,
    nombreArchivo: json.nombreArchivo ?? null,
    tipoMime: json.tipoMime ?? null,
    tamanoBytes: json.tamanoBytes ?? null,
    fechaSubida: json.fechaSubida,
  };
}

function buildSolicitudDetalle(json: any): SolicitudDetalle {
  return {
    ...buildSolicitudResumen(json),
    documentos: Array.isArray(json.documentos) ? json.documentos.map(buildDocumento) : [],
  };
}
```

---
## 8. Checklist Implementación Front
- [ ] Endpoint creación (multipart) + vista de confirmación.
- [ ] Listado "Mis Solicitudes" (resumen).
- [ ] Detalle con documentos y acciones (editar mensaje, cancelar, eliminar).
- [ ] Módulo Admin: pendientes + búsqueda con filtros.
- [ ] Cambio de estado con validaciones UI (deshabilitar acciones inválidas).
- [ ] Vista documentos admin (lista + visor).
- [ ] Manejo uniforme errores (422/403/409).

---
## 9. Notas Futuras / Extensiones Posibles
- Endpoint para añadir más documentos (no implementado actualmente en este flujo refactor). Podría exponerse posteriormente.
- Websocket / eventos para notificar cambios de estado al solicitante.
- Auditoría extendida (historial de transiciones) si se requiere trazabilidad.

---
## 10. Resumen Rápido de Endpoints
| Método | Ruta | Uso | Devuelve |
|--------|------|-----|----------|
| POST | /api/usuario/adopciones/solicitudes | Crear solicitud | Solicitud detalle |
| GET | /api/usuario/adopciones/solicitudes/mis | Listar propias | Array resumen |
| GET | /api/usuario/adopciones/solicitudes/{id} | Detalle | Solicitud detalle |
| PATCH | /api/usuario/adopciones/solicitudes/{id}/mensaje | Actualizar mensaje | Solicitud resumen |
| POST | /api/usuario/adopciones/solicitudes/{id}/cancelar | Cancelar | Solicitud resumen |
| DELETE | /api/usuario/adopciones/solicitudes/{id} | Eliminar | 200 vacío |
| GET | /api/usuario/adopciones/solicitudes/{id}/documentos | Listar docs | Array documento |
| GET | /api/usuario/adopciones/solicitudes/{id}/documentos/{docId} | Doc puntual | Documento |
| GET | /api/admin/adopciones/solicitudes/pendientes | Pendientes | Array resumen |
| GET | /api/admin/adopciones/solicitudes | Buscar filtrado | Array resumen |
| PATCH | /api/admin/adopciones/solicitudes/{id}/estado?estado= | Cambiar estado | Solicitud resumen |
| POST | /api/admin/adopciones/solicitudes/{id}/revertir | Revertir adopción | Solicitud resumen |
| GET | /api/admin/adopciones/solicitudes/{id}/documentos | Listar docs | Array documento |
| GET | /api/admin/adopciones/solicitudes/{id}/documentos/{docId} | Doc puntual | Documento |

---
**Fin del documento.**

