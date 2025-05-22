package com.example.demo.service;

import java.io.File;
import java.io.IOException;
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

    @Value("${app.detections.file-path}")
    private String filePath;

    @Transactional
    public void loadJsonAndSaveToDb() throws Exception {
        File jsonFile = new File(filePath);
        if (!jsonFile.exists()) {
            logger.error("❌ El archivo JSON no existe: {}", filePath);
            throw new IllegalArgumentException("El archivo JSON no existe: " + filePath);
        }
        
        try {
            logger.info("📁 Leyendo el archivo JSON desde: {}", filePath);
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
            long existingCount = detectionRepository.countAllDetections();
            logger.info("📊 Registros existentes en BD: {}", existingCount);

            // Limpiar datos existentes para evitar duplicados
            if (existingCount > 0) {
                logger.info("🧹 Limpiando {} registros existentes...", existingCount);
                detectionRepository.deleteAll();
                logger.info("✅ Base de datos limpiada");
            }

            // Procesar las detecciones
            int processedCount = 0;
            int errorCount = 0;
            
            List<Detection> detections = detectionsJson.stream()
                .map(d -> {
                    try {
                        logger.debug("🔄 Procesando detección con timestamp_ms: {}", d.getTimestamp_ms());
                        
                        // Validar datos básicos
                        if (d.getTimestamp_ms() == null) {
                            logger.warn("⚠️ Detección sin timestamp_ms, omitiendo");
                            return null;
                        }
                        
                        // Procesar análisis simple
                        processDetectionAnalysis(d);
                        
                        // Crear entidad Detection
                        Detection detection = Detection.builder()
                            .timestampMs(d.getTimestamp_ms())
                            .date(d.getDate() != null ? d.getDate() : "")
                            .objectsTotal(safeWriteValueAsString(d.getObjects_total()))
                            .objectsByLane(safeWriteValueAsString(d.getObjects_by_lane()))
                            .avgSpeedByLane(safeWriteValueAsString(d.getAvg_speed_by_lane()))
                            .build();
                            
                        logger.debug("✅ Detección procesada: ID temporal, timestamp: {}, objetos: {}", 
                                   d.getTimestamp_ms(), 
                                   d.getObjects_total() != null ? d.getObjects_total().size() : 0);
                        
                        return detection;
                    } catch (RuntimeException e) {
                        String errorMessage = e.getMessage();
                        String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
                        logger.error("❌ Error de ejecución procesando detección con timestamp_ms {}: {}", 
                                   d.getTimestamp_ms(), safeMessage);
                        return null;
                    } catch (Exception e) {
                        String errorMessage = e.getMessage();
                        String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
                        logger.error("❌ Error general procesando detección con timestamp_ms {}: {}", 
                                   d.getTimestamp_ms(), safeMessage);
                        return null;
                    }
                })
                .filter(d -> d != null)
                .collect(Collectors.toList());

            if (detections.isEmpty()) {
                logger.warn("⚠️ No se procesó ninguna detección válida.");
                return;
            }

            logger.info("💾 Guardando {} detecciones válidas en la base de datos...", detections.size());
            
            // Guardar en lotes para mejor rendimiento
            int batchSize = 20;
            for (int i = 0; i < detections.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, detections.size());
                List<Detection> batch = detections.subList(i, endIndex);
                
                try {
                    detectionRepository.saveAll(batch);
                    processedCount += batch.size();
                    logger.debug("💾 Lote guardado: {} - {} ({} registros)", i + 1, endIndex, batch.size());
                } catch (org.springframework.dao.DataAccessException e) {
                    String errorMessage = e.getMessage();
                    String safeMessage = errorMessage != null ? errorMessage : "Error de acceso a datos";
                    logger.error("❌ Error de BD guardando lote {}-{}: {}", i + 1, endIndex, safeMessage);
                    errorCount += batch.size();
                } catch (Exception e) {
                    String errorMessage = e.getMessage();
                    String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
                    logger.error("❌ Error guardando lote {}-{}: {}", i + 1, endIndex, safeMessage);
                    errorCount += batch.size();
                }
            }

            // Verificar que se guardaron correctamente
            long finalCount = detectionRepository.countAllDetections();
            logger.info("🎉 Proceso completado:");
            logger.info("   📥 Detecciones en JSON: {}", detectionsJson.size());
            logger.info("   ✅ Detecciones procesadas: {}", processedCount);
            logger.info("   ❌ Errores: {}", errorCount);
            logger.info("   💾 Registros en BD: {}", finalCount);
            
            if (finalCount != processedCount) {
                logger.warn("⚠️ Discrepancia: se procesaron {} pero hay {} en BD", processedCount, finalCount);
            }
            
        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Argumento inválido";
            logger.error("❌ Archivo JSON inválido: {}", safeMessage);
            throw e;
        } catch (JsonProcessingException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de procesamiento JSON";
            logger.error("❌ Error de JSON al cargar el archivo: {}", safeMessage, e);
            throw e;
        } catch (IOException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de entrada/salida";
            logger.error("❌ Error de E/O al cargar el archivo: {}", safeMessage, e);
            throw e;
        } catch (org.springframework.dao.DataAccessException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de acceso a datos";
            logger.error("❌ Error de BD al cargar el archivo: {}", safeMessage, e);
            throw e;
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general al cargar el archivo JSON: {}", safeMessage, e);
            throw e;
        }
    }

    @Transactional
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
                    .filter(count -> count != null && count > 0)
                    .mapToInt(Integer::intValue)
                    .sum();
                logger.debug("📊 Total de vehículos detectados: {}", totalVehicles);
                
                // Log por tipo de vehículo
                detection.getObjects_total().forEach((type, count) -> {
                    if (count != null && count > 0) {
                        logger.debug("🚗 Tipo {}: {} vehículos", type, count);
                    }
                });
            }
            
            // Análisis de velocidad
            if (detection.getAvg_speed_by_lane() != null && !detection.getAvg_speed_by_lane().isEmpty()) {
                double avgSpeed = detection.getAvg_speed_by_lane().values().stream()
                    .filter(speed -> speed != null && speed > 0)
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
                logger.debug("🚗 Velocidad promedio: {:.2f} km/h", avgSpeed);
                
                // Log por carril
                detection.getAvg_speed_by_lane().forEach((lane, speed) -> {
                    if (speed != null && speed > 0) {
                        logger.debug("🛣️ {}: {:.2f} km/h", lane, speed);
                    }
                });
            }
            
            // Análisis de carriles
            if (detection.getObjects_by_lane() != null && !detection.getObjects_by_lane().isEmpty()) {
                int totalLanes = detection.getObjects_by_lane().size();
                logger.debug("🛣️ Datos de {} carriles disponibles", totalLanes);
            }
            
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.warn("⚠️ Error de ejecución en análisis de detección con timestamp {}: {}", 
                       detection.getTimestamp_ms(), safeMessage);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.warn("⚠️ Error general en análisis de detección con timestamp {}: {}", 
                       detection.getTimestamp_ms(), safeMessage);
        }
    }

    /**
     * Convierte un objeto a JSON de manera segura
     */
    private String safeWriteValueAsString(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            String jsonString = objectMapper.writeValueAsString(value);
            // Verificar que no sea una cadena vacía después de la conversión
            if (jsonString == null || jsonString.trim().isEmpty() || jsonString.equals("null")) {
                return "{}";
            }
            logger.debug("📝 JSON generado: {}", jsonString.length() > 100 ? 
                        jsonString.substring(0, 100) + "..." : jsonString);
            return jsonString;
        } catch (JsonProcessingException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de procesamiento JSON";
            logger.error("❌ Error de JSON al convertir objeto: {} - Objeto: {}", safeMessage, value);
            return "{}";
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general al convertir objeto a JSON: {} - Objeto: {}", safeMessage, value);
            return "{}";
        }
    }

    /**
     * Método para verificar la integridad de los datos cargados
     */
    public void verifyDataIntegrity() {
        try {
            logger.info("🔍 Verificando integridad de datos...");
            
            long totalCount = detectionRepository.countAllDetections();
            logger.info("📊 Total de registros: {}", totalCount);
            
            if (totalCount == 0) {
                logger.warn("⚠️ No hay registros en la base de datos");
                return;
            }
            
            // Verificar que existen datos con contenido
            List<Detection> withObjectData = detectionRepository.findDetectionsWithObjectData();
            List<Detection> withSpeedData = detectionRepository.findDetectionsWithSpeedData();
            
            logger.info("📊 Registros con datos de objetos: {}", withObjectData.size());
            logger.info("📊 Registros con datos de velocidad: {}", withSpeedData.size());
            
            // Obtener una muestra para verificar
            Detection mostRecent = detectionRepository.findMostRecentDetection();
            if (mostRecent != null) {
                logger.info("📊 Registro más reciente:");
                logger.info("   ID: {}", mostRecent.getId());
                logger.info("   Timestamp: {}", mostRecent.getTimestampMs());
                logger.info("   Fecha: {}", mostRecent.getDate());
                logger.info("   Objetos totales: {}", 
                           mostRecent.getObjectsTotal() != null && !mostRecent.getObjectsTotal().equals("{}") 
                           ? "Disponible" : "Vacío");
                logger.info("   Objetos por carril: {}", 
                           mostRecent.getObjectsByLane() != null && !mostRecent.getObjectsByLane().equals("{}") 
                           ? "Disponible" : "Vacío");
                logger.info("   Velocidades: {}", 
                           mostRecent.getAvgSpeedByLane() != null && !mostRecent.getAvgSpeedByLane().equals("{}") 
                           ? "Disponible" : "Vacío");
            }
            
            logger.info("✅ Verificación de integridad completada");
            
        } catch (org.springframework.dao.DataAccessException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de acceso a datos";
            logger.error("❌ Error de BD verificando integridad de datos: {}", safeMessage, e);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general verificando integridad de datos: {}", safeMessage, e);
        }
    }

    /**
     * Método para limpiar la base de datos manualmente
     */
    @Transactional
    public void clearDatabase() {
        try {
            long countBefore = detectionRepository.countAllDetections();
            logger.info("🧹 Limpiando base de datos... ({} registros)", countBefore);
            
            detectionRepository.deleteAll();
            
            long countAfter = detectionRepository.countAllDetections();
            logger.info("✅ Base de datos limpiada. Registros restantes: {}", countAfter);
            
        } catch (org.springframework.dao.DataAccessException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de acceso a datos";
            logger.error("❌ Error de BD limpiando base de datos: {}", safeMessage, e);
            throw new RuntimeException("Error limpiando base de datos", e);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general limpiando base de datos: {}", safeMessage, e);
            throw new RuntimeException("Error limpiando base de datos", e);
        }
    }

    /**
     * Método para recargar datos forzando la actualización
     */
    @Transactional
    public void forceReload() throws Exception {
        logger.info("🔄 Iniciando recarga forzada de datos...");
        
        // Limpiar primero
        clearDatabase();
        
        // Cargar nuevamente
        loadJsonAndSaveToDb();
        
        // Verificar integridad
        verifyDataIntegrity();
        
        logger.info("✅ Recarga forzada completada");
    }
}