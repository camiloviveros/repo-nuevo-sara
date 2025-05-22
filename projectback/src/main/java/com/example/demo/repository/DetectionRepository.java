package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Detection;

@Repository
public interface DetectionRepository extends JpaRepository<Detection, Long> {
    
    // Obtener las últimas N detecciones ordenadas por timestamp descendente
    @Query("SELECT d FROM Detection d ORDER BY d.timestampMs DESC LIMIT 50")
    List<Detection> findTop50ByOrderByTimestampMsDesc();
    
    // Obtener detecciones por rango de tiempo
    @Query("SELECT d FROM Detection d WHERE d.timestampMs BETWEEN :startTime AND :endTime ORDER BY d.timestampMs ASC")
    List<Detection> findByTimestampRange(@Param("startTime") Long startTime, @Param("endTime") Long endTime);
    
    // Obtener todas las detecciones ordenadas por timestamp ascendente
    @Query("SELECT d FROM Detection d ORDER BY d.timestampMs ASC")
    List<Detection> findAllOrderByTimestamp();
    
    // Contar total de detecciones
    @Query("SELECT COUNT(d) FROM Detection d")
    Long countAllDetections();
    
    // Obtener detecciones por fecha específica
    @Query("SELECT d FROM Detection d WHERE d.date LIKE :datePattern ORDER BY d.timestampMs ASC")
    List<Detection> findByDatePattern(@Param("datePattern") String datePattern);
    
    // Obtener las últimas N detecciones (método alternativo)
    @Query(value = "SELECT * FROM detections ORDER BY timestamp_ms DESC LIMIT :limit", nativeQuery = true)
    List<Detection> findTopNByOrderByTimestampMsDesc(@Param("limit") int limit);
    
    // Verificar si existen datos en la tabla
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Detection d")
    boolean existsAnyDetection();
    
    // Obtener la detección más reciente
    @Query("SELECT d FROM Detection d ORDER BY d.timestampMs DESC LIMIT 1")
    Detection findMostRecentDetection();
    
    // Obtener detecciones que tengan datos de objetos totales no vacíos
    @Query("SELECT d FROM Detection d WHERE d.objectsTotal IS NOT NULL AND d.objectsTotal != '{}' AND d.objectsTotal != '' ORDER BY d.timestampMs DESC")
    List<Detection> findDetectionsWithObjectData();
    
    // Obtener detecciones que tengan datos de velocidad no vacíos
    @Query("SELECT d FROM Detection d WHERE d.avgSpeedByLane IS NOT NULL AND d.avgSpeedByLane != '{}' AND d.avgSpeedByLane != '' ORDER BY d.timestampMs DESC")
    List<Detection> findDetectionsWithSpeedData();
}