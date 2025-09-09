# FASE 5 – Actualización en tiempo real del Catálogo de Perros (WebSockets STOMP)

## 1. Objetivo
Implementar actualización en tiempo real del catálogo público de perros cuando:
- Un perro es aprobado y pasa automáticamente a estado "Disponible".
- Un perro cambia su estado de adopción provocando su entrada o salida del catálogo.

## 2. Alcance
Incluye backend (Spring WebSocket STOMP), eventos de dominio, listener, publicación automática, y cliente frontend (Vue 3) con SockJS + STOMP para reflejar los cambios en la primera página del catálogo sin recarga manual.
Quedan fuera: notificaciones por usuario, reconexión configurable avanzada, métricas específicas del broker y paginación dinámica incremental en tiempo real.

## 3. Diseño Arquitectónico
Flujo básico:
1. Moderador APRUEBA perro -> `PerroService.aprobarPerro()` fuerza `Disponible` si estaba `Pendiente` y publica `PerroCatalogoNuevoEvent`.
2. Cambio de estado adopción -> `PerroService.cambiarEstadoAdopcion()` evalúa si entra o sale del catálogo y publica `PerroCatalogoNuevoEvent` o `PerroCatalogoRemoveEvent`.
3. `PerroCatalogoEventListener` escucha eventos, carga entidad y envía por STOMP:
   - `/topic/catalogo/nuevo` (payload: `DtoPerro`)
   - `/topic/catalogo/remove` (payload: `String` id)
4. Frontend abre conexión a `/ws` y suscribe ambos topics. Si la página actual es 0 y pasa filtros, inyecta o quita registros.

## 4. Componentes Clave
| Componente | Responsabilidad |
|------------|------------------|
| `WebSocketConfig` | Configura endpoint `/ws`, broker simple y prefijo `/topic`. |
| Eventos (`PerroCatalogoNuevoEvent`, `PerroCatalogoRemoveEvent`) | Señalizan cambios de visibilidad. |
| `PerroService` | Lógica de transición + publicación de eventos. |
| `PerroCatalogoEventListener` | Traduce eventos a mensajes STOMP. |
| Front `realtime.js` | Conexión única STOMP + manejadores de eventos. |
| `Catalogo.vue` | Suscripción y mutación reactiva de la lista. |
| `DogReadModal.vue` | Ajuste botón Adoptar (ownership). |

## 5. Cambios Realizados
Backend:
- `pom.xml`: agregado `spring-boot-starter-websocket`.
- `SecurityConfiguration`: permitir `/ws/**`.
- `WebSocketConfig`: configuración STOMP + SockJS.
- Nuevos eventos y listener en paquete `event`.
- `PerroService`: publicación de eventos y auto-disponibilización al aprobar.
- `DtoUsuario`: ahora incluye `id`.

Frontend:
- `package.json`: dependencias `sockjs-client`, `stompjs`.
- `utils/realtime.js`: helper conexión reutilizable.
- `Catalogo.vue`: integración tiempo real (página 0, filtros, deduplicación, recorte por tamaño de página).
- `DogReadModal.vue`: lógica de ocultar botón Adoptar para dueño (sin parpadeo, espera ambos IDs).

## 6. Estrategia de Seguridad
- Handshake abierto (`/ws/**`) pero solo datos no sensibles: catálogo es ya restringido a autenticados vía REST. *Riesgo*: clientes no autenticados pueden abrir socket pero no tienen token asociado en este flujo (no se inyecta validación principal). Posible mejora futura: canal seguro con autorización basada en token JWT (canal STOMP con interceptor). Clasificado como Mejora Futura (ver sección 11).

## 7. Estrategia de Reconexión
Implementada reconexión simple exponencial suave (retry cada 4s) en caso de cierre. A mejorar con backoff incremental.

## 8. Validación / QA
Checklist manual:
1. Crear perro (usuario normal) → Estado inicial fuera del catálogo.
2. Aprobar perro (moderador) → Aparece en Catálogo (sin refrescar) en cliente abierto en página 1.
3. Cambiar estado a "Adoptado" → Se remueve en tiempo real.
4. Volver a "Disponible" → Reaparece.
5. Aplicar filtro que excluya al perro → Evento llega pero no se inserta.
6. Estar en página > 0 → No se inyecta (solo se actualiza página 0 por diseño).

## 9. Plan de Rollback
- Revertir commits que introducen WebSocket.
- Eliminar dependencias `spring-boot-starter-websocket`, `sockjs-client`, `stompjs`.
- Remover código de eventos y listener.
- Remover helper `realtime.js` y lógica asociada en `Catalogo.vue`.

## 10. Riesgos y Mitigaciones
| Riesgo | Mitigación |
|--------|-----------|
| Sesión sin autenticación en WS | Interceptor STOMP futuro validará token. |
| Crecimiento de conexiones | Cambiar a broker externo (RabbitMQ / ActiveMQ) si escala. |
| Desfase paginación (nuevos en página 0 desplazan resultados) | Aceptado: comportamiento esperado; mensaje informativo futuro. |
| Flood de eventos | Debounce/agrupación si volumen supera X eventos/minuto. |

## 11. Mejoras Futuras (Backlog)
- Interceptor STOMP validando JWT para no permitir sockets anónimos.
- Notificaciones por usuario (suscripción `/user/queue/...`).
- Métricas en Actuator para conexiones activas.
- Paginación incremental en tiempo real (inserción en otras páginas).
- Buffer de eventos perdidos tras reconexión (snapshot delta).

## 12. Estado Final
Fase completada y desplegable. No se detectaron errores de compilación en backend tras integración inicial.

---
**Fin Fase 5**

