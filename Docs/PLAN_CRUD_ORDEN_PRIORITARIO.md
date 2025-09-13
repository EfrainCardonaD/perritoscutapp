# Plan Prioritario y Dependencias para Implementación CRUD + Imágenes (Cloudinary)

Objetivo: Reordenar el plan previo optimizando dependencias técnicas (DB, servicios de almacenamiento, lógica de negocio) para minimizar retrabajo y riesgos de datos huérfanos.

---
## 0. Estado de Avance (Progreso Incremental)
Resumen rápido para retomar sin releer todo:
- Completado:
  - Extensión ImageStorageService con deleteProfileImage (Fase 1 / Micro 14.1)
  - Implementado deleteProfileImage en Cloudinary y Local (Fase 1)
  - CRUD parcial Perro: GET detalle, PATCH (actualizar), DELETE implementados (Fases 3,4,5)
  - Validaciones e infraestructura de imágenes (planUpdateImagenes, attach, helpers)
  - Endpoint eliminación imagen suelta: DELETE /api/imagenes/perritos/{id}
  - Perfil: DTO actualización (DtoActualizarPerfilRequest), método actualizarPerfilCampos y reemplazo imagen con borrado anterior
  - Endpoint PATCH /api/perfil añadido
  - Borrado lógico de usuario (DELETE /api/usuarios/admin/{id}) marcando activo=false sin eliminar datos
- Decisión: Omitidas pruebas unitarias en esta iteración (documentado)
- Pendiente inmediato:
  1. (Opcional) Endpoint GET /api/solicitudes/{id}
  2. (Opcional) DELETE /api/solicitudes/{id}
  3. Documentar endpoints nuevos en API_DOCUMENTATION.md (si procede)
- Evaluar si se requiere documentación adicional en API_DOCUMENTATION.md

---
## 1. Mapa de Entidades y Dependencias
| Entidad | Relación Imágenes | Dependencias externas | Observaciones |
|---------|-------------------|------------------------|---------------|
| Perro (perros) | 1..N ImagenPerro | ImageStorageService (dog) | Requiere control principal única y máx 5 |
| ImagenPerro (imagenes_perros) | FK perro_id | Cloudinary (folder perros) | Orphan si se sube y no se asocia |
| PerfilUsuario (perfil_usuarios) | 1 ImagenPerfil (opcional) | ImageStorageService (profile) | Reemplazo implica borrar anterior |
| ImagenPerfil (imagenes_perfil) | FK perfil_usuario_id | Cloudinary (folder perfiles) | Nombre de archivo derivado del UUID |
| Usuario (usuarios) | 1 PerfilUsuario | Seguridad / Auth | Borrado debe cascada o soft-delete |
| SolicitudAdopcion | Documentos lógicos | (Futuro: storage) | Hoy sin archivos binarios |

---
## 2. Cadena de Dependencias Críticas
1. DB consistente (schema, constraints, cascadas JPA) antes de añadir lógica de borrado.
2. Interface de almacenamiento debe exponer TODOS los métodos de eliminación antes de que los servicios los usen (deleteDogImage, deleteProfileImage, futura verificación isPresent?).
3. Servicios de dominio (PerroService, PerfilUsuarioService) deben incorporar lógica interna de actualización/borrado ANTES de exponer endpoints públicos (controlador) para reducir iteraciones en API.
4. Endpoints de lectura (GET detalle) se agregan antes de PATCH/DELETE para facilitar pruebas y validaciones.
5. Pruebas unitarias/integración básicas deben existir antes de introducir lógica de eliminación en cascada (fail-fast si se rompe).

---
## 3. Riesgos de Orden Incorrecto
| Riesgo | Si se hace demasiado pronto | Mitigación en este plan |
|--------|-----------------------------|--------------------------|
| Borrar imágenes sin fallback | Exponer DELETE perro antes de deleteProfileImage implementado | Implementar interface completa primero |
| Transacciones largas + llamadas externas | Llamar Cloudinary dentro de @Transactional sin estrategia | Pre-colectar IDs fuera o usar patrón try-catch compensación |
| Huérfanas acumuladas | Subir imágenes sin limpieza inicial | Añadir etiquetado temporal y job al final |
| Inconsistencia principal | PATCH perro antes de validadores unificados | Centralizar validación en método interno validateImagenes() |

---
## 4. Principios de Implementación
- "Interface First": ampliar ImageStorageService y Cloudinary impl antes de tocar servicios.
- "Read Before Mutate": crear endpoints GET detalle para permitir pruebas de estado.
- "Atomic Domain Update": mutaciones (PATCH perro/perfil) encapsulan validaciones + recolección de IDs a borrar.
- "Borrado Seguro": invocar Cloudinary fuera de la transacción principal (o antes de commit) almacenando errores en log sin interrumpir operación si DB ya consistente.
- "Idempotencia": Métodos delete deben tolerar que Cloudinary ya no tenga el recurso.

