-- Agrega columnas de version para soporte de @Version (bloqueo optimista)
-- Compatible con MySQL usando verificación en information_schema

SET @schema := DATABASE();

-- perros.version
SET @exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema=@schema AND table_name='perros' AND column_name='version'
);
SET @sql := IF(@exists=0, 'ALTER TABLE perros ADD COLUMN version BIGINT NOT NULL DEFAULT 0', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- solicitudes_adopcion.version
SET @exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema=@schema AND table_name='solicitudes_adopcion' AND column_name='version'
);
SET @sql := IF(@exists=0, 'ALTER TABLE solicitudes_adopcion ADD COLUMN version BIGINT NOT NULL DEFAULT 0', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

