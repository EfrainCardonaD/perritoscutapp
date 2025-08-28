# 🚀 API Perritos - Guía Completa de Endpoints JSON

## 📖 Estructura Final Consolidada

**¡TODO centralizado en RegistroController!** - Eliminados conflictos de rutas duplicadas

---

## 🏗️ **Estructura de Controladores**

### **RegistroController** (PRINCIPAL - TODO EN UNO)
```
📝 Formularios Web:
  GET  /registro                    → Mostrar formulario HTML
  POST /registro                    → Procesar formulario HTML

🔧 APIs REST - Registro:
  POST /api/registro                → Registro completo con perfil ⭐ PRINCIPAL
  POST /api/registro/basico         → Registro básico JSON

👤 APIs REST - Perfil:
  GET  /api/perfil/completo         → Obtener perfil del usuario
  POST /api/perfil/imagen           → Actualizar imagen de perfil
  GET  /api/perfil/usuario/{id}     → Obtener perfil por ID (admin)
```

### **AuthenticationController**
```
🔐 Autenticación:
  POST /api/login                   → Login con credenciales
  POST /api/refresh                 → Renovar token
  POST /api/logout                  → Cerrar sesión
```

### **ImagenController**
```
📸 Servir Archivos:
  GET  /api/imagenes/perfil/{archivo} → Servir imagen estática
```

---

## 🎯 **1. REGISTRO COMPLETO (Endpoint Principal)**

### **POST `/api/registro`** ⭐
**Content-Type**: `multipart/form-data`

**Campos Obligatorios:**
```
userName: testuser123
email: test@example.com
confirmEmail: test@example.com
password: Password123!
confirmPassword: Password123!
terms: true
```

**Campos Opcionales del Perfil:**
```
nombreReal: Juan Pérez
telefono: +5212345678901
idioma: es
zonaHoraria: America/Mexico_City
fechaNacimiento: 1990-05-15
fotoPerfil: [archivo imagen]
```

**Ejemplo usando curl:**
```bash
curl -X POST "http://localhost:8080/api/registro" \
  -F "userName=testuser123" \
  -F "email=test@example.com" \
  -F "confirmEmail=test@example.com" \
  -F "password=Password123!" \
  -F "confirmPassword=Password123!" \
  -F "terms=true" \
  -F "nombreReal=Juan Pérez" \
  -F "telefono=+5212345678901" \
  -F "idioma=es" \
  -F "zonaHoraria=America/Mexico_City" \
  -F "fechaNacimiento=1990-05-15" \
  -F "fotoPerfil=@/path/to/image.jpg"
```

**Respuesta exitosa (201):**
```json
{
  "mensaje": "Usuario registrado exitosamente",
  "usuario": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "userName": "testuser123",
    "email": "test@example.com",
    "nombreReal": "Juan Pérez",
    "telefono": "+5212345678901",
    "tieneImagenPerfil": true
  }
}
```

---

## 🔧 **2. REGISTRO BÁSICO JSON**

### **POST `/api/registro/basico`**
**Content-Type**: `application/json`

**Request Body:**
```json
{
  "userName": "testuser456",
  "email": "test2@example.com",
  "confirmEmail": "test2@example.com",
  "password": "Password123!",
  "confirmPassword": "Password123!",
  "terms": true
}
```

**Respuesta exitosa (201):**
```json
{
  "mensaje": "Usuario registrado exitosamente",
  "usuario": {
    "id": "456e7890-e89b-12d3-a456-426614174001",
    "userName": "testuser456",
    "email": "test2@example.com"
  }
}
```

---

## 👤 **3. GESTIÓN DE PERFIL**

### **GET `/api/perfil/completo`**
**Requiere**: Token de autenticación
**Headers**: `Authorization: Bearer <token>`

