# Seguridad y JWT — Perritos Cut

Este documento describe la seguridad de la API: autenticación con JWT, CORS, rutas públicas/protegidas y roles.

## Stack
- Spring Security 6
- Filtro JWT propio (SecurityFilter) validando tokens en cada petición
- Librería de tokens: Auth0 java-jwt
- Hash de contraseñas: BCrypt

## Propiedades y variables de entorno
- app.security.jwt.secret: secreto para firmar/validar JWT (env: JWT_SECRET)
- app.security.jwt.issuer: emisor del token; por defecto "perritoscutapp"
- app.cors.allowed-origins: orígenes permitidos (env: APP_CORS_ALLOWED_ORIGINS)

Datasource:
- SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD

Ejemplo en application.properties:
```
app.security.jwt.secret=${JWT_SECRET:changeit}
app.security.jwt.issuer=perritoscutapp
app.cors.allowed-origins=http://localhost:3000
```

## Flujo de autenticación
1) POST /api/login con credenciales válidas → se emite access token (y refresh si aplica).
2) Para renovar, POST /api/refresh enviando el refresh token en el header Authorization:
```
Authorization: Bearer <refresh_token>
```
3) Para cerrar sesión: POST /api/logout con el token en Authorization.

Claims del token (access): sub (username), roles, id, iat, exp.

## Rutas públicas vs protegidas
Públicas (no requieren JWT):
- POST /api/login, POST /api/refresh
- POST /api/registro, /api/registro-completo, /api/validar-paso1, /api/validar-paso2
- GET/HEAD /api/imagenes/perfil/**
- GET /api/perros/catalogo
- Swagger/OpenAPI: /swagger-ui.html, /v3/api-docs/**, /swagger-ui/**
- Actuator: /actuator/health/**, /actuator/info

Protegidas: resto de rutas.

## Roles y autorización
Roles efectivos:
- ROLE_USER
- ROLE_ADMIN
- ROLE_REVIEWER

Reglas destacadas (@PreAuthorize):
- Moderación de perros (aprobar/rechazar/cambiar estado): ADMIN o REVIEWER.
- Creación de perros y solicitudes: USER.

## CORS
Configurable por propiedad `app.cors.allowed-origins` (lista separada por comas). En producción, restringir solo a los orígenes necesarios.

## Contraseñas
Almacenadas con BCryptPasswordEncoder.

## Rate limiting
Existe un RateLimitFilter registrado vía FilterRegistrationBean.
- Estado actual: límite muy alto (MAX_REQUESTS=50,000,000 en 1 minuto), sin efecto práctico.
- Pendiente: parametrizar por propiedades y fijar límites reales por IP/endpoint.

## Errores comunes
- 401 Unauthorized: token ausente, inválido o expirado.
- 403 Forbidden: token válido sin permisos suficientes (p.ej., sin rol ADMIN/REVIEWER en endpoints de moderación).

## Revocación/Logout
No hay blacklist de tokens implementada. Recomendación: expiración corta para access token, refresh token rotado y almacenamiento/invalidación por servidor (pendiente de roadmap).
