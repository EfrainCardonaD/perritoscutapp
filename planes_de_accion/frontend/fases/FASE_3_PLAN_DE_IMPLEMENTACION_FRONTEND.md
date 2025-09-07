# Fase 3 — Plan de Implementación Frontend (Mis Perros + Alta)

Objetivo
- Entregar “Mis Perros” y “Alta de Perro” consumiendo los endpoints existentes del backend.

Alcance y supuestos
- Endpoints disponibles: GET /api/perros/mis y POST /api/perros; catálogo ya operativo.
- Gestión de imágenes: subida de archivos al backend (endpoint local de imágenes) y asociación por id en el payload (MVP).
- Autenticación/verificación ya operativas (guards y axios central listos).

Entregables
- Rutas protegidas:
  - /perros/mios → MisPerros.vue (requiresAuth, rol USER).
  - /perros/nuevo → NuevoPerro.vue (requiresAuth, rol USER).
- Store perros (Pinia): front/src/stores/perros.js
  - acciones: fetchMy(), create(payload)
  - estado: list, loading, error
- Componentes:
  - DogForm.vue: formulario (nombre, edad, sexo, tamaño, raza, ubicación, descripción, imágenes con principal).
  - DogImagesEditor.vue: subida a /api/imagenes/perritos, listado, marcar principal único, eliminar.
- UX/Estado
  - Loaders y skeletons; mensajes de error/success; validación previa a envío (≥1 imagen y exactamente una principal).

Criterios de aceptación
- Crear perro válido redirige a “Mis Perros” y aparece en el listado sin recargar.
- Estados vacíos con CTA; errores visibles; loaders en llamadas.
- Validaciones mínimas OK (nombre, rangos, sexo/tamaño, imágenes/principal).

Checklist
- [ ] Rutas y guards agregados.
- [ ] Store perros creada e integrada con api.js.
- [ ] MisPerros.vue con listado, vacío, error y loaders.
- [ ] NuevoPerro.vue con DogForm.vue.
- [ ] DogImagesEditor.vue con principal único y previsualización.
- [ ] Smoke: login → crear → ver en “Mis Perros”.

Pruebas y smoke
- Manual: flujo feliz crear perro; edge: sin imagen principal y validación de formulario.
- Opcional: tests de componentes (validaciones y render básico).

Riesgos y mitigación
- Divergencia de contratos → encapsular payload en store y adaptar si cambia DTO.
- Estados no verificados/usuario inactivo → aviso en UI y guard ya existente.

Notas
- El catálogo puede hacerse público si se desea, ajustando meta.requiresAuth.