**Respuesta exitosa (200):**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "userName": "testuser123",
  "email": "test@example.com",
  "nombreReal": "Juan Pérez",
  "telefono": "+5212345678901",
  "idioma": "es",
  "zonaHoraria": "America/Mexico_City",
  "fechaNacimiento": "1990-05-15",
  "fotoPerfilUrl": "http://localhost:8080/api/imagenes/perfil/123e4567_profile.jpg"
}
```

### **POST `/api/perfil/imagen`**
**Content-Type**: `multipart/form-data`
**Requiere**: Token de autenticación
**Headers**: `Authorization: Bearer <token>`

**Request:**
```
imagen: [archivo de imagen]
```

**Respuesta exitosa (200):**
```json
{
  "mensaje": "Imagen de perfil actualizada exitosamente",
  "urlImagen": "http://localhost:8080/api/imagenes/perfil/123e4567_profile.jpg"
}
```

---

## 🔐 **4. AUTENTICACIÓN**

### **POST `/api/login`**
**Content-Type**: `application/json`

**Request Body:**
```json
{
  "userName": "testuser123",
  "password": "Password123!"
}
```

**Respuesta exitosa (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IlJFRlJFU0gi...",
  "expiresIn": 3600,
  "usuario": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "userName": "testuser123",
    "email": "test@example.com",
    "rol": "ROLE_USER"
  }
}
```

### **POST `/api/refresh`**
**Content-Type**: `application/json`

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IlJFRlJFU0gi..."
}
```

**Respuesta exitosa (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 3600
}
```

---

## ❌ **5. RESPUESTAS DE ERROR**

### **Errores de Validación (400)**
```json
{
  "error": "Los correos electrónicos no coinciden"
}
```

```json
{
  "error": "Las contraseñas no coinciden"
}
```

```json
{
  "error": "Debe aceptar los términos y condiciones"
}
```

```json
{
  "error": "El nombre de usuario solo puede contener letras y números"
}
```

### **Usuario Duplicado (400)**
```json
{
  "error": "El usuario o email ya existe"
}
```

### **No Autorizado (401)**
```json
{
  "error": "Token inválido o expirado"
}
```

### **No Encontrado (404)**
```json
{
  "error": "Perfil no encontrado"
}
```

### **Error del Servidor (500)**
```json
{
  "error": "Error interno del servidor"
}
```

---

## 🧪 **6. TESTING CON INSOMNIA/POSTMAN**

### **Flujo Completo de Prueba:**

1. **Registro Completo:**
   ```
   POST /api/registro
   → Subir imagen y datos completos
   → Verificar respuesta 201
   ```

2. **Login:**
   ```
   POST /api/login
   → Usar credenciales del registro
   → Guardar token de respuesta
   ```

3. **Obtener Perfil:**
   ```
   GET /api/perfil/completo
   → Usar token en Authorization header
   → Verificar datos completos
   ```

4. **Actualizar Imagen:**
   ```
   POST /api/perfil/imagen
   → Subir nueva imagen
   → Verificar nueva URL
   ```

