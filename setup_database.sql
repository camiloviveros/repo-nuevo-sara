-- Script para configurar la base de datos MySQL para el proyecto

-- Crear la base de datos si no existe
CREATE DATABASE IF NOT EXISTS detections 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- Usar la base de datos
USE detections;

-- Crear usuario si no existe (opcional, ya tienes el usuario root)
-- CREATE USER IF NOT EXISTS 'camilo'@'localhost' IDENTIFIED BY 'camilo';
-- GRANT ALL PRIVILEGES ON detections.* TO 'camilo'@'localhost';

-- Verificar que el usuario root tenga permisos
GRANT ALL PRIVILEGES ON detections.* TO 'root'@'localhost';
FLUSH PRIVILEGES;

-- Crear la tabla detections si no existe (Hibernate la creará automáticamente, pero por si acaso)
CREATE TABLE IF NOT EXISTS detections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp_ms BIGINT,
    date VARCHAR(50),
    objects_total TEXT,
    objects_by_lane TEXT,
    avg_speed_by_lane TEXT,
    INDEX idx_timestamp (timestamp_ms),
    INDEX idx_date (date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Verificar que la tabla existe
DESCRIBE detections;

-- Mostrar el conteo actual de registros
SELECT COUNT(*) as total_records FROM detections;

-- Mostrar algunos registros de ejemplo si existen
SELECT id, timestamp_ms, date, 
       SUBSTRING(objects_total, 1, 100) as objects_preview,
       SUBSTRING(objects_by_lane, 1, 50) as lanes_preview
FROM detections 
ORDER BY timestamp_ms DESC 
LIMIT 5;