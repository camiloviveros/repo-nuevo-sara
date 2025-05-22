package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.demo.service.DTO.DetectionJson;

@Service
public class AnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalysisService.class);

    public void processDetection(String strategyType, DetectionJson detection) {
        try {
            logger.debug("🔍 Procesando detección tipo: {} con timestamp: {}", 
                        strategyType, detection.getTimestamp_ms());
            
            // Análisis simplificado sin pattern Strategy
            switch (strategyType.toLowerCase()) {
                case "vehicle":
                    processVehicleDetection(detection);
                    break;
                case "axle":
                    processAxleCount(detection);
                    break;
                default:
                    logger.debug("⚠️ Tipo de análisis no reconocido: {}", strategyType);
            }
            
        } catch (Exception e) {
            logger.error("❌ Error procesando detección: {}", e.getMessage());
        }
    }
    
    private void processVehicleDetection(DetectionJson detection) {
        try {
            if (detection.getObjects_total() != null) {
                int totalVehicles = detection.getObjects_total().values().stream()
                    .mapToInt(Integer::intValue).sum();
                logger.debug("🚗 Total de vehículos: {}", totalVehicles);
            }
            
            if (detection.getAvg_speed_by_lane() != null) {
                detection.getAvg_speed_by_lane().forEach((lane, speed) -> 
                    logger.debug("🛣️ Velocidad en {}: {:.2f} km/h", lane, speed));
            }
            
        } catch (Exception e) {
            logger.warn("⚠️ Error en análisis de vehículos: {}", e.getMessage());
        }
    }
    
    private void processAxleCount(DetectionJson detection) {
        logger.debug("🔧 Procesando conteo de ejes para timestamp: {}", detection.getTimestamp_ms());
        // Implementación futura para conteo de ejes
    }
}