---
## 5. Fases Priorizadas
### Fase 0: Auditoría y Base
- Verificar entidades JPA: orphanRemoval en Perro.imagenes (OK) y relación PerfilUsuario -> ImagenPerfil (si existe, confirmar). 
- Confirmar que public_id == UUID almacenado es estable (sí).
- Decidir estrategia de borrado usuario: HARD (simple) inicialmente.
- Añadir índice (si falta) en imagenes_perros(perro_id) (revisión posterior en DB). (Documentar si no se gestiona aquí).

Entrega: Documento de verificación + (opcional) script SQL índice.

### Fase 1: Infra Almacenamiento
- Extender ImageStorageService: deleteProfileImage(String id).
- CloudinaryImageStorageService: implementar deleteProfileImage.
- (Opcional) Añadir método resolveDogImagePublicUrl(id) idempotente (ya presente) + nota de no necesidad de URL en BD a futuro.
- No exponer endpoints nuevos aún.

### Fase 2: Métodos Internos en Servicios (sin Controlador)
- PerroService: agregar obtenerPerro(String id) (read-only).
- PerroService: refactor internamente creación de imágenes en método privado attachImagenes(Perro, List<Request>). (Preparar para reutilizar en PATCH).
- PerroService: agregar método interno planUpdateImagenes(perro, request) -> resultado (nuevas, a eliminar, mantener, validar principal única, límite 5).
- PerfilUsuarioService: agregar actualizarPerfilCampos(DtoActualizarPerfilRequest) (sin imagen).
- PerfilUsuarioService: agregar reemplazarImagenPerfil(idNuevo) (borrar anterior si existe via deleteProfileImage).

### Fase 3: Endpoints de Lectura
- GET /api/perros/{id}
- GET /api/solicitudes/{id} (si requerido temprano; si no, mover a Fase 6).

### Fase 4: Mutaciones - Update
- PATCH /api/perros/{id}
  - Usa planUpdateImagenes para obtener lista a borrar (Cloudinary después).
  - Aplica validaciones (principal única, límite, ownership).
- PATCH /api/perfil (campos básicos + opcional imagen en endpoint separado ya existente).
- Tests: crear->patch (cambiar principal, agregar/quitar imágenes) -> verificar estado final.

### Fase 5: Borrados Principales
- DELETE /api/perros/{id}
  - Flujo: cargar perro + IDs imágenes -> (opcional) marcar en log -> eliminar perro (DB) -> intentar borrar en Cloudinary cada ID.
  - Si falla Cloudinary, log warn.
- DELETE /api/usuarios/{id} (ADMIN) (simple: cargar usuario, si tiene perfil e imagen -> borrar imagen -> borrar usuario).
- Tests: creación perro + delete (verificar ausencia en repo, Cloudinary mockeado si se introduce test-doubles).

### Fase 6: Complementos y Limpieza
- DELETE /api/imagenes/perritos/{id} (solo si no asociada a perro).
  - Verifica inexistencia en repositorioImagenPerro antes de borrar Cloudinary.
- (Opcional) DELETE /api/solicitudes/{id} (validar estado PENDIENTE).
- Endpoint diagnóstico: GET /api/admin/imagenes/orphans (lista IDs sin asociación (si se guarda staging table) o no implementado si no hay tracking).

### Fase 7: Job Purga Huérfanas (Opcional)
- Estrategia: crear tabla staging_subidas (uuid, created_at, tipo, asociado BOOLEAN).
- Cron: selecciona donde asociado=false y created_at < NOW() - 24h; intenta delete en Cloudinary; elimina fila.

---
## 6. Flujo Estándar de PATCH Perro (Contrato Interno)
1. Cargar perro con imágenes (lock optimista por @Version).
2. Mapear imágenes actuales -> set IDs.
3. Validar que request.imagenes no exceda límite y no repita IDs.
4. Identificar principal (exactamente 1).
5. Calcular:
   - toKeep = intersección
   - toAdd = nuevas (IDs no presentes)
   - toRemove = actuales - request
6. Persistir nuevas filas ImagenPerro (sin borrar todavía en Cloudinary).
7. Remover filas ImagenPerro toRemove.
8. Actualizar atributos perro.
9. Fuera (o después) de transacción: intentar deleteDogImage para cada id in toRemove.

Edge cases: request sin imágenes -> reject; más de una principal -> reject; principal eliminada y no reemplazada -> reject.

