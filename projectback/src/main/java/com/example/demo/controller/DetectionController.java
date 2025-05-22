package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.DetectionAnalysisService;
import com.example.demo.service.JsonLoader;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/detections")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001"})
@RequiredArgsConstructor
public class DetectionController {

    private static final Logger logger = LoggerFactory.getLogger(DetectionController.class);
    private final DetectionAnalysisService analysisService;
    private final JsonLoader jsonLoader;

    // Endpoint básico de prueba
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        logger.info("🔧 Endpoint de prueba ejecutado");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "API funcionando correctamente");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    // Endpoint para obtener el volumen total de vehículos
    @GetMapping("/volume/total")
    public ResponseEntity<Map<String, Object>> getTotalVehicleVolume() {
        logger.info("📊 Solicitando volumen total de vehículos");
        try {
            Map<String, Object> result = analysisService.getTotalVehicleVolume();
            logger.info("✅ Volumen total obtenido exitosamente: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo volumen total: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error interno del servidor");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Endpoint para obtener volumen por carril
    @GetMapping("/volume/by-lane")
    public ResponseEntity<Map<String, Map<String, Integer>>> getVehicleVolumeByLane() {
        logger.info("🛣️ Solicitando volumen por carril");
        try {
            Map<String, Map<String, Integer>> result = analysisService.getVehicleVolumeByLane();
            logger.info("✅ Volumen por carril obtenido exitosamente");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo volumen por carril: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para obtener patrones horarios
    @GetMapping("/patterns/hourly")
    public ResponseEntity<Map<String, Integer>> getHourlyPatterns() {
        logger.info("⏰ Solicitando patrones horarios");
        try {
            Map<String, Integer> result = analysisService.getHourlyPatterns();
            logger.info("✅ Patrones horarios obtenidos exitosamente");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo patrones horarios: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para obtener velocidad promedio por carril
    @GetMapping("/lanes/speed")
    public ResponseEntity<Map<String, Double>> getAvgSpeedByLane() {
        logger.info("🚗 Solicitando velocidad promedio por carril");
        try {
            Map<String, Double> result = analysisService.getAvgSpeedByLane();
            logger.info("✅ Velocidades por carril obtenidas exitosamente");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo velocidades por carril: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para obtener cuellos de botella
    @GetMapping("/lanes/bottlenecks")
    public ResponseEntity<Object[]> getBottlenecks() {
        logger.info("🚧 Solicitando cuellos de botella");
        try {
            Object[] result = analysisService.getBottlenecks();
            logger.info("✅ Cuellos de botella obtenidos exitosamente: {} elementos", result.length);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo cuellos de botella: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para evolución temporal del tráfico
    @GetMapping("/temporal/evolution")
    public ResponseEntity<Map<String, Object>> getTrafficEvolution() {
        logger.info("📈 Solicitando evolución temporal del tráfico");
        try {
            Map<String, Object> result = analysisService.getTrafficEvolution();
            logger.info("✅ Evolución temporal obtenida exitosamente");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo evolución temporal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para evolución de velocidad
    @GetMapping("/temporal/speed")
    public ResponseEntity<Map<String, Object>> getSpeedEvolution() {
        logger.info("🏎️ Solicitando evolución de velocidad");
        try {
            Map<String, Object> result = analysisService.getSpeedEvolution();
            logger.info("✅ Evolución de velocidad obtenida exitosamente");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo evolución de velocidad: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para dominancia de tipos de vehículos
    @GetMapping("/vehicle-types/dominance")
    public ResponseEntity<Map<String, Double>> getVehicleTypeDominance() {
        logger.info("🚙 Solicitando dominancia de tipos de vehículos");
        try {
            Map<String, Double> result = analysisService.getVehicleTypeDominance();
            logger.info("✅ Dominancia de tipos obtenida exitosamente");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo dominancia de tipos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoints para estructuras de datos
    @GetMapping("/structures/array")
    public ResponseEntity<int[]> getArrayData() {
        logger.info("📊 Solicitando datos de array");
        try {
            int[] result = analysisService.getArrayData();
            logger.info("✅ Datos de array obtenidos exitosamente: {} elementos", result.length);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo datos de array: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/structures/linked-list")
    public ResponseEntity<Object[]> getLinkedListData() {
        logger.info("🔗 Solicitando datos de lista enlazada");
        try {
            Object[] result = analysisService.getLinkedListData();
            logger.info("✅ Datos de lista enlazada obtenidos exitosamente: {} elementos", result.length);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo datos de lista enlazada: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/structures/double-linked-list")
    public ResponseEntity<Object[]> getDoubleLinkedListData() {
        logger.info("🔗🔗 Solicitando datos de lista doblemente enlazada");
        try {
            Object[] result = analysisService.getDoubleLinkedListData();
            logger.info("✅ Datos de lista doblemente enlazada obtenidos exitosamente: {} elementos", result.length);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo datos de lista doblemente enlazada: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/structures/circular-double-linked-list")
    public ResponseEntity<Object[]> getCircularDoubleLinkedListData() {
        logger.info("⭕ Solicitando datos de lista circular doblemente enlazada");
        try {
            Object[] result = analysisService.getCircularDoubleLinkedListData();
            logger.info("✅ Datos de lista circular obtenidos exitosamente: {} elementos", result.length);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo datos de lista circular: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/structures/stack")
    public ResponseEntity<Object[]> getStackData() {
        logger.info("📚 Solicitando datos de pila");
        try {
            Object[] result = analysisService.getStackData();
            logger.info("✅ Datos de pila obtenidos exitosamente: {} elementos", result.length);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo datos de pila: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/structures/queue")
    public ResponseEntity<Object[]> getQueueData() {
        logger.info("📋 Solicitando datos de cola");
        try {
            Object[] result = analysisService.getQueueData();
            logger.info("✅ Datos de cola obtenidos exitosamente: {} elementos", result.length);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo datos de cola: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/structures/tree")
    public ResponseEntity<Map<String, Object>> getTreeData() {
        logger.info("🌳 Solicitando datos de árbol");
        try {
            Map<String, Object> result = analysisService.getTreeData();
            logger.info("✅ Datos de árbol obtenidos exitosamente");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo datos de árbol: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para cargar manualmente el JSON
    @PostMapping("/load-json")
    public ResponseEntity<Map<String, Object>> loadJsonManually() {
        logger.info("📂 Cargando JSON manualmente");
        try {
            jsonLoader.loadJsonAndSaveToDb();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "JSON cargado exitosamente");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("✅ JSON cargado manualmente con éxito");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error cargando JSON manualmente: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error cargando JSON: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Endpoint para obtener estadísticas generales
    @GetMapping("/analysis/summary")
    public ResponseEntity<Map<String, Object>> getAnalysisSummary() {
        logger.info("📋 Solicitando resumen de análisis");
        try {
            Map<String, Object> result = analysisService.getAnalysisSummary();
            logger.info("✅ Resumen de análisis obtenido exitosamente");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo resumen de análisis: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para verificar el estado del servidor
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        logger.info("💚 Verificando estado de salud");
        try {
            long totalDetections = analysisService.getTotalDetections();
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());
            health.put("totalDetections", totalDetections);
            health.put("service", "Detection Analysis API");
            health.put("version", "1.0.0");
            health.put("database", "Connected");
            
            logger.info("💚 Estado de salud verificado: {} detecciones en BD", totalDetections);
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("❌ Error verificando estado de salud: {}", e.getMessage(), e);
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("timestamp", System.currentTimeMillis());
            health.put("error", e.getMessage());
            health.put("database", "Error");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(health);
        }
    }

    // Endpoint para obtener estadísticas rápidas
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQuickStats() {
        logger.info("📊 Solicitando estadísticas rápidas");
        try {
            Map<String, Object> totalVolume = analysisService.getTotalVehicleVolume();
            Map<String, Double> avgSpeeds = analysisService.getAvgSpeedByLane();
            long totalDetections = analysisService.getTotalDetections();
            
            @SuppressWarnings("unchecked")
            Map<String, Integer> totals = (Map<String, Integer>) totalVolume.get("total");
            int totalVehicles = totals != null ? totals.values().stream().mapToInt(Integer::intValue).sum() : 0;
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalDetections", totalDetections);
            stats.put("totalVehicles", totalVehicles);
            stats.put("avgSpeedOverall", avgSpeeds.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
            stats.put("activeLines", avgSpeeds.size());
            stats.put("lastUpdated", System.currentTimeMillis());
            
            logger.info("✅ Estadísticas rápidas obtenidas exitosamente");
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo estadísticas rápidas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint para obtener conteo total de registros en BD
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getDetectionCount() {
        logger.info("🔢 Solicitando conteo de detecciones");
        try {
            long count = analysisService.getTotalDetections();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalDetections", count);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("✅ Conteo obtenido: {} detecciones", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo conteo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

//findetection