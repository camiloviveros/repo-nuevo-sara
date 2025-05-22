package com.example.demo.service;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Detection;
import com.example.demo.repository.DetectionRepository;
import com.example.demo.service.DTO.DetectionJson;
import com.example.demo.service.DTO.DetectionsWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JsonLoader {

    private static final Logger logger = LoggerFactory.getLogger(JsonLoader.class);

    private final DetectionRepository detectionRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.detections.file-path:../detections/detections.json}")
    private String filePath;

    @Transactional
    public void loadJsonAndSaveToDb() throws Exception {
        loadJsonAndSaveToDb(filePath);
    }

    @Transactional
    public void loadJsonAndSaveToDb(String customFilePath) throws Exception {
        File jsonFile = new File(customFilePath);
        if (!jsonFile.exists()) {
            logger.error("❌ El archivo JSON no existe: {}", customFilePath);
            throw new IllegalArgumentException("El archivo JSON no existe: " + customFilePath);
        }
        
        try {
            logger.info("📁 Leyendo el archivo JSON desde: {}", customFilePath);
            logger.info("📏 Tamaño del archivo: {} bytes", jsonFile.length());

            DetectionsWrapper wrapper = objectMapper.readValue(jsonFile, DetectionsWrapper.class);
            logger.info("✅ Archivo JSON cargado exitosamente.");

            List<DetectionJson> detectionsJson = wrapper.getDetections();
            if (detectionsJson == null || detectionsJson.isEmpty()) {
                logger.warn("⚠️ No se encontraron detecciones en el archivo JSON.");
                return;
            }

            logger.info("📊 Se encontraron {} detecciones en el JSON", detectionsJson.size());

            // Verificar si hay datos existentes
            long existingCount = detectionRepository.count();
            logger.info("📊 Registros existentes en BD: {}", existingCount);

            // Limpiar datos existentes para evitar duplicados
            if (existingCount > 0) {
                logger.info("🧹 Limpiando {} registros existentes...", existingCount);
                detectionRepository.deleteAll();
                logger.info("✅ Base de datos limpiada");
            }

            // Procesar las detecciones
            List<Detection> detections = detectionsJson.stream()
                .filter(d -> d.getTimestamp_ms() != null)
                .map(this::convertToEntity)
                .filter(d -> d != null)
                .collect(Collectors.toList());

            if (detections.isEmpty()) {
                logger.warn("⚠️ No se procesó ninguna detección válida.");
                return;
            }

            logger.info("💾 Guardando {} detecciones válidas en la base de datos...", detections.size());
            
            // Guardar en lotes para mejor rendimiento
            saveInBatches(detections);

            // Verificar que se guardaron correctamente
            long finalCount = detectionRepository.count();
            logger.info("🎉 Proceso completado:");
            logger.info("   📥 Detecciones en JSON: {}", detectionsJson.size());
            logger.info("   ✅ Detecciones procesadas: {}", detections.size());
            logger.info("   💾 Registros en BD: {}", finalCount);
            
        } catch (Exception e) {
            logger.error("❌ Error al cargar el archivo JSON: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Método SEGURO que no lanza excepciones que puedan parar el servidor
     */
    public void loadJsonAndSaveToDbSafely(String customFilePath) {
        try {
            loadJsonAndSaveToDb(customFilePath);
        } catch (Exception e) {
            logger.warn("⚠️ Error cargando JSON (no crítico): {}", e.getMessage());
        }
    }

    private Detection convertToEntity(DetectionJson detectionJson) {
        try {
            logger.debug("🔄 Procesando detección con timestamp_ms: {}", detectionJson.getTimestamp_ms());
            
            return Detection.builder()
                .timestampMs(detectionJson.getTimestamp_ms())
                .date(detectionJson.getDate() != null ? detectionJson.getDate() : "")
                .objectsTotal(safeWriteValueAsString(detectionJson.getObjects_total()))
                .objectsByLane(safeWriteValueAsString(detectionJson.getObjects_by_lane()))
                .avgSpeedByLane(safeWriteValueAsString(detectionJson.getAvg_speed_by_lane()))
                .build();
                
        } catch (Exception e) {
            logger.error("❌ Error procesando detección con timestamp_ms {}: {}", 
                       detectionJson.getTimestamp_ms(), e.getMessage());
            return null;
        }
    }

    private void saveInBatches(List<Detection> detections) {
        int batchSize = 20;
        int processedCount = 0;
        
        for (int i = 0; i < detections.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, detections.size());
            List<Detection> batch = detections.subList(i, endIndex);
            
            try {
                detectionRepository.saveAll(batch);
                processedCount += batch.size();
                logger.debug("💾 Lote guardado: {} - {} ({} registros)", i + 1, endIndex, batch.size());
            } catch (Exception e) {
                logger.error("❌ Error guardando lote {}-{}: {}", i + 1, endIndex, e.getMessage());
            }
        }
        
        logger.info("✅ Procesadas {} detecciones", processedCount);
    }

    private String safeWriteValueAsString(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            String jsonString = objectMapper.writeValueAsString(value);
            if (jsonString == null || jsonString.trim().isEmpty() || jsonString.equals("null")) {
                return "{}";
            }
            return jsonString;
        } catch (JsonProcessingException e) {
            logger.warn("⚠️ Error convirtiendo objeto a JSON: {}", e.getMessage());
            return "{}";
        }
    }

    public void verifyDataIntegrity() {
        try {
            logger.info("🔍 Verificando integridad de datos...");
            
            long totalCount = detectionRepository.count();
            logger.info("📊 Total de registros: {}", totalCount);
            
            if (totalCount == 0) {
                logger.warn("⚠️ No hay registros en la base de datos");
                return;
            }
            
            logger.info("✅ Verificación de integridad completada");
            
        } catch (Exception e) {
            logger.error("❌ Error verificando integridad de datos: {}", e.getMessage());
        }
    }

    @Transactional
    public void clearDatabase() {
        try {
            long countBefore = detectionRepository.count();
            logger.info("🧹 Limpiando base de datos... ({} registros)", countBefore);
            
            detectionRepository.deleteAll();
            
            long countAfter = detectionRepository.count();
            logger.info("✅ Base de datos limpiada. Registros restantes: {}", countAfter);
            
        } catch (Exception e) {
            logger.error("❌ Error limpiando base de datos: {}", e.getMessage());
            throw new RuntimeException("Error limpiando base de datos", e);
        }
    }

    @Transactional
    public void forceReload() throws Exception {
        logger.info("🔄 Iniciando recarga forzada de datos...");
        
        clearDatabase();
        loadJsonAndSaveToDb();
        verifyDataIntegrity();
        
        logger.info("✅ Recarga forzada completada");
    }
}