---
## 7. Flujo Estándar DELETE Perro
1. Cargar perro + imágenes (solo IDs).
2. Guardar lista imageIds.
3. repositorioPerro.delete(perro) (JPA cascada elimina ImagenPerro filas).
4. Iterar imageIds -> imageStorageService.deleteDogImage(id).
5. Loggear resultado.

Motivo de orden: si Cloudinary falla no bloquea consistencia de BD (preferimos fuga temporal vs rollback prolongado).

---
## 8. Transaccionalidad Recomendada
| Operación | @Transactional | Llamadas Cloudinary | Notas |
|-----------|----------------|----------------------|-------|
| Crear Perro | Sí | Upload ya ocurrió antes (se asume) | Solo persistencia |
| PATCH Perro | Sí (persist cambios) | Borrado imágenes removidas DESPUÉS | Evita locks largos |
| DELETE Perro | Sí (delete BD) | Después commit | Minimiza riesgo rollback parcial |
| Reemplazar Imagen Perfil | Sí (persist ref) | Borrar anterior después | Mantener siempre una referenciada |

---
## 9. Validaciones Centralizadas (Funciones Sugeridas)
- validatePrincipalUnica(listaImagenesRequest)
- validateLimiteImagenes(lista, MAX=5)
- validateOwnership(perro.usuarioId == authUserId OR rolAdmin)
- validateIdsFormatoUUID

Reutilizar en crear y patch.

---
## 10. Testing Priorizado
| Fase | Test mínimo |
|------|-------------|
| Fase 2 | PerroService.obtener retorna DTO correcto |
| Fase 4 | Patch: cambiar principal; eliminar imagen; agregar nueva; error >5; error sin principal |
| Fase 5 | Delete: elimina perro e imágenes JPA (repos vacíos) |
| Fase 6 | Delete imagen suelta: prohíbe si asociada |
| Fase 6/7 | (Opcional) Orphans job no borra asociadas |

Mock: CloudinaryImageStorageService en tests -> verificar invocaciones deleteDogImage(ids esperados).

---
## 11. Métricas de Éxito
- 0 referencias a imágenes inexistentes en catálogos (HEAD 404 rate < 0.5%).
- Tiempo medio PATCH perro < 300ms (sin latencia Cloudinary en transacción).
- Imágenes huérfanas (staging) < 2% sobre total subidas semanales.

---
## 12. Checklist Final (Reordenada)
-[ ] Auditoría entidades / constraints (Fase 0)
-[x] Extensión ImageStorageService.deleteProfileImage (Fase 1)
-[x] Implementación Cloudinary deleteProfileImage (Fase 1)
-[x] Métodos internos servicio Perro (obtener) (Fase 2 - parcial)
-[x] Refactor attachImagenes (Fase 2)
-[x] Helpers validación imágenes (Fase 2)
-[x] DTO ActualizarPerroRequest (Fase 2)
-[x] planUpdateImagenes (Fase 2 - sin tests) 
-[-] Tests unitarios planUpdateImagenes (OMITIDO)
-[x] Método actualizarPerro (Fase 4)
-[x] PATCH /api/perros/{id} (Fase 4)
-[x] DELETE /api/perros/{id} (Fase 5)
-[x] DELETE /api/imagenes/perritos/{id} (Fase 6)
-[x] Métodos internos Perfil (actualizar campos, reemplazo imagen) (Fase 4)
-[x] PATCH /api/perfil (Fase 4)
-[x] GET /api/perros/{id} (Fase 3)
-[x] DELETE /api/usuarios/{id} (Fase 5 - lógico)
-[x] GET /api/solicitudes/{id} (Fase 6)
-[x] (Opc) DELETE /api/solicitudes/{id} (Fase 6)
+[x] Auditoría entidades / constraints (Fase 0)
+[x] Extensión ImageStorageService.deleteProfileImage (Fase 1)
+[x] Implementación Cloudinary deleteProfileImage (Fase 1)
+[x] Métodos internos servicio Perro (obtener) (Fase 2 - parcial)
+[x] Refactor attachImagenes (Fase 2)
+[x] Helpers validación imágenes (Fase 2)
+[x] DTO ActualizarPerroRequest (Fase 2)
+[x] planUpdateImagenes (Fase 2 - sin tests)
+[-] Tests unitarios planUpdateImagenes (OMITIDO)
+[x] Método actualizarPerro (Fase 4)
+[x] PATCH /api/perros/{id} (Fase 4)
+[x] DELETE /api/perros/{id} (Fase 5)
+[x] DELETE /api/imagenes/perritos/{id} (Fase 6)
+[x] Métodos internos Perfil (actualizar campos, reemplazo imagen) (Fase 4)
+[x] PATCH /api/perfil (Fase 4)
+[x] GET /api/perros/{id} (Fase 3)
+[x] DELETE /api/usuarios/{id} (Fase 5 - lógico)
+[>] Decidir implementación de endpoints de solicitudes (Fase 6) - En progreso
+[x] Documentar endpoints añadidos (PATCH /api/perfil, DELETE imagen suelta, DELETE usuario lógico)
+[>] Job purga huérfanas (Fase 7) - Implementación con tabla staging_subidas y servicio
+[-] Suite de tests mínima (OMITIDA)

