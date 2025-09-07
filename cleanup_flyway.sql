-- Script para limpiar base de datos y resetear Flyway
-- Ejecutar manualmente en MySQL si es necesario

-- Eliminar tablas problemáticas si existen
DROP TABLE IF EXISTS imagenes_perfil;
DROP TABLE IF EXISTS perfiles_usuario;

-- Limpiar historial de Flyway para la migración V2
-- Opción 1: Desactivar safe mode temporalmente
SET SQL_SAFE_UPDATES = 0;
DELETE FROM flyway_schema_history WHERE version = '2';
SET SQL_SAFE_UPDATES = 1;

-- Opción 2: Si lo anterior no funciona, usar esta consulta más específica
-- DELETE FROM flyway_schema_history WHERE version = '2' AND script = 'V2__create_perfil_usuario_tables.sql';
