# CUT - Sistema de Autenticación y Gráficos

## Descripción del Proyecto

CUT es una aplicación web en Spring Boot que ofrece autenticación con JWT, gestión de usuarios y procesamiento de gráficos/histogramas. Incluye seguridad, migraciones con Flyway, observabilidad con Actuator y despliegue con Docker.

## Tecnologías Principales
- Java 17
- Spring Boot 3.4.x
- Spring Security 6
- Spring Data JPA (MySQL)
- JWT (Auth0 java-jwt)
- Thymeleaf
- Flyway
- Actuator
- Docker/Docker Compose

## Estructura (resumen)
```
src/main/java/com/cut/cardona/
├── controllers/            # Controladores REST y web
├── modelo/                 # Entidades y DTOs
├── security/               # Configuración de seguridad (JWT, filtros, CORS)
└── ...
```

## Instalación y Configuración

### Prerrequisitos
- Java 17
- Maven
- MySQL 8.x (o Docker)

### Variables de entorno
Recomendado usar las estándar de Spring. Las antiguas DB_* siguen funcionando como fallback.

Obligatorias:
- SPRING_DATASOURCE_URL=jdbc:mysql://HOST:3306/cutdb?useSSL=false&serverTimezone=UTC
- SPRING_DATASOURCE_USERNAME=usuario
- SPRING_DATASOURCE_PASSWORD=clave
- JWT_SECRET=secreto_fuerte

Opcionales:
- SPRING_PROFILES_ACTIVE=dev
- APP_CORS_ALLOWED_ORIGINS=http://localhost:3000

### Ejecutar local
```
./mvnw spring-boot:run
```

### Docker (recomendado)
```
docker compose up -d
```
- Servicio backend expuesto en http://localhost:8080
- MySQL expuesto en 3306 con base cutdb (ver docker-compose.yml)

## Endpoints principales

Autenticación:
- POST /api/login
- POST /api/registro

Imágenes/estáticos públicos:
- GET /api/imagenes/perfil/**

OpenAPI/Swagger:
- /swagger-ui.html
- /v3/api-docs

Actuator (observabilidad):
- GET /actuator/health
- GET /actuator/info

Más detalles en documentos dedicados:
- docs: Seguridad y JWT -> ver SECURITY.md
- docs: Observabilidad y Actuator -> ver ACTUATOR.md

## Seguridad (resumen)
- Autenticación por JWT con Auth0 java-jwt.
- Contraseñas con BCrypt.
- CORS configurable por app.cors.allowed-origins.
- Filtro personalizado para validar tokens en cada request.
- Rutas públicas: login/registro, imágenes, Swagger y health/info.

Consulta SECURITY.md para el detalle de configuración y uso.

## Observabilidad (resumen)
Spring Boot Actuator habilitado con health e info expuestos. Health incluye probes de readiness/liveness.

Consulta ACTUATOR.md para configuración y consumo.

## Despliegue

Producción (jar):
```
./mvnw clean package
java -jar target/*.jar
```

Dockerfile multi-stage incluido. Compose levanta API + MySQL con healthcheck y variables estándar.

## Contribución
- Issues y PRs bienvenidos.
