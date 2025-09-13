# API Perfil de Usuario

Objetivo: Permitir a un usuario autenticado (roles: USER, REVIEWER, ADMIN) gestionar su panel de perfil: ver resumen, ver perfil completo, actualizar datos básicos, actualizar foto de perfil. ADMIN además puede consultar y modificar/eliminar (desactivar) perfiles de otros usuarios.

Base URL (ejemplo): `https://tu-dominio/api/usuarios`
Autenticación: JWT en header
```
Authorization: Bearer <token>
```
Todas las respuestas usan el envoltorio estándar `RestResponse`:
```
{
  "success": true|false,
  "mensaje": "...",
  "data": { ... },
  "timestamp": "2025-01-01T12:00:00"
}
```

---
## 1. Obtener resumen propio
GET `/me/resumen`
Roles: USER, REVIEWER, ADMIN
Descripción: Devuelve datos ligeros para cabecera o menú.
Respuesta (200):
```
{
  "success": true,
  "mensaje": "Resumen de usuario",
  "data": {
    "id": "uuid",
    "userName": "juan",
    "email": "juan@example.com",
    "fotoPerfilUrl": "https://.../imagen.jpg" | null
  }
}
```
Errores:
- 401 no autenticado

---
## 2. Obtener perfil completo propio
GET `/me`
Roles: USER, REVIEWER, ADMIN
Descripción: Datos extendidos + metadatos + imagen activa.
Respuesta (200):
```
{
  "success": true,
  "mensaje": "Perfil completo",
  "data": {
    "id": "uuid-usuario",
    "userName": "juan",
    "email": "juan@example.com",
    "rol": "ROLE_USER",
    "activo": true,
    "fechaCreacion": "2025-01-01T11:22:33.000+00:00",
    "perfilId": "uuid-perfil",
    "nombreReal": "Juan Pérez",
    "telefono": "+521234567890",
    "idioma": "es",
    "zonaHoraria": "America/Mexico_City",
    "fechaNacimiento": "2000-05-10",
    "esMayorDeEdad": true,
    "fotoPerfilId": "uuid-foto" | null,
    "fotoPerfilUrl": "https://.../foto.jpg" | null,
    "nombreArchivoFoto": "abc123.jpg" | null,
    "tipoMimeFoto": "image/jpeg" | null,
    "tamañoBytesFoto": 123456 | null,
    "fechaCreacionPerfil": "2025-01-01T11:22:40.000+00:00",
    "fechaActualizacionPerfil": "2025-01-02T09:10:00.000+00:00"
  }
}
```
Errores: 401, 404 (si falta el perfil extendido)

---
## 3. Actualizar campos del perfil propio
PATCH `/me`
Content-Type: `application/json`
Roles: USER, REVIEWER, ADMIN
Body (todos opcionales; sólo se actualizan los presentes):
```
{
  "nombreReal": "Juan Actualizado",
  "telefono": "+52 123 456 7890",
  "idioma": "es",
  "zonaHoraria": "America/Mexico_City",
  "fechaNacimiento": "2000-05-10"
}
```
Validaciones:
- telefono: regex `^[+0-9()\s-]{5,20}$`
- idioma: máx 5 chars
- zonaHoraria: máx 50 chars
- fechaNacimiento: >= 15 años
Respuesta (200): mismo esquema de perfil completo actualizado.
Errores:
- 400 validación
- 401 no autenticado
- 404 perfil no encontrado

---
## 4. Actualizar foto de perfil propia
POST `/me/foto`
Roles: USER, REVIEWER, ADMIN
Content-Type: `multipart/form-data`
Campo obligatorio:
- `archivo`: imagen (jpeg, png, webp, gif) tamaño <= 15MB
Respuesta (200):
```
{
  "success": true,
  "mensaje": "Imagen actualizada" | "Foto de perfil actualizada",
  "data": { "fotoPerfilUrl": "https://.../nueva.jpg" }
}
```
Errores:
- 400 archivo inválido
- 401 no autenticado
- 500 error de subida

