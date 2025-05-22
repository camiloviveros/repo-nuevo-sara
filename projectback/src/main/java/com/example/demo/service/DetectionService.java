package com.example.demo.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Detection;
import com.example.demo.repository.DetectionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DetectionService {

    private static final Logger logger = LoggerFactory.getLogger(DetectionService.class);
    private final DetectionRepository detectionRepository;

    public void saveDetections(List<Detection> detections) {
        try {
            logger.info("💾 Guardando {} detecciones en la base de datos...", detections.size());
            detectionRepository.saveAll(detections);
            logger.info("✅ Detecciones guardadas exitosamente.");
        } catch (Exception e) {
            logger.error("❌ Error guardando detecciones: {}", e.getMessage());
            throw e;
        }
    }
    
    public long countDetections() {
        return detectionRepository.count();
    }
    
    public List<Detection> getAllDetections() {
        return detectionRepository.findAllOrderByTimestamp();
    }
    
    public List<Detection> getRecentDetections() {
        return detectionRepository.findTop50ByOrderByTimestampMsDesc();
    }
}