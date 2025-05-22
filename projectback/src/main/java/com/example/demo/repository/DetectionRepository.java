package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Detection;

@Repository
public interface DetectionRepository extends JpaRepository<Detection, Long> {
    
    // Obtener las últimas N detecciones
    List<Detection> findTop50ByOrderByTimestampMsDesc();
    
    // Obtener detecciones por rango de tiempo
    @Query("SELECT d FROM Detection d WHERE d.timestampMs BETWEEN ?1 AND ?2 ORDER BY d.timestampMs")
    List<Detection> findByTimestampRange(Long startTime, Long endTime);
    
    // Obtener todas las detecciones ordenadas por timestamp
    @Query("SELECT d FROM Detection d ORDER BY d.timestampMs")
    List<Detection> findAllOrderByTimestamp();
    
    // Contar total de detecciones
    @Query("SELECT COUNT(d) FROM Detection d")
    Long countAllDetections();
}