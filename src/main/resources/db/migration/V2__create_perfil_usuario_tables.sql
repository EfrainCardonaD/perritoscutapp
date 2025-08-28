-- Migración V2: Crear tablas para perfiles de usuario e imágenes de perfil
-- Fecha: 2025-01-09
-- Descripción: Añade funcionalidad de perfil extendido para usuarios

-- Tabla de perfiles de usuario (información adicional)
CREATE TABLE perfiles_usuario (
    id CHAR(36) PRIMARY KEY,
    usuario_id CHAR(36) NOT NULL,
    nombre_real VARCHAR(100),
    telefono VARCHAR(20),
    idioma VARCHAR(10) DEFAULT 'es',
    zona_horaria VARCHAR(50) DEFAULT 'America/Mexico_City',
    fecha_nacimiento DATE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_perfil_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT uk_perfil_usuario_id UNIQUE (usuario_id),
    CONSTRAINT uk_perfil_telefono UNIQUE (telefono)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de imágenes de perfil
CREATE TABLE imagenes_perfil (
    id CHAR(36) PRIMARY KEY,
    perfil_usuario_id CHAR(36) NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_archivo VARCHAR(500) NOT NULL,
    tipo_mime VARCHAR(100),
    tamano_bytes BIGINT,
    url_publica VARCHAR(500),
    activa BOOLEAN DEFAULT TRUE,
    fecha_subida TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT fk_imagen_perfil_usuario FOREIGN KEY (perfil_usuario_id) REFERENCES perfiles_usuario(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices para optimizar consultas
CREATE INDEX idx_perfil_usuario_id ON perfiles_usuario(usuario_id);
CREATE INDEX idx_perfil_telefono ON perfiles_usuario(telefono);
CREATE INDEX idx_perfil_fecha_creacion ON perfiles_usuario(fecha_creacion);

CREATE INDEX idx_imagen_perfil_usuario_id ON imagenes_perfil(perfil_usuario_id);
CREATE INDEX idx_imagen_activa ON imagenes_perfil(activa);
CREATE INDEX idx_imagen_fecha_subida ON imagenes_perfil(fecha_subida);
