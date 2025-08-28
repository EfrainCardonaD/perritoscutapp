-- Triggers y restricciones de negocio para adopciones
-- Motor: MySQL 8

-- 1) Validar que un perro aprobado tenga al menos una imagen
DROP TRIGGER IF EXISTS trg_perros_before_update_validar_imagen;
CREATE TRIGGER trg_perros_before_update_validar_imagen
BEFORE UPDATE ON perros
FOR EACH ROW
BEGIN
    IF NEW.estado_revision = 'Aprobado' AND (SELECT COUNT(*) FROM imagenes_perros WHERE perro_id = NEW.id) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Perro debe tener al menos una imagen antes de ser aprobado';
    END IF;
END;

-- 2) Evitar "Disponible" si el perro no está aprobado
DROP TRIGGER IF EXISTS trg_perros_before_update_disponible_si_aprobado;
CREATE TRIGGER trg_perros_before_update_disponible_si_aprobado
BEFORE UPDATE ON perros
FOR EACH ROW
BEGIN
    IF NEW.estado_adopcion = 'Disponible' AND NEW.estado_revision <> 'Aprobado' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo perros aprobados pueden estar Disponibles';
    END IF;
END;

-- 2.1) Evitar solicitudes para perros no aprobados/no disponibles y evitar auto-solicitud
DROP TRIGGER IF EXISTS trg_solicitudes_before_insert_validaciones;
CREATE TRIGGER trg_solicitudes_before_insert_validaciones
BEFORE INSERT ON solicitudes_adopcion
FOR EACH ROW
BEGIN
    -- Perro debe existir
    IF NOT EXISTS (SELECT 1 FROM perros WHERE id = NEW.perro_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Perro no existe';
    END IF;
    -- Perro debe estar aprobado y disponible
    IF NOT EXISTS (
        SELECT 1 FROM perros
         WHERE id = NEW.perro_id
           AND estado_revision = 'Aprobado'
           AND estado_adopcion = 'Disponible'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se puede solicitar adopción de perros aprobados y disponibles';
    END IF;
    -- El dueño no puede solicitar su propio perro
    IF EXISTS (SELECT 1 FROM perros WHERE id = NEW.perro_id AND usuario_id = NEW.solicitante_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El dueño no puede solicitar su propio perro';
    END IF;
END;

-- 3) Validar que una solicitud en revisión/aceptada tenga ambos documentos requeridos
DROP TRIGGER IF EXISTS trg_solicitudes_before_update_validar_docs;
CREATE TRIGGER trg_solicitudes_before_update_validar_docs
BEFORE UPDATE ON solicitudes_adopcion
FOR EACH ROW
BEGIN
    IF NEW.estado IN ('En revisión','Aceptada') THEN
        IF (
            SELECT COUNT(DISTINCT tipo_documento)
              FROM documentos_solicitud
             WHERE solicitud_id = NEW.id
               AND tipo_documento IN ('Identificacion','CartaResponsiva')
        ) < 2 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La solicitud requiere Identificacion y CartaResponsiva';
        END IF;
    END IF;
END;

-- 3.1) Validar que el perro siga disponible al aceptar
DROP TRIGGER IF EXISTS trg_solicitudes_before_update_validar_disponibilidad;
CREATE TRIGGER trg_solicitudes_before_update_validar_disponibilidad
BEFORE UPDATE ON solicitudes_adopcion
FOR EACH ROW
BEGIN
    IF NEW.estado = 'Aceptada' THEN
        IF (
            SELECT estado_adopcion FROM perros WHERE id = NEW.perro_id
        ) <> 'Disponible' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El perro ya no está disponible';
        END IF;
    END IF;
END;

-- 4) Sincronizar estado de perro a "Adoptado" cuando una solicitud es aceptada
DROP TRIGGER IF EXISTS trg_solicitudes_after_update_sync_perro;
CREATE TRIGGER trg_solicitudes_after_update_sync_perro
AFTER UPDATE ON solicitudes_adopcion
FOR EACH ROW
BEGIN
    IF NEW.estado = 'Aceptada' THEN
        UPDATE perros SET estado_adopcion = 'Adoptado' WHERE id = NEW.perro_id;
    END IF;
END;

-- 4.1) Rechazar otras solicitudes del mismo perro al aceptar una
DROP TRIGGER IF EXISTS trg_solicitudes_after_update_rechazar_otras;
CREATE TRIGGER trg_solicitudes_after_update_rechazar_otras
AFTER UPDATE ON solicitudes_adopcion
FOR EACH ROW
BEGIN
    IF NEW.estado = 'Aceptada' THEN
        UPDATE solicitudes_adopcion
           SET estado = 'Rechazada'
         WHERE perro_id = NEW.perro_id
           AND id <> NEW.id
           AND estado IN ('Pendiente','En revisión');
    END IF;
END;

-- 5) Evitar poner a un perro en "Adoptado" sin solicitud aceptada
DROP TRIGGER IF EXISTS trg_perros_before_update_validar_adoptado;
CREATE TRIGGER trg_perros_before_update_validar_adoptado
BEFORE UPDATE ON perros
FOR EACH ROW
BEGIN
    IF NEW.estado_adopcion = 'Adoptado' AND OLD.estado_adopcion <> 'Adoptado' THEN
        IF (SELECT COUNT(*) FROM solicitudes_adopcion WHERE perro_id = NEW.id AND estado = 'Aceptada') = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se puede marcar como Adoptado sin solicitud aceptada';
        END IF;
    END IF;
END;
