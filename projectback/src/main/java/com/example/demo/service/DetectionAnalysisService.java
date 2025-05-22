package com.example.demo.service;

import com.example.demo.entity.Detection;
import com.example.demo.repository.DetectionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DetectionAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(DetectionAnalysisService.class);
    private final DetectionRepository detectionRepository;
    private final ObjectMapper objectMapper;

    public Map<String, Object> getTotalVehicleVolume() {
        logger.debug("🔍 Iniciando consulta de volumen total de vehículos");
        
        try {
            List<Detection> detections = detectionRepository.findAllOrderByTimestamp();
            logger.debug("📊 Se obtuvieron {} detecciones de la base de datos", detections.size());
            
            if (detections.isEmpty()) {
                logger.warn("⚠️ No se encontraron detecciones en la base de datos");
                return getDefaultTotalVolumeData();
            }
            
            Map<String, Integer> totalCounts = new HashMap<>();
            Map<String, Integer> hourlyCounts = new HashMap<>();
            Map<String, Integer> dailyCounts = new HashMap<>();
            
            for (Detection detection : detections) {
                try {
                    if (detection.getObjectsTotal() != null && !detection.getObjectsTotal().trim().isEmpty() && !detection.getObjectsTotal().equals("{}")) {
                        logger.debug("📝 Procesando detección ID: {} con datos: {}", detection.getId(), detection.getObjectsTotal());
                        
                        Map<String, Integer> objects = objectMapper.readValue(
                            detection.getObjectsTotal(), 
                            new TypeReference<Map<String, Integer>>() {}
                        );
                        
                        if (objects != null && !objects.isEmpty()) {
                            // Sumar totales
                            objects.forEach((key, value) -> {
                                if (value != null && value > 0) {
                                    totalCounts.merge(key, value, Integer::sum);
                                    logger.debug("🚗 Sumando {} {}: total ahora = {}", value, key, totalCounts.get(key));
                                }
                            });
                            
                            // Análisis horario
                            String hour = extractHourFromDate(detection.getDate());
                            if (hour != null) {
                                int hourlyTotal = objects.values().stream()
                                    .filter(Objects::nonNull)
                                    .mapToInt(Integer::intValue)
                                    .sum();
                                if (hourlyTotal > 0) {
                                    hourlyCounts.merge(hour, hourlyTotal, Integer::sum);
                                }
                            }
                            
                            // Análisis diario (simplificado)
                            String dayType = getDayType();
                            int dailyTotal = objects.values().stream()
                                .filter(Objects::nonNull)
                                .mapToInt(Integer::intValue)
                                .sum();
                            if (dailyTotal > 0) {
                                dailyCounts.merge(dayType, dailyTotal, Integer::sum);
                            }
                        }
                    }
                } catch (JsonProcessingException e) {
                    String errorMessage = e.getMessage();
                    String safeMessage = errorMessage != null ? errorMessage : "Error de procesamiento JSON";
                    logger.error("❌ Error JSON procesando detección ID {}: {}", detection.getId(), safeMessage);
                } catch (RuntimeException e) {
                    String errorMessage = e.getMessage();
                    String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
                    logger.error("❌ Error de ejecución procesando detección ID {}: {}", detection.getId(), safeMessage);
                }
            }
            
            logger.info("✅ Totales calculados: {}", totalCounts);
            
            Map<String, Object> result = new HashMap<>();
            result.put("total", totalCounts.isEmpty() ? Map.of("car", 0, "bus", 0, "truck", 0) : totalCounts);
            result.put("hourly", hourlyCounts);
            result.put("daily", dailyCounts);
            
            return result;
            
        } catch (org.springframework.dao.DataAccessException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de acceso a datos";
            logger.error("❌ Error de BD en getTotalVehicleVolume: {}", safeMessage, e);
            return getDefaultTotalVolumeData();
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.error("❌ Error de ejecución en getTotalVehicleVolume: {}", safeMessage, e);
            return getDefaultTotalVolumeData();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general en getTotalVehicleVolume: {}", safeMessage, e);
            return getDefaultTotalVolumeData();
        }
    }

    public Map<String, Map<String, Integer>> getVehicleVolumeByLane() {
        logger.debug("🔍 Iniciando consulta de volumen por carril");
        
        try {
            List<Detection> detections = detectionRepository.findTop50ByOrderByTimestampMsDesc();
            logger.debug("🛣️ Se obtuvieron {} detecciones recientes de la base de datos", detections.size());
            
            if (detections.isEmpty()) {
                logger.warn("⚠️ No se encontraron detecciones recientes en la base de datos");
                return getDefaultLaneData();
            }
            
            Map<String, Map<String, Integer>> laneData = new HashMap<>();
            
            for (Detection detection : detections) {
                try {
                    if (detection.getObjectsByLane() != null && !detection.getObjectsByLane().trim().isEmpty() && !detection.getObjectsByLane().equals("{}")) {
                        logger.debug("📝 Procesando datos de carril para detección ID: {}", detection.getId());
                        
                        Map<String, Map<String, Integer>> lanes = objectMapper.readValue(
                            detection.getObjectsByLane(), 
                            new TypeReference<Map<String, Map<String, Integer>>>() {}
                        );
                        
                        if (lanes != null && !lanes.isEmpty()) {
                            lanes.forEach((lane, vehicles) -> {
                                if (vehicles != null && !vehicles.isEmpty()) {
                                    laneData.computeIfAbsent(lane, k -> new HashMap<>());
                                    vehicles.forEach((vehicleType, count) -> {
                                        if (count != null && count > 0) {
                                            laneData.get(lane).merge(vehicleType, count, Integer::sum);
                                            logger.debug("🚗 Carril {}: {} {} (total: {})", 
                                                lane, count, vehicleType, laneData.get(lane).get(vehicleType));
                                        }
                                    });
                                }
                            });
                        }
                    }
                } catch (JsonProcessingException e) {
                    String errorMessage = e.getMessage();
                    String safeMessage = errorMessage != null ? errorMessage : "Error de procesamiento JSON";
                    logger.error("❌ Error JSON procesando datos de carril para detección ID {}: {}", detection.getId(), safeMessage);
                } catch (RuntimeException e) {
                    String errorMessage = e.getMessage();
                    String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
                    logger.error("❌ Error de ejecución procesando datos de carril para detección ID {}: {}", detection.getId(), safeMessage);
                }
            }
            
            logger.info("✅ Datos de carril calculados: {}", laneData);
            return laneData.isEmpty() ? getDefaultLaneData() : laneData;
            
        } catch (org.springframework.dao.DataAccessException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de acceso a datos";
            logger.error("❌ Error de BD en getVehicleVolumeByLane: {}", safeMessage, e);
            return getDefaultLaneData();
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.error("❌ Error de ejecución en getVehicleVolumeByLane: {}", safeMessage, e);
            return getDefaultLaneData();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general en getVehicleVolumeByLane: {}", safeMessage, e);
            return getDefaultLaneData();
        }
    }

    public Map<String, Integer> getHourlyPatterns() {
        logger.debug("🔍 Iniciando consulta de patrones horarios");
        
        try {
            List<Detection> detections = detectionRepository.findAllOrderByTimestamp();
            logger.debug("⏰ Se obtuvieron {} detecciones para análisis horario", detections.size());
            
            if (detections.isEmpty()) {
                logger.warn("⚠️ No se encontraron detecciones para análisis horario");
                return getDefaultHourlyPattern();
            }
            
            Map<String, Integer> hourlyPattern = new HashMap<>();
            
            for (Detection detection : detections) {
                try {
                    String hour = extractHourFromDate(detection.getDate());
                    if (hour != null && detection.getObjectsTotal() != null && !detection.getObjectsTotal().trim().isEmpty() && !detection.getObjectsTotal().equals("{}")) {
                        Map<String, Integer> objects = objectMapper.readValue(
                            detection.getObjectsTotal(), 
                            new TypeReference<Map<String, Integer>>() {}
                        );
                        
                        if (objects != null && !objects.isEmpty()) {
                            int totalVehicles = objects.values().stream()
                                .filter(Objects::nonNull)
                                .mapToInt(Integer::intValue)
                                .sum();
                            if (totalVehicles > 0) {
                                hourlyPattern.merge(hour, totalVehicles, Integer::sum);
                                logger.debug("⏰ Hora {}: {} vehículos (total: {})", hour, totalVehicles, hourlyPattern.get(hour));
                            }
                        }
                    }
                } catch (JsonProcessingException e) {
                    String errorMessage = e.getMessage();
                    String safeMessage = errorMessage != null ? errorMessage : "Error de procesamiento JSON";
                    logger.error("❌ Error JSON procesando patrón horario para detección ID {}: {}", detection.getId(), safeMessage);
                } catch (RuntimeException e) {
                    String errorMessage = e.getMessage();
                    String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
                    logger.error("❌ Error de ejecución procesando patrón horario para detección ID {}: {}", detection.getId(), safeMessage);
                }
            }
            
            logger.info("✅ Patrones horarios calculados: {}", hourlyPattern);
            return hourlyPattern.isEmpty() ? getDefaultHourlyPattern() : hourlyPattern;
            
        } catch (org.springframework.dao.DataAccessException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de acceso a datos";
            logger.error("❌ Error de BD en getHourlyPatterns: {}", safeMessage, e);
            return getDefaultHourlyPattern();
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.error("❌ Error de ejecución en getHourlyPatterns: {}", safeMessage, e);
            return getDefaultHourlyPattern();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general en getHourlyPatterns: {}", safeMessage, e);
            return getDefaultHourlyPattern();
        }
    }

    public Map<String, Double> getAvgSpeedByLane() {
        logger.debug("🔍 Iniciando consulta de velocidades por carril");
        
        try {
            List<Detection> detections = detectionRepository.findTop50ByOrderByTimestampMsDesc();
            logger.debug("🏎️ Se obtuvieron {} detecciones para análisis de velocidad", detections.size());
            
            if (detections.isEmpty()) {
                logger.warn("⚠️ No se encontraron detecciones para análisis de velocidad");
                return getDefaultSpeedData();
            }
            
            Map<String, List<Double>> speedsByLane = new HashMap<>();
            
            for (Detection detection : detections) {
                try {
                    if (detection.getAvgSpeedByLane() != null && !detection.getAvgSpeedByLane().trim().isEmpty() && !detection.getAvgSpeedByLane().equals("{}")) {
                        logger.debug("📝 Procesando velocidades para detección ID: {}", detection.getId());
                        
                        Map<String, Double> speeds = objectMapper.readValue(
                            detection.getAvgSpeedByLane(), 
                            new TypeReference<Map<String, Double>>() {}
                        );
                        
                        if (speeds != null && !speeds.isEmpty()) {
                            speeds.forEach((lane, speed) -> {
                                if (speed != null && speed > 0) {
                                    speedsByLane.computeIfAbsent(lane, k -> new ArrayList<>()).add(speed);
                                    logger.debug("🏎️ Carril {}: velocidad {} km/h", lane, speed);
                                }
                            });
                        }
                    }
                } catch (JsonProcessingException e) {
                    String errorMessage = e.getMessage();
                    String safeMessage = errorMessage != null ? errorMessage : "Error de procesamiento JSON";
                    logger.error("❌ Error JSON procesando velocidades para detección ID {}: {}", detection.getId(), safeMessage);
                } catch (RuntimeException e) {
                    String errorMessage = e.getMessage();
                    String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
                    logger.error("❌ Error de ejecución procesando velocidades para detección ID {}: {}", detection.getId(), safeMessage);
                }
            }
            
            Map<String, Double> avgSpeeds = new HashMap<>();
            speedsByLane.forEach((lane, speeds) -> {
                if (!speeds.isEmpty()) {
                    double average = speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    avgSpeeds.put(lane, Math.round(average * 100.0) / 100.0); // Redondear a 2 decimales
                    logger.debug("🏎️ Carril {}: velocidad promedio {} km/h", lane, avgSpeeds.get(lane));
                }
            });
            
            logger.info("✅ Velocidades promedio calculadas: {}", avgSpeeds);
            return avgSpeeds.isEmpty() ? getDefaultSpeedData() : avgSpeeds;
            
        } catch (org.springframework.dao.DataAccessException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de acceso a datos";
            logger.error("❌ Error de BD en getAvgSpeedByLane: {}", safeMessage, e);
            return getDefaultSpeedData();
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.error("❌ Error de ejecución en getAvgSpeedByLane: {}", safeMessage, e);
            return getDefaultSpeedData();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general en getAvgSpeedByLane: {}", safeMessage, e);
            return getDefaultSpeedData();
        }
    }

    public Object[] getBottlenecks() {
        logger.debug("🔍 Iniciando identificación de cuellos de botella");
        
        try {
            Map<String, Double> avgSpeeds = getAvgSpeedByLane();
            Map<String, Map<String, Integer>> laneData = getVehicleVolumeByLane();
            
            List<Map<String, Object>> bottlenecks = new ArrayList<>();
            
            avgSpeeds.forEach((lane, avgSpeed) -> {
                if (avgSpeed < 15.0) { // Threshold para identificar cuello de botella
                    Map<String, Integer> vehicles = laneData.getOrDefault(lane, new HashMap<>());
                    int totalVehicles = vehicles.values().stream()
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .sum();
                    int heavyVehicles = vehicles.getOrDefault("truck", 0) + vehicles.getOrDefault("bus", 0);
                    
                    Map<String, Object> bottleneck = new HashMap<>();
                    bottleneck.put("lane", lane);
                    bottleneck.put("avgSpeed", Math.round(avgSpeed * 100.0) / 100.0);
                    bottleneck.put("totalVehicles", totalVehicles);
                    bottleneck.put("heavyVehicles", heavyVehicles);
                    
                    bottlenecks.add(bottleneck);
                    logger.debug("🚧 Cuello de botella identificado en {}: {}km/h, {} vehículos", lane, avgSpeed, totalVehicles);
                }
            });
            
            logger.info("✅ Identificados {} cuellos de botella", bottlenecks.size());
            return bottlenecks.toArray();
            
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.error("❌ Error de ejecución en getBottlenecks: {}", safeMessage, e);
            return new Object[0];
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general en getBottlenecks: {}", safeMessage, e);
            return new Object[0];
        }
    }

    public Map<String, Object> getTrafficEvolution() {
        logger.debug("🔍 Iniciando consulta de evolución del tráfico");
        
        try {
            List<Detection> detections = detectionRepository.findTop50ByOrderByTimestampMsDesc();
            logger.debug("📈 Se obtuvieron {} detecciones para evolución temporal", detections.size());
            
            if (detections.isEmpty()) {
                logger.warn("⚠️ No se encontraron detecciones para evolución temporal");
                return getDefaultTrafficEvolution();
            }
            
            // Ordenar por timestamp ascendente para mostrar evolución correcta
            detections.sort(Comparator.comparing(Detection::getTimestampMs));
            
            List<String> timestamps = new ArrayList<>();
            List<Integer> carCounts = new ArrayList<>();
            List<Integer> busCounts = new ArrayList<>();
            List<Integer> truckCounts = new ArrayList<>();
            
            for (Detection detection : detections) {
                try {
                    timestamps.add(detection.getDate() != null ? detection.getDate() : "N/A");
                    
                    if (detection.getObjectsTotal() != null && !detection.getObjectsTotal().trim().isEmpty() && !detection.getObjectsTotal().equals("{}")) {
                        Map<String, Integer> objects = objectMapper.readValue(
                            detection.getObjectsTotal(), 
                            new TypeReference<Map<String, Integer>>() {}
                        );
                        
                        if (objects != null) {
                            carCounts.add(objects.getOrDefault("car", 0));
                            busCounts.add(objects.getOrDefault("bus", 0));
                            truckCounts.add(objects.getOrDefault("truck", 0));
                        } else {
                            carCounts.add(0);
                            busCounts.add(0);
                            truckCounts.add(0);
                        }
                    } else {
                        carCounts.add(0);
                        busCounts.add(0);
                        truckCounts.add(0);
                    }
                } catch (JsonProcessingException e) {
                    String errorMessage = e.getMessage();
                    String safeMessage = errorMessage != null ? errorMessage : "Error de procesamiento JSON";
                    logger.error("❌ Error JSON procesando evolución para detección ID {}: {}", detection.getId(), safeMessage);
                    carCounts.add(0);
                    busCounts.add(0);
                    truckCounts.add(0);
                } catch (RuntimeException e) {
                    String errorMessage = e.getMessage();
                    String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
                    logger.error("❌ Error de ejecución procesando evolución para detección ID {}: {}", detection.getId(), safeMessage);
                    carCounts.add(0);
                    busCounts.add(0);
                    truckCounts.add(0);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("timestamps", timestamps);
            result.put("car", carCounts);
            result.put("bus", busCounts);
            result.put("truck", truckCounts);
            
            logger.info("✅ Evolución del tráfico calculada con {} puntos de datos", timestamps.size());
            return result;
            
        } catch (org.springframework.dao.DataAccessException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de acceso a datos";
            logger.error("❌ Error de BD en getTrafficEvolution: {}", safeMessage, e);
            return getDefaultTrafficEvolution();
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.error("❌ Error de ejecución en getTrafficEvolution: {}", safeMessage, e);
            return getDefaultTrafficEvolution();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general en getTrafficEvolution: {}", safeMessage, e);
            return getDefaultTrafficEvolution();
        }
    }

    public Map<String, Object> getSpeedEvolution() {
        logger.debug("🔍 Iniciando consulta de evolución de velocidad");
        
        try {
            List<Detection> detections = detectionRepository.findTop50ByOrderByTimestampMsDesc();
            logger.debug("🏎️ Se obtuvieron {} detecciones para evolución de velocidad", detections.size());
            
            if (detections.isEmpty()) {
                logger.warn("⚠️ No se encontraron detecciones para evolución de velocidad");
                return getDefaultSpeedEvolution();
            }
            
            // Ordenar por timestamp ascendente para mostrar evolución correcta
            detections.sort(Comparator.comparing(Detection::getTimestampMs));
            
            List<String> timestamps = new ArrayList<>();
            List<Double> lane1Speeds = new ArrayList<>();
            List<Double> lane2Speeds = new ArrayList<>();
            List<Double> lane3Speeds = new ArrayList<>();
            
            for (Detection detection : detections) {
                try {
                    timestamps.add(detection.getDate() != null ? detection.getDate() : "N/A");
                    
                    if (detection.getAvgSpeedByLane() != null && !detection.getAvgSpeedByLane().trim().isEmpty() && !detection.getAvgSpeedByLane().equals("{}")) {
                        Map<String, Double> speeds = objectMapper.readValue(
                            detection.getAvgSpeedByLane(), 
                            new TypeReference<Map<String, Double>>() {}
                        );
                        
                        if (speeds != null) {
                            lane1Speeds.add(speeds.getOrDefault("lane_1", 0.0));
                            lane2Speeds.add(speeds.getOrDefault("lane_2", 0.0));
                            lane3Speeds.add(speeds.getOrDefault("lane_3", 0.0));
                        } else {
                            lane1Speeds.add(0.0);
                            lane2Speeds.add(0.0);
                            lane3Speeds.add(0.0);
                        }
                    } else {
                        lane1Speeds.add(0.0);
                        lane2Speeds.add(0.0);
                        lane3Speeds.add(0.0);
                    }
                } catch (JsonProcessingException e) {
                    String errorMessage = e.getMessage();
                    String safeMessage = errorMessage != null ? errorMessage : "Error de procesamiento JSON";
                    logger.error("❌ Error JSON procesando evolución de velocidad para detección ID {}: {}", detection.getId(), safeMessage);
                    lane1Speeds.add(0.0);
                    lane2Speeds.add(0.0);
                    lane3Speeds.add(0.0);
                } catch (RuntimeException e) {
                    String errorMessage = e.getMessage();
                    String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
                    logger.error("❌ Error de ejecución procesando evolución de velocidad para detección ID {}: {}", detection.getId(), safeMessage);
                    lane1Speeds.add(0.0);
                    lane2Speeds.add(0.0);
                    lane3Speeds.add(0.0);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("timestamps", timestamps);
            result.put("lane_1", lane1Speeds);
            result.put("lane_2", lane2Speeds);
            result.put("lane_3", lane3Speeds);
            
            logger.info("✅ Evolución de velocidad calculada con {} puntos de datos", timestamps.size());
            return result;
            
        } catch (org.springframework.dao.DataAccessException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de acceso a datos";
            logger.error("❌ Error de BD en getSpeedEvolution: {}", safeMessage, e);
            return getDefaultSpeedEvolution();
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.error("❌ Error de ejecución en getSpeedEvolution: {}", safeMessage, e);
            return getDefaultSpeedEvolution();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general en getSpeedEvolution: {}", safeMessage, e);
            return getDefaultSpeedEvolution();
        }
    }

    public Map<String, Double> getVehicleTypeDominance() {
        logger.debug("🔍 Iniciando consulta de dominancia de tipos de vehículos");
        
        try {
            Map<String, Object> totalVolume = getTotalVehicleVolume();
            @SuppressWarnings("unchecked")
            Map<String, Integer> totals = (Map<String, Integer>) totalVolume.get("total");
            
            if (totals == null || totals.isEmpty()) {
                logger.warn("⚠️ No se encontraron totales para calcular dominancia");
                return getDefaultDominanceData();
            }
            
            int totalVehicles = totals.values().stream()
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
            
            Map<String, Double> dominance = new HashMap<>();
            if (totalVehicles > 0) {
                totals.forEach((type, count) -> {
                    if (count != null && count > 0) {
                        double percentage = Math.round((count.doubleValue() / totalVehicles) * 10000.0) / 100.0; // 2 decimales
                        dominance.put(type, percentage);
                        logger.debug("🚙 Tipo {}: {} vehículos ({}%)", type, count, percentage);
                    }
                });
            }
            
            logger.info("✅ Dominancia de tipos calculada: {}", dominance);
            return dominance.isEmpty() ? getDefaultDominanceData() : dominance;
            
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.error("❌ Error de ejecución en getVehicleTypeDominance: {}", safeMessage, e);
            return getDefaultDominanceData();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general en getVehicleTypeDominance: {}", safeMessage, e);
            return getDefaultDominanceData();
        }
    }

    public long getTotalDetections() {
        try {
            long count = detectionRepository.countAllDetections();
            logger.debug("📊 Total de detecciones en BD: {}", count);
            return count;
        } catch (org.springframework.dao.DataAccessException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de acceso a datos";
            logger.error("❌ Error de BD obteniendo conteo total: {}", safeMessage, e);
            return 0L;
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.error("❌ Error de ejecución obteniendo conteo total: {}", safeMessage, e);
            return 0L;
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general obteniendo conteo total: {}", safeMessage, e);
            return 0L;
        }
    }

    public Map<String, Object> getAnalysisSummary() {
        logger.debug("🔍 Iniciando resumen de análisis");
        
        try {
            long totalDetections = getTotalDetections();
            Map<String, Object> totalVolume = getTotalVehicleVolume();
            Map<String, Double> avgSpeeds = getAvgSpeedByLane();
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("totalDetections", totalDetections);
            summary.put("totalVolume", totalVolume.get("total"));
            summary.put("avgSpeedByLane", avgSpeeds);
            summary.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            summary.put("dataQuality", totalDetections > 0 ? "Good" : "No Data");
            
            logger.info("✅ Resumen de análisis generado: {} detecciones", totalDetections);
            return summary;
            
        } catch (java.time.DateTimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de formato de fecha";
            logger.error("❌ Error de fecha en getAnalysisSummary: {}", safeMessage, e);
            Map<String, Object> errorSummary = new HashMap<>();
            errorSummary.put("error", "Unable to generate summary - date error");
            errorSummary.put("timestamp", "Error");
            return errorSummary;
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.error("❌ Error de ejecución en getAnalysisSummary: {}", safeMessage, e);
            Map<String, Object> errorSummary = new HashMap<>();
            errorSummary.put("error", "Unable to generate summary - runtime error");
            errorSummary.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return errorSummary;
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general en getAnalysisSummary: {}", safeMessage, e);
            Map<String, Object> errorSummary = new HashMap<>();
            errorSummary.put("error", "Unable to generate summary");
            errorSummary.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return errorSummary;
        }
    }

    // Métodos para estructuras de datos
    public int[] getArrayData() {
        try {
            List<Detection> detections = detectionRepository.findTop50ByOrderByTimestampMsDesc();
            logger.debug("📊 Generando array con {} detecciones", Math.min(detections.size(), 10));
            
            return detections.stream()
                    .limit(10)
                    .mapToInt(d -> {
                        Long timestampMs = d.getTimestampMs();
                        return timestampMs != null ? timestampMs.intValue() % 100 : 0;
                    })
                    .toArray();
        } catch (org.springframework.dao.DataAccessException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de acceso a datos";
            logger.error("❌ Error de BD en getArrayData: {}", safeMessage, e);
            return new int[]{45, 23, 78, 12, 90, 32, 56, 67, 89, 15}; // Datos por defecto
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.error("❌ Error de ejecución en getArrayData: {}", safeMessage, e);
            return new int[]{45, 23, 78, 12, 90, 32, 56, 67, 89, 15}; // Datos por defecto
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general en getArrayData: {}", safeMessage, e);
            return new int[]{45, 23, 78, 12, 90, 32, 56, 67, 89, 15}; // Datos por defecto
        }
    }

    public Object[] getLinkedListData() {
        return getListStructureData();
    }

    public Object[] getDoubleLinkedListData() {
        return getListStructureData();
    }

    public Object[] getCircularDoubleLinkedListData() {
        return getListStructureData();
    }

    public Object[] getStackData() {
        return getListStructureData();
    }

    public Object[] getQueueData() {
        return getListStructureData();
    }

    public Map<String, Object> getTreeData() {
        try {
            Map<String, Object> root = new HashMap<>();
            root.put("value", "Traffic Data");
            
            List<Map<String, Object>> children = new ArrayList<>();
            
            Map<String, Object> vehicles = new HashMap<>();
            vehicles.put("value", "Vehicles");
            vehicles.put("children", Arrays.asList(
                Map.of("value", "Cars"),
                Map.of("value", "Buses"),
                Map.of("value", "Trucks")
            ));
            
            Map<String, Object> lanes = new HashMap<>();
            lanes.put("value", "Lanes");
            lanes.put("children", Arrays.asList(
                Map.of("value", "Lane 1"),
                Map.of("value", "Lane 2"),
                Map.of("value", "Lane 3")
            ));
            
            children.add(vehicles);
            children.add(lanes);
            root.put("children", children);
            
            logger.debug("🌳 Datos de árbol generados");
            return root;
            
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.error("❌ Error de ejecución en getTreeData: {}", safeMessage, e);
            return Map.of("value", "Error", "children", Collections.emptyList());
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general en getTreeData: {}", safeMessage, e);
            return Map.of("value", "Error", "children", Collections.emptyList());
        }
    }

    // Métodos auxiliares
    private Object[] getListStructureData() {
        try {
            List<Detection> detections = detectionRepository.findTop50ByOrderByTimestampMsDesc();
            logger.debug("🔗 Generando estructura de lista con {} detecciones", Math.min(detections.size(), 8));
            
            return detections.stream()
                    .limit(8)
                    .map(d -> {
                        Map<String, Object> item = new HashMap<>();
                        Long id = d.getId();
                        item.put("id", id != null ? id : 0L);
                        item.put("date", d.getDate() != null ? d.getDate() : "N/A");
                        return item;
                    })
                    .toArray();
        } catch (org.springframework.dao.DataAccessException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de acceso a datos";
            logger.error("❌ Error de BD en getListStructureData: {}", safeMessage, e);
            // Datos por defecto
            return IntStream.range(1, 9)
                    .mapToObj(i -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", (long) i);
                        item.put("date", "2025-05-0" + i + " 12:00:00");
                        return item;
                    })
                    .toArray();
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.error("❌ Error de ejecución en getListStructureData: {}", safeMessage, e);
            // Datos por defecto
            return IntStream.range(1, 9)
                    .mapToObj(i -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", (long) i);
                        item.put("date", "2025-05-0" + i + " 12:00:00");
                        return item;
                    })
                    .toArray();
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.error("❌ Error general en getListStructureData: {}", safeMessage, e);
            // Datos por defecto
            return IntStream.range(1, 9)
                    .mapToObj(i -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", (long) i);
                        item.put("date", "2025-05-0" + i + " 12:00:00");
                        return item;
                    })
                    .toArray();
        }
    }

    private String extractHourFromDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            // Extraer la hora del formato "2025-05-08 21:07:12"
            String[] parts = dateStr.split(" ");
            if (parts.length > 1) {
                String timePart = parts[1];
                String[] timeParts = timePart.split(":");
                if (timeParts.length > 0) {
                    return timeParts[0] + ":00";
                }
            }
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución";
            logger.debug("⚠️ Error de ejecución extrayendo hora de fecha '{}': {}", dateStr, safeMessage);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido";
            logger.debug("⚠️ Error general extrayendo hora de fecha '{}': {}", dateStr, safeMessage);
        }
        return null;
    }

    private String getDayType() {
        // Simplificado: retorna "weekday" o "weekend"
        // En una implementación real, se analizaría la fecha
        return "weekday";
    }

    // Métodos para datos por defecto cuando no hay datos en la BD
    private Map<String, Object> getDefaultTotalVolumeData() {
        logger.debug("📊 Usando datos por defecto para volumen total");
        Map<String, Object> defaultData = new HashMap<>();
        defaultData.put("total", Map.of("car", 0, "bus", 0, "truck", 0));
        defaultData.put("hourly", Map.of("08:00", 0, "09:00", 0, "10:00", 0));
        defaultData.put("daily", Map.of("weekday", 0, "weekend", 0));
        return defaultData;
    }

    private Map<String, Map<String, Integer>> getDefaultLaneData() {
        logger.debug("🛣️ Usando datos por defecto para carriles");
        Map<String, Map<String, Integer>> defaultData = new HashMap<>();
        defaultData.put("lane_1", Map.of("car", 0, "bus", 0, "truck", 0));
        defaultData.put("lane_2", Map.of("car", 0, "bus", 0, "truck", 0));
        defaultData.put("lane_3", Map.of("car", 0, "bus", 0, "truck", 0));
        return defaultData;
    }

    private Map<String, Integer> getDefaultHourlyPattern() {
        logger.debug("⏰ Usando datos por defecto para patrones horarios");
        Map<String, Integer> defaultPattern = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            defaultPattern.put(String.format("%02d:00", i), 0);
        }
        return defaultPattern;
    }

    private Map<String, Double> getDefaultSpeedData() {
        logger.debug("🏎️ Usando datos por defecto para velocidades");
        Map<String, Double> defaultSpeeds = new HashMap<>();
        defaultSpeeds.put("lane_1", 0.0);
        defaultSpeeds.put("lane_2", 0.0);
        defaultSpeeds.put("lane_3", 0.0);
        return defaultSpeeds;
    }

    private Map<String, Double> getDefaultDominanceData() {
        logger.debug("🚙 Usando datos por defecto para dominancia");
        Map<String, Double> defaultDominance = new HashMap<>();
        defaultDominance.put("car", 0.0);
        defaultDominance.put("bus", 0.0);
        defaultDominance.put("truck", 0.0);
        return defaultDominance;
    }

    private Map<String, Object> getDefaultTrafficEvolution() {
        logger.debug("📈 Usando datos por defecto para evolución del tráfico");
        Map<String, Object> defaultEvolution = new HashMap<>();
        defaultEvolution.put("timestamps", Arrays.asList("08:00", "09:00", "10:00"));
        defaultEvolution.put("car", Arrays.asList(0, 0, 0));
        defaultEvolution.put("bus", Arrays.asList(0, 0, 0));
        defaultEvolution.put("truck", Arrays.asList(0, 0, 0));
        return defaultEvolution;
    }

    private Map<String, Object> getDefaultSpeedEvolution() {
        logger.debug("🏎️ Usando datos por defecto para evolución de velocidad");
        Map<String, Object> defaultEvolution = new HashMap<>();
        defaultEvolution.put("timestamps", Arrays.asList("08:00", "09:00", "10:00"));
        defaultEvolution.put("lane_1", Arrays.asList(0.0, 0.0, 0.0));
        defaultEvolution.put("lane_2", Arrays.asList(0.0, 0.0, 0.0));
        defaultEvolution.put("lane_3", Arrays.asList(0.0, 0.0, 0.0));
        return defaultEvolution;
    }
}