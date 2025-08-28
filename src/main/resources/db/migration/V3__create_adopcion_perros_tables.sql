-- Perritos Cut - Esquema de adopción
-- Motor: MySQL (InnoDB), Charset: utf8mb4

-- Tabla: perros
CREATE TABLE IF NOT EXISTS perros (
    id CHAR(36) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    edad INT,
    sexo ENUM('Macho','Hembra'),
    tamano ENUM('Pequeño','Mediano','Grande'),
    raza VARCHAR(100),
    descripcion TEXT,
    ubicacion VARCHAR(255),
    fecha_publicacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado_adopcion ENUM('Disponible','Adoptado','Pendiente','No disponible') DEFAULT 'Pendiente',
    estado_revision ENUM('Pendiente','Aprobado','Rechazado') DEFAULT 'Pendiente',
    usuario_id CHAR(36) NOT NULL,
    revisado_por CHAR(36),
    fecha_revision TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_perro_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_perro_revisado_por FOREIGN KEY (revisado_por) REFERENCES usuarios(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices útiles para catálogo y filtros (idempotentes)
SET @schema := DATABASE();
-- idx_perro_catalogo
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='perros' AND index_name='idx_perro_catalogo');
SET @sql := IF(@exists=0, 'CREATE INDEX idx_perro_catalogo ON perros (estado_revision, estado_adopcion)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- idx_perro_usuario
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='perros' AND index_name='idx_perro_usuario');
SET @sql := IF(@exists=0, 'CREATE INDEX idx_perro_usuario ON perros (usuario_id)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- idx_perro_fecha_publicacion
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='perros' AND index_name='idx_perro_fecha_publicacion');
SET @sql := IF(@exists=0, 'CREATE INDEX idx_perro_fecha_publicacion ON perros (fecha_publicacion)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- idx_perro_sexo
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='perros' AND index_name='idx_perro_sexo');
SET @sql := IF(@exists=0, 'CREATE INDEX idx_perro_sexo ON perros (sexo)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- idx_perro_tamano
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='perros' AND index_name='idx_perro_tamano');
SET @sql := IF(@exists=0, 'CREATE INDEX idx_perro_tamano ON perros (tamano)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- idx_perro_ubicacion
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='perros' AND index_name='idx_perro_ubicacion');
SET @sql := IF(@exists=0, 'CREATE INDEX idx_perro_ubicacion ON perros (ubicacion)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- Tabla: imagenes_perros
CREATE TABLE IF NOT EXISTS imagenes_perros (
    id CHAR(36) PRIMARY KEY,
    perro_id CHAR(36) NOT NULL,
    url VARCHAR(500) NOT NULL,
    descripcion VARCHAR(255),
    principal TINYINT(1) DEFAULT 0,
    -- Columna generada para permitir unicidad de la imagen principal por perro
    principal_flag TINYINT(1) GENERATED ALWAYS AS (CASE WHEN principal = 1 THEN 1 ELSE NULL END) STORED,
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_imagen_perro FOREIGN KEY (perro_id) REFERENCES perros(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices: búsqueda por perro y unicidad de principal por perro (permite múltiples NULLs)
-- idx_imagenes_perro_id
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='imagenes_perros' AND index_name='idx_imagenes_perro_id');
SET @sql := IF(@exists=0, 'CREATE INDEX idx_imagenes_perro_id ON imagenes_perros (perro_id)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- idx_imagenes_perro_principal
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='imagenes_perros' AND index_name='idx_imagenes_perro_principal');
SET @sql := IF(@exists=0, 'CREATE INDEX idx_imagenes_perro_principal ON imagenes_perros (perro_id, principal)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- uk_imagen_principal_perro
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='imagenes_perros' AND index_name='uk_imagen_principal_perro');
SET @sql := IF(@exists=0, 'CREATE UNIQUE INDEX uk_imagen_principal_perro ON imagenes_perros (perro_id, principal_flag)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- Tabla: solicitudes_adopcion
CREATE TABLE IF NOT EXISTS solicitudes_adopcion (
    id CHAR(36) PRIMARY KEY,
    perro_id CHAR(36) NOT NULL,
    solicitante_id CHAR(36) NOT NULL,
    estado ENUM('Pendiente','En revisión','Aceptada','Rechazada','Cancelada') DEFAULT 'Pendiente',
    -- Flag generado para restringir una sola solicitud activa por perro/solicitante
    activo_flag TINYINT(1) GENERATED ALWAYS AS (CASE WHEN estado IN ('Pendiente','En revisión') THEN 1 ELSE NULL END) STORED,
    -- Flag generado para restringir una sola solicitud aceptada por perro
    accepted_flag TINYINT(1) GENERATED ALWAYS AS (CASE WHEN estado = 'Aceptada' THEN 1 ELSE NULL END) STORED,
    mensaje TEXT,
    fecha_solicitud TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_respuesta TIMESTAMP NULL,
    revisado_por CHAR(36),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_solicitud_perro FOREIGN KEY (perro_id) REFERENCES perros(id) ON DELETE CASCADE,
    CONSTRAINT fk_solicitud_solicitante FOREIGN KEY (solicitante_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT fk_solicitud_revisor FOREIGN KEY (revisado_por) REFERENCES usuarios(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices y unicidad de solicitud activa
-- idx_solicitudes_perro
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='solicitudes_adopcion' AND index_name='idx_solicitudes_perro');
SET @sql := IF(@exists=0, 'CREATE INDEX idx_solicitudes_perro ON solicitudes_adopcion (perro_id)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- idx_solicitudes_solicitante
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='solicitudes_adopcion' AND index_name='idx_solicitudes_solicitante');
SET @sql := IF(@exists=0, 'CREATE INDEX idx_solicitudes_solicitante ON solicitudes_adopcion (solicitante_id)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- idx_solicitudes_estado
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='solicitudes_adopcion' AND index_name='idx_solicitudes_estado');
SET @sql := IF(@exists=0, 'CREATE INDEX idx_solicitudes_estado ON solicitudes_adopcion (estado)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- idx_solicitudes_fecha
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='solicitudes_adopcion' AND index_name='idx_solicitudes_fecha');
SET @sql := IF(@exists=0, 'CREATE INDEX idx_solicitudes_fecha ON solicitudes_adopcion (fecha_solicitud)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- uk_solicitud_activa
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='solicitudes_adopcion' AND index_name='uk_solicitud_activa');
SET @sql := IF(@exists=0, 'CREATE UNIQUE INDEX uk_solicitud_activa ON solicitudes_adopcion (perro_id, solicitante_id, activo_flag)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
-- uk_solicitud_aceptada_perro
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='solicitudes_adopcion' AND index_name='uk_solicitud_aceptada_perro');
SET @sql := IF(@exists=0, 'CREATE UNIQUE INDEX uk_solicitud_aceptada_perro ON solicitudes_adopcion (perro_id, accepted_flag)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;


-- Tabla: documentos_solicitud
CREATE TABLE IF NOT EXISTS documentos_solicitud (
    id CHAR(36) PRIMARY KEY,
    solicitud_id CHAR(36) NOT NULL,
    tipo_documento ENUM('Identificacion','CartaResponsiva') NOT NULL,
    url_documento VARCHAR(500) NOT NULL,
    nombre_archivo VARCHAR(255),
    tipo_mime VARCHAR(100),
    tamano_bytes BIGINT,
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documento_solicitud FOREIGN KEY (solicitud_id) REFERENCES solicitudes_adopcion(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Un documento por tipo por solicitud
SET @exists := (SELECT COUNT(1) FROM information_schema.statistics WHERE table_schema=@schema AND table_name='documentos_solicitud' AND index_name='uk_documento_por_tipo');
SET @sql := IF(@exists=0, 'CREATE UNIQUE INDEX uk_documento_por_tipo ON documentos_solicitud (solicitud_id, tipo_documento)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
