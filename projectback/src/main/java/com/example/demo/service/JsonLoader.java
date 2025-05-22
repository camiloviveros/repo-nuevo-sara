package com.example.demo.service;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.entity.Detection;
import com.example.demo.repository.DetectionRepository;
import com.example.demo.service.DTO.DetectionJson;
import com.example.demo.service.DTO.DetectionsWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JsonLoader {

    private static final Logger logger = LoggerFactory.getLogger(JsonLoader.class);

    private final DetectionRepository detectionRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.detections.file-path}")
    private String filePath;

    public void loadJsonAndSaveToDb() throws Exception {
        File jsonFile = new File(filePath);
        if (!jsonFile.exists()) {
            logger.error("El archivo JSON no existe: {}", filePath);
            throw new IllegalArgumentException("El archivo JSON no existe: " + filePath);
        }
        
        try {
            logger.info("📁 Leyendo el archivo JSON desde: {}", filePath);

            DetectionsWrapper wrapper = objectMapper.readValue(jsonFile, DetectionsWrapper.class);
            logger.info("✅ Archivo JSON cargado exitosamente.");

            List<DetectionJson> detectionsJson = wrapper.getDetections();
            if (detectionsJson == null || detectionsJson.isEmpty()) {
                logger.warn("⚠️ No se encontraron detecciones en el archivo JSON.");
                return;
            }

            // Limpiar datos existentes para evitar duplicados
            logger.info("🧹 Limpiando datos existentes...");
            detectionRepository.deleteAll();

            List<Detection> detections = detectionsJson.stream()
                .map(d -> {
                    try {
                        logger.debug("🔄 Procesando detección con timestamp_ms: {}", d.getTimestamp_ms());
                        
                        // Procesar análisis simple sin Strategy pattern
                        processDetectionAnalysis(d);
                        
                        return Detection.builder()
                            .timestampMs(d.getTimestamp_ms())
                            .date(d.getDate())
                            .objectsTotal(safeWriteValueAsString(d.getObjects_total()))
                            .objectsByLane(safeWriteValueAsString(d.getObjects_by_lane()))
                            .avgSpeedByLane(safeWriteValueAsString(d.getAvg_speed_by_lane()))
                            .build();
                    } catch (Exception e) {
                        logger.error("❌ Error procesando detección con timestamp_ms {}: {}", 
                                   d.getTimestamp_ms(), e.getMessage());
                        return null;
                    }
                })
                .filter(d -> d != null)
                .collect(Collectors.toList());

            if (detections.isEmpty()) {
                logger.warn("⚠️ No se procesó ninguna detección válida.");
                return;
            }

            logger.info("💾 Guardando {} detecciones en la base de datos...", detections.size());
            detectionRepository.saveAll(detections);

            logger.info("🎉 {} detecciones cargadas exitosamente en la base de datos.", detections.size());
            
        } catch (IllegalArgumentException e) {
            logger.error("❌ Archivo JSON inválido: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("❌ Error al cargar el archivo JSON: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void loadJsonAndSaveToDb(String customFilePath) throws Exception {
        String originalPath = this.filePath;
        this.filePath = customFilePath;
        try {
            loadJsonAndSaveToDb();
        } finally {
            this.filePath = originalPath;
        }
    }

    /**
     * Procesa el análisis de detección de manera simple
     */
    private void processDetectionAnalysis(DetectionJson detection) {
        try {
            // Análisis básico de vehículos
            if (detection.getObjects_total() != null && !detection.getObjects_total().isEmpty()) {
                int totalVehicles = detection.getObjects_total().values().stream()
                    .mapToInt(Integer::intValue).sum();
                logger.debug("📊 Total de vehículos detectados: {}", totalVehicles);
            }
            
            // Análisis de velocidad
            if (detection.getAvg_speed_by_lane() != null && !detection.getAvg_speed_by_lane().isEmpty()) {
                double avgSpeed = detection.getAvg_speed_by_lane().values().stream()
                    .mapToDouble(Double::doubleValue).average().orElse(0.0);
                logger.debug("🚗 Velocidad promedio: {:.2f} km/h", avgSpeed);
            }
            
        } catch (Exception e) {
            logger.warn("⚠️ Error en análisis de detección: {}", e.getMessage());
        }
    }

    private String safeWriteValueAsString(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            logger.error("❌ Error al convertir objeto a JSON: {}", e.getMessage());
            return "{}";
        }
    }
}