---
## 13. Próximos Pasos Inmediatos (Concretos)
-1. [>] Decidir implementación de endpoints de solicitudes (GET / DELETE) si siguen en alcance. - En progreso
-2. [>] Documentar endpoints añadidos (PATCH /api/perfil, DELETE imagen suelta, DELETE usuario lógico). - En progreso
-3. [x] Implementar job de purga huérfanas con tabla staging_subidas y servicio dedicado. - Completado
-4. [ ] Cerrar plan marcando tareas opcionales como fuera de alcance si no se abordarán.
+1. [>] Decidir implementación de endpoints de solicitudes (GET / DELETE) si siguen en alcance. - En progreso
+2. [x] Documentar endpoints añadidos (PATCH /api/perfil, DELETE imagen suelta, DELETE usuario lógico). - Completado (ver API_DOCUMENTATION.md)
+3. [>] Definir alcance real del job de purga huérfanas (si se mantiene) y crear stub. - En progreso (stub creado)
+4. [ ] Cerrar plan marcando tareas opcionales como fuera de alcance si no se abordarán.

---
## 14. Micro-Entregas Basadas en DoD (Integración Progresiva)
Secuencia granular que no rompe comportamiento existente. Cada punto debe compilar y (si aplica) mantener tests verdes.

### 14.1 Preparación (encaja en Fase 0 / inicio Fase 1)
- Añadir comentarios TODO en PerroService y PerfilUsuarioService referenciando pasos futuros.
- Extender ImageStorageService con deleteProfileImage(String id) default no-op.
- Implementar override en CloudinaryImageStorageService.
- (Opcional) Añadir stub en LocalImageStorageService si se usa en perfiles.

### 14.2 Lectura y Refactor Interno (Fase 2 primera parte)
- Método obtenerPerro(id) -> DtoPerro (read-only).
- Extraer attachImagenes(...) de lógica de creación.
- Añadir helpers validateLimiteImagenes / validatePrincipalUnica reutilizados por crear.
- Crear DTO ActualizarPerroRequest (sin uso todavía).

### 14.3 Exposición Inicial (Fase 3)
- GET /api/perros/{id} usando obtenerPerro.
- Test unitario obtenerPerro (happy path + not found).

### 14.4 Preparar Actualización (Fase 4 pre-commit)
- Implementar planUpdateImagenes(...) sin exponer endpoint.
- Tests unitarios de plan (escenarios: cambiar principal, remover, exceder límite, duplicados).

### 14.5 Activar PATCH (Fase 4)
- Endpoint PATCH /api/perros/{id} consumiendo ActualizarPerroRequest.
- Uso de planUpdateImagenes + post-commit deletes Cloudinary.
- Tests integración crear -> patch.

### 14.6 Borrado Seguro (Fase 5)
- Implementar eliminarPerro(id) en servicio (capturar IDs antes de delete).
- Endpoint DELETE /api/perros/{id}.
- Tests: verificar repos vacíos + invocaciones a deleteDogImage (mock).

### 14.7 Perfil e Imagen Perfil (Fase 4 paralela)
- DTO DtoActualizarPerfilRequest + método actualizarPerfilCampos.
- Reemplazo imagen perfil: borrar anterior (deleteProfileImage) tras persistir nueva.

### 14.8 Limpieza y Orphans (Fase 6/7)
- Endpoint DELETE imagen perro suelta (verifica no asociada).
- (Opc) listado orphans si se implementa staging.
- Job purga (cron) + test superficial.

### 14.9 End Game / DoD Check
- Revisar métricas iniciales (logs Cloudinary deletes vs imágenes creadas).
- Ejecutar suite completa tests.
- Marcar checklist final en README técnico.

---
## 15. Cierre
El orden de micro-entregas asegura primero capacidad de lectura y validación, luego mutaciones controladas, y finalmente limpieza y automatizaciones sin bloquear entregas tempranas.

---
Fin del plan prioritario.

## 16. Nota sobre Borrado Lógico de Usuarios
- Endpoint: DELETE /api/usuarios/admin/{id}
- Efecto: usuario.activo=false, se limpian token y fechaExpiracionToken, no se elimina imagen de perfil ni registros asociados.
- Razón: preservar historial y trazabilidad.
- Considerar (futuro): flag de visibilidad para ocultar contenido público del usuario desactivado.
