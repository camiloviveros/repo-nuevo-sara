package com.example.demo.controller;

import com.example.demo.service.DetectionAnalysisService;
import com.example.demo.service.JsonLoader;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/detections")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001"})
@RequiredArgsConstructor
public class DetectionController {

    private static final Logger logger = LoggerFactory.getLogger(DetectionController.class);
    private final DetectionAnalysisService analysisService;
    private final JsonLoader jsonLoader;

    // Endpoint para obtener el volumen total de vehículos
    @GetMapping("/volume/total")
    public ResponseEntity<Map<String, Object>> getTotalVehicleVolume() {
        try {
            logger.info("📊 Solicitando volumen total de vehículos");
            Map<String, Object> result = analysisService.getTotalVehicleVolume();
            logger.debug("✅ Volumen total obtenido: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo volumen total: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para obtener volumen por carril
    @GetMapping("/volume/by-lane")
    public ResponseEntity<Map<String, Map<String, Integer>>> getVehicleVolumeByLane() {
        try {
            logger.info("🛣️ Solicitando volumen por carril");
            Map<String, Map<String, Integer>> result = analysisService.getVehicleVolumeByLane();
            logger.debug("✅ Volumen por carril obtenido: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo volumen por carril: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para obtener patrones horarios
    @GetMapping("/patterns/hourly")
    public ResponseEntity<Map<String, Integer>> getHourlyPatterns() {
        try {
            logger.info("⏰ Solicitando patrones horarios");
            Map<String, Integer> result = analysisService.getHourlyPatterns();
            logger.debug("✅ Patrones horarios obtenidos: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo patrones horarios: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para obtener velocidad promedio por carril
    @GetMapping("/lanes/speed")
    public ResponseEntity<Map<String, Double>> getAvgSpeedByLane() {
        try {
            logger.info("🚗 Solicitando velocidad promedio por carril");
            Map<String, Double> result = analysisService.getAvgSpeedByLane();
            logger.debug("✅ Velocidades por carril obtenidas: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo velocidades por carril: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para obtener cuellos de botella
    @GetMapping("/lanes/bottlenecks")
    public ResponseEntity<Object[]> getBottlenecks() {
        try {
            logger.info("🚧 Solicitando cuellos de botella");
            Object[] result = analysisService.getBottlenecks();
            logger.debug("✅ Cuellos de botella obtenidos: {} elementos", result.length);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo cuellos de botella: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para evolución temporal del tráfico
    @GetMapping("/temporal/evolution")
    public ResponseEntity<Map<String, Object>> getTrafficEvolution() {
        try {
            logger.info("📈 Solicitando evolución temporal del tráfico");
            Map<String, Object> result = analysisService.getTrafficEvolution();
            logger.debug("✅ Evolución temporal obtenida");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo evolución temporal: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para evolución de velocidad
    @GetMapping("/temporal/speed")
    public ResponseEntity<Map<String, Object>> getSpeedEvolution() {
        try {
            logger.info("🏎️ Solicitando evolución de velocidad");
            Map<String, Object> result = analysisService.getSpeedEvolution();
            logger.debug("✅ Evolución de velocidad obtenida");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo evolución de velocidad: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para dominancia de tipos de vehículos
    @GetMapping("/vehicle-types/dominance")
    public ResponseEntity<Map<String, Double>> getVehicleTypeDominance() {
        try {
            logger.info("🚙 Solicitando dominancia de tipos de vehículos");
            Map<String, Double> result = analysisService.getVehicleTypeDominance();
            logger.debug("✅ Dominancia de tipos obtenida: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo dominancia de tipos: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoints para estructuras de datos
    @GetMapping("/structures/array")
    public ResponseEntity<int[]> getArrayData() {
        try {
            logger.info("📊 Solicitando datos de array");
            int[] result = analysisService.getArrayData();
            logger.debug("✅ Datos de array obtenidos: {} elementos", result.length);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo datos de array: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/structures/linked-list")
    public ResponseEntity<Object[]> getLinkedListData() {
        try {
            logger.info("🔗 Solicitando datos de lista enlazada");
            Object[] result = analysisService.getLinkedListData();
            logger.debug("✅ Datos de lista enlazada obtenidos: {} elementos", result.length);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo datos de lista enlazada: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/structures/double-linked-list")
    public ResponseEntity<Object[]> getDoubleLinkedListData() {
        try {
            logger.info("🔗🔗 Solicitando datos de lista doblemente enlazada");
            Object[] result = analysisService.getDoubleLinkedListData();
            logger.debug("✅ Datos de lista doblemente enlazada obtenidos: {} elementos", result.length);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo datos de lista doblemente enlazada: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/structures/circular-double-linked-list")
    public ResponseEntity<Object[]> getCircularDoubleLinkedListData() {
        try {
            logger.info("⭕ Solicitando datos de lista circular doblemente enlazada");
            Object[] result = analysisService.getCircularDoubleLinkedListData();
            logger.debug("✅ Datos de lista circular obtenidos: {} elementos", result.length);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo datos de lista circular: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/structures/stack")
    public ResponseEntity<Object[]> getStackData() {
        try {
            logger.info("📚 Solicitando datos de pila");
            Object[] result = analysisService.getStackData();
            logger.debug("✅ Datos de pila obtenidos: {} elementos", result.length);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo datos de pila: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/structures/queue")
    public ResponseEntity<Object[]> getQueueData() {
        try {
            logger.info("📋 Solicitando datos de cola");
            Object[] result = analysisService.getQueueData();
            logger.debug("✅ Datos de cola obtenidos: {} elementos", result.length);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo datos de cola: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/structures/tree")
    public ResponseEntity<Map<String, Object>> getTreeData() {
        try {
            logger.info("🌳 Solicitando datos de árbol");
            Map<String, Object> result = analysisService.getTreeData();
            logger.debug("✅ Datos de árbol obtenidos");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo datos de árbol: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para cargar manualmente el JSON
    @PostMapping("/load-json")
    public ResponseEntity<Map<String, Object>> loadJsonManually() {
        try {
            logger.info("📂 Cargando JSON manualmente");
            jsonLoader.loadJsonAndSaveToDb();
            
            Map<String, Object> response = Map.of(
                "status", "success",
                "message", "JSON cargado exitosamente",
                "timestamp", System.currentTimeMillis()
            );
            
            logger.info("✅ JSON cargado manualmente con éxito");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error cargando JSON manualmente: {}", e.getMessage());
            
            Map<String, Object> errorResponse = Map.of(
                "status", "error",
                "message", "Error cargando JSON: " + e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // Endpoint para obtener estadísticas generales
    @GetMapping("/analysis/summary")
    public ResponseEntity<Map<String, Object>> getAnalysisSummary() {
        try {
            logger.info("📋 Solicitando resumen de análisis");
            Map<String, Object> result = analysisService.getAnalysisSummary();
            logger.debug("✅ Resumen de análisis obtenido: {}", result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo resumen de análisis: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Endpoint para verificar el estado del servidor
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        try {
            long totalDetections = analysisService.getTotalDetections();
            
            Map<String, Object> health = Map.of(
                "status", "UP",
                "timestamp", System.currentTimeMillis(),
                "totalDetections", totalDetections,
                "service", "Detection Analysis API",
                "version", "1.0.0"
            );
            
            logger.debug("💚 Estado de salud verificado: {} detecciones en BD", totalDetections);
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("❌ Error verificando estado de salud: {}", e.getMessage());
            
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "timestamp", System.currentTimeMillis(),
                "error", e.getMessage()
            );
            
            return ResponseEntity.internalServerError().body(health);
        }
    }

    // Endpoint para obtener estadísticas rápidas
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQuickStats() {
        try {
            logger.info("📊 Solicitando estadísticas rápidas");
            
            Map<String, Object> totalVolume = analysisService.getTotalVehicleVolume();
            Map<String, Double> avgSpeeds = analysisService.getAvgSpeedByLane();
            long totalDetections = analysisService.getTotalDetections();
            
            @SuppressWarnings("unchecked")
            Map<String, Integer> totals = (Map<String, Integer>) totalVolume.get("total");
            int totalVehicles = totals.values().stream().mapToInt(Integer::intValue).sum();
            
            Map<String, Object> stats = Map.of(
                "totalDetections", totalDetections,
                "totalVehicles", totalVehicles,
                "avgSpeedOverall", avgSpeeds.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0),
                "activeLines", avgSpeeds.size(),
                "lastUpdated", System.currentTimeMillis()
            );
            
            logger.debug("✅ Estadísticas rápidas obtenidas: {}", stats);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("❌ Error obteniendo estadísticas rápidas: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}