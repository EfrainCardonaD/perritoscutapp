# Fase 1 — Catálogo Público (Frontend)

Objetivo
- Implementar el catálogo público de perros con filtros básicos y paginación simple, alineado con Backend F1 (GET /api/perros/catalogo).

Alcance
- Vista pública Catalogo.vue (ruta /perros) que consume /api/perros/catalogo.
- Componentes: DogCard.vue (tarjeta de perro) y CatalogFilters.vue (filtros: sexo, tamaño, ubicación).
- Paginación simple con page/size, orden implícito por fecha_publicacion DESC.
- Manejo de estados: cargando, vacío, error.

Supuestos
- Endpoint devuelve una lista [DtoPerro] sin total; la última página se infiere por resultados < size.
- Campos disponibles: { id, nombre, edad, sexo, tamano, raza, descripcion, ubicacion, estadoAdopcion, estadoRevision, usuarioId }.
- Imágenes no forman parte del DtoPerro en esta fase; se usa placeholder visual.

Entregables (esta PR)
- [x] planes_de_accion/frontend/fases/FASE_1_PLAN_DE_IMPLEMENTACION_FRONTEND.md
- [x] src/components/DogCard.vue (imagen placeholder + metadatos básicos)
- [x] src/components/CatalogFilters.vue (sexo, tamaño, ubicación + botón aplicar)
- [x] src/views/Catalogo.vue (lista + filtros + paginación + loaders/errores)
- [x] src/router/index.js → ruta pública /perros con meta title

Criterios de aceptación
- La ruta /perros lista perros aprobados/disponibles según backend, con filtros sexo/tamaño/ubicación aplicables.
- Paginación con botones Anterior/Siguiente; Siguiente deshabilita cuando resultados < size; Anterior deshabilita en primera página.
- Estados visibles: cargando (skeleton/simple), vacío (mensaje), error (alerta).

Riesgos y mitigaciones
- Contrato sin imágenes: usar placeholder; cuando haya URL principal, DogCard soportará prop imageUrl opcional.
- Sin total de resultados: inferir última página por longitud < size; mejorar cuando backend exponga page/total.
- Validación de filtros: valores se restringen a listas conocidas para sexo/tamaño; ubicación texto libre con longitud limitada.

Siguientes pasos (futuros)
- Enlazar acción "Adoptar" cuando exista flujo de adopciones (F4).
- Mostrar etiqueta de estado (Disponible/Adoptado) y fecha de publicación si backend la expone.
- Añadir tests de unidad ligeros (composición) y smoke E2E básico para catálogo (F7).