---
## 5. Obtener perfil por ID (otro usuario)
GET `/{id}`
Roles:
- ADMIN: cualquiera
- USER / REVIEWER: sólo su propio id
Seguridad: `@PreAuthorize("hasRole('ADMIN') or #id == principal.usuario.id")`
Respuesta: igual a "perfil completo".
Errores: 401, 403, 404

---
## 6. Actualizar perfil de otro usuario (ADMIN)
PATCH `/{id}`
Roles: ADMIN
Body: mismo que PATCH /me.
Respuesta: perfil completo actualizado.
Errores: 400, 401, 403 (si no admin), 404

---
## 7. Desactivar usuario (ADMIN)
DELETE `/admin/{id}`
Roles: ADMIN
Acción: marca activo = false y limpia token.
Respuesta (200):
```
{
  "success": true,
  "mensaje": "Usuario desactivado" | "Usuario ya estaba inactivo"
}
```
Errores: 401, 403, 404

---
## Ejemplos Curl
Resumen propio:
```
curl -H "Authorization: Bearer <TOKEN>" https://api.tuapp.com/api/usuarios/me/resumen
```
Actualizar perfil propio:
```
curl -X PATCH https://api.tuapp.com/api/usuarios/me \
 -H "Authorization: Bearer <TOKEN>" \
 -H "Content-Type: application/json" \
 -d '{"nombreReal":"Nuevo Nombre","telefono":"+521234567890"}'
```
Subir foto:
```
curl -X POST https://api.tuapp.com/api/usuarios/me/foto \
 -H "Authorization: Bearer <TOKEN>" \
 -F "archivo=@/ruta/local/foto.jpg"
```
Admin: obtener otro perfil:
```
curl -H "Authorization: Bearer <TOKEN_ADMIN>" https://api.tuapp.com/api/usuarios/UUID-USUARIO
```
Desactivar usuario:
```
curl -X DELETE -H "Authorization: Bearer <TOKEN_ADMIN>" https://api.tuapp.com/api/usuarios/admin/UUID-USUARIO
```

---
## Resumen de Reglas de Acceso
| Endpoint | USER/REVIEWER | ADMIN |
|----------|---------------|-------|
| GET /me/resumen | Sí | Sí |
| GET /me | Sí | Sí |
| PATCH /me | Sí | Sí |
| POST /me/foto | Sí | Sí |
| GET /{id} | Sólo propio | Cualquiera |
| PATCH /{id} | No | Sí |
| DELETE /admin/{id} | No | Sí |

---
## Notas de Integración Front
1. Guardar en el estado global el resultado de `/me/resumen` para header.
2. Al abrir página de perfil cargar `/me` (mostrar loader) y rellenar formulario.
3. Enviar sólo campos modificados en PATCH (evitar overwrites vacíos).
4. Tras subir imagen, refrescar `/me` o actualizar URL directamente.
5. Manejar 401 -> redirigir login; 403 -> mostrar mensaje de acceso denegado.
6. Si 404 en `/me`, forzar flujo de completado de perfil inicial (si aplica).

---
## Errores Comunes y Manejo
| Código | Caso | Acción Front |
|-------|------|--------------|
| 400 | Validación (teléfono, edad, formato imagen) | Mostrar mensaje `mensaje` |
| 401 | Token ausente/expirado | Redirigir login |
| 403 | Intento acceder a perfil ajeno | Mostrar pantalla acceso denegado |
| 404 | Perfil no encontrado | Mostrar invitación a completar perfil |
| 500 | Subida imagen falló | Reintentar / notificar |

---
## Checklist Front sugerido
- [ ] Hook para obtener/actualizar perfil
- [ ] Vista edición perfil (form + preview imagen)
- [ ] Componente subida imagen con validaciones locales (tipo, tamaño)
- [ ] Manejo optimista de actualización de campos
- [ ] Cachear resumen en store (ej. pinia/vuex)

---
## Futuras Extensiones (Opcional)
- Cambiar email con flujo de verificación
- Cambiar password seguro (endpoint separado)
- Historial de imágenes de perfil

Fin.

