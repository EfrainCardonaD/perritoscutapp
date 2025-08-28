-- Añade la columna 'rol' faltante según la entidad Usuario (Enum STRING)
-- Se define como VARCHAR(20) con valor por defecto 'ROLE_USER' y NOT NULL
-- Compatible con MySQL/MariaDB

ALTER TABLE usuarios
    ADD COLUMN rol VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER';

-- Asegurar que filas existentes tengan un valor válido (por si el motor ignora default en add)
UPDATE usuarios SET rol = 'ROLE_USER' WHERE rol IS NULL;

