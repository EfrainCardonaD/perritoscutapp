# Observabilidad con Spring Boot Actuator

Este documento explica cómo se habilita y usa Actuator en la aplicación para health checks, métricas básicas y readiness/liveness probes.

Se añadió la dependencia:
- org.springframework.boot:spring-boot-starter-actuator

## Endpoints expuestos
- /actuator/health: estado general (abierto sin autenticación)
- /actuator/health/liveness: liveness probe
- /actuator/health/readiness: readiness probe
- /actuator/info: información básica de la app (abierto sin autenticación)

Otros endpoints de Actuator permanecen cerrados por defecto.

## Configuración
application.properties incluye:
- management.endpoints.web.exposure.include=health,info
- management.endpoint.health.probes.enabled=true

Seguridad (SecurityConfiguration) permite acceso público a:
- /actuator/health/**
- /actuator/info

## Uso típico
Ver salud general:
- curl http://localhost:8080/actuator/health

Liveness y Readiness (útil en contenedores y orquestadores):
- curl http://localhost:8080/actuator/health/liveness
- curl http://localhost:8080/actuator/health/readiness

## Docker/Compose
El docker-compose levanta MySQL con healthcheck y la API depende de que la DB esté healthy. Actuator ofrece endpoints para probes de Kubernetes u orquestadores similares.

Recomendaciones en producción:
- Mantener expuestos solo health e info.
- Si /actuator/info muestra datos sensibles, configúralo con contenido mínimo o protégelo.
- Añadir métricas y trazas solo si se integrará con un stack de observabilidad (Prometheus, etc.).

