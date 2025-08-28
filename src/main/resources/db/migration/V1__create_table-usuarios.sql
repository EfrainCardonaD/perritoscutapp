CREATE TABLE IF NOT EXISTS usuarios
(
    id                     CHAR(36) PRIMARY KEY,
    user_name              VARCHAR(100) UNIQUE NOT NULL,
    email                  VARCHAR(150) UNIQUE NOT NULL,
    password               VARCHAR(255)        NOT NULL,
    fecha_creacion         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultimo_acceso          TIMESTAMP,
    activo                 BOOLEAN   DEFAULT TRUE,
    token                  VARCHAR(255),
    fecha_expiracion_token TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