### **Headers Necesarios:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json (para JSON)
Content-Type: multipart/form-data (para archivos)
```

---

## 🔧 **7. CONFIGURACIÓN PARA DESARROLLO**

### **Base URL:**
```
http://localhost:8080
```

### **Documentación Swagger:**
```
http://localhost:8080/swagger-ui.html
```

### **Variables de Entorno:**
```
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=graficos
MYSQL_USERNAME=root
MYSQL_PASSWORD=root
JWT_SECRET=your-secret-key
```

---

# Ejemplos JSON — Perritos Cut

Base URL: http://localhost:8080/api

1) Autenticación
- POST /login
Request:
{
  "userName": "testuser",
  "password": "Password123!"
}
Response 200:
{
  "token":"...",
  "refreshToken":"...",
  "expiresIn":3600,
  "usuario": {"id":"...","userName":"testuser","email":"u@e.com","rol":"ROLE_USER"}
}

- POST /refresh (enviar refreshToken por header)
Headers: Authorization: Bearer <refresh_token>
Response 200:
{ "token":"...", "expiresIn":3600 }

2) Perros
- GET /perros/catalogo?sexo=Macho&tamano=Mediano&ubicacion=CDMX&page=0&size=20
Response 200:
[
  {
    "id":"...",
    "nombre":"Firulais",
    "edad":3,
    "sexo":"Macho",
    "tamano":"Mediano",
    "raza":"Mestizo",
    "descripcion":"Juguetón",
    "ubicacion":"CDMX",
    "estadoAdopcion":"Disponible",
    "estadoRevision":"Aprobado",
    "usuarioId":"...",
    "imagenPrincipalId":"a3e2...-uuid",
    "imagenIds":["a3e2...-uuid","b4f5...-uuid"]
  }
]

- POST /imagenes/perritos (subir imagen)
Headers: Authorization: Bearer <token>
Content-Type: multipart/form-data
Form-data: file=@/ruta/local/perro1.jpg
Response 200:
{
  "id": "a3e2...-uuid",
  "filename": "a3e2...-uuid.jpg",
  "url": "/api/imagenes/perritos/a3e2...-uuid",
  "contentType": "image/jpeg",
  "size": 123456
}

- POST /perros (ROLE_USER)
Headers: Authorization: Bearer <token>
Body:
{
  "nombre":"Luna",
  "edad":2,
  "sexo":"Hembra",
  "tamano":"Pequeño",
  "raza":"Mestizo",
  "descripcion":"Muy tranquila",
  "ubicacion":"GDL",
  "imagenes":[{"id":"a3e2...-uuid","descripcion":"principal","principal":true}]
}
Response 200: DtoPerro
Response 400: errores de validación
Response 422: {"error":"Debe incluir al menos una imagen"}

- POST /admin/perros/{id}/aprobar (ROLE_ADMIN o ROLE_REVIEWER)
Response 200: DtoPerro
Response 422: {"error":"No se puede aprobar un perro sin imagen principal"}

- PATCH /admin/perros/{id}/estado?estado=Disponible (ROLE_ADMIN o ROLE_REVIEWER)
Response 200: DtoPerro
Response 422: {"error":"Solo perros aprobados pueden estar disponibles"}

3) Solicitudes de adopción
- POST /solicitudes (ROLE_USER)
Headers: Authorization: Bearer <token>
Body:
{ "perroId":"e1a5...-uuid", "mensaje":"Me encantaría adoptarlo" }
Response 200: DtoSolicitud
Response 422: {"error":"Solo se puede solicitar adopción de perros aprobados y disponibles"}

- POST /solicitudes/{id}/documentos (ROLE_USER)
Body:
{
  "tipoDocumento":"Identificacion",
  "urlDocumento":"https://cdn/doc1.pdf",
  "nombreArchivo":"INE.pdf",
  "tipoMime":"application/pdf",
  "tamanoBytes": 123456
}
Response 200 vacío
Response 400: errores de validación

- GET /admin/solicitudes/pendientes (ROLE_ADMIN o ROLE_REVIEWER)
Response 200: [DtoSolicitud]

- PATCH /admin/solicitudes/{id}/estado?estado=Aceptada (ROLE_ADMIN o ROLE_REVIEWER)
Respuestas:
- 200 OK
- 422 {"error":"Faltan documentos requeridos para continuar la revisión"}
- 422 {"error":"El perro no está disponible para ser adoptado"}
- 409 {"error":"Ya existe una solicitud aceptada para este perro"}

4) Errores estándar
- 400 Validación:
{
  "success": false,
  "message": "El nombre es obligatorio",
  "data": {"nombre":"El nombre es obligatorio"}
}

- 401 No autorizado:
{
  "success": false,
  "message": "Credenciales inválidas"
}

- 404 No encontrado:
{
  "success": false,
  "message": "Recurso no encontrado"
}

- 409 Conflicto:
{
  "success": false,
  "message": "Ya existe una solicitud aceptada para este perro"
}

- 422 Regla de negocio:
{
  "success": false,
  "message": "Solo perros aprobados pueden estar disponibles"
}
