-- V7: Verificación de contacto (EMAIL) y recuperación de contraseña

-- 1) Alter tabla usuarios: columnas de verificación y activación
ALTER TABLE usuarios ADD COLUMN email_verificado TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN telefono VARCHAR(20) NULL;
ALTER TABLE usuarios ADD COLUMN telefono_verificado TINYINT(1) NOT NULL DEFAULT 0;

-- Normalizar posibles NULLs en activo antes de forzar NOT NULL
UPDATE usuarios SET activo = 1 WHERE activo IS NULL;
-- Establecer default de activo a 0 para nuevos registros
ALTER TABLE usuarios MODIFY activo TINYINT(1) NOT NULL DEFAULT 0;

-- Marcar como verificados los usuarios que ya estaban activos (para no romper entornos existentes)
UPDATE usuarios SET email_verificado = 1 WHERE activo = 1 AND email_verificado = 0;

-- 2) Tabla verification_tokens
CREATE TABLE IF NOT EXISTS verification_tokens (
    id CHAR(36) PRIMARY KEY,
    usuario_id CHAR(36) NOT NULL,
    canal ENUM('EMAIL','PHONE') NOT NULL,
    token VARCHAR(255) NOT NULL,
    codigo_otp VARCHAR(10) NULL,
    expira_en TIMESTAMP NOT NULL,
    usado_en TIMESTAMP NULL,
    intentos INT DEFAULT 0,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_verif_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_verif_usuario_canal ON verification_tokens (usuario_id, canal, usado_en);
CREATE INDEX idx_verif_token ON verification_tokens (token);
CREATE INDEX idx_verif_otp ON verification_tokens (codigo_otp);

-- 3) Tabla tokens_reset
CREATE TABLE IF NOT EXISTS tokens_reset (
    id CHAR(36) PRIMARY KEY,
    usuario_id CHAR(36) NOT NULL,
    token VARCHAR(255) NOT NULL,
    expira_en TIMESTAMP NOT NULL,
    usado_en TIMESTAMP NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reset_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT uq_reset_token UNIQUE (token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_reset_usuario_usado ON tokens_reset (usuario_id, usado_en);

-- V7: Hacer nullable la columna url en imagenes_perros (idempotente)
SET @schema := DATABASE();
SET @is_not_nullable := (
    SELECT CASE WHEN UPPER(IS_NULLABLE)='NO' THEN 1 ELSE 0 END
    FROM information_schema.columns
    WHERE table_schema=@schema AND table_name='imagenes_perros' AND column_name='url'
);
SET @sql := IF(@is_not_nullable=1, 'ALTER TABLE imagenes_perros MODIFY url VARCHAR(500) NULL', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
