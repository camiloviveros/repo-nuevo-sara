package com.example.demo.service;

import com.example.demo.entity.Detection;
import com.example.demo.repository.DetectionRepository;
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
        logger.debug("🔍 Consultando volumen total de vehículos");
        
        try {
            List<Detection> detections = detectionRepository.findAll();
            logger.debug("📊 Obtenidas {} detecciones de la BD", detections.size());
            
            if (detections.isEmpty()) {
                return getDefaultTotalVolumeData();
            }
            
            Map<String, Integer> totalCounts = new HashMap<>();
            Map<String, Integer> hourlyCounts = new HashMap<>();
            
            for (Detection detection : detections) {
                try {
                    if (isValidJson(detection.getObjectsTotal())) {
                        Map<String, Integer> objects = parseJson(detection.getObjectsTotal(), 
                            new TypeReference<Map<String, Integer>>() {});
                        
                        if (objects != null) {
                            objects.forEach((key, value) -> {
                                if (value != null && value > 0) {
                                    totalCounts.merge(key, value, Integer::sum);
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
                        }
                    }
                } catch (Exception e) {
                    logger.debug("⚠️ Error procesando detección ID {}: {}", detection.getId(), e.getMessage());
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("total", totalCounts.isEmpty() ? getDefaultTotals() : totalCounts);
            result.put("hourly", hourlyCounts);
            result.put("daily", Map.of("weekday", getTotalVehicleCount(totalCounts)));
            
            logger.info("✅ Volumen total calculado: {}", totalCounts);
            return result;
            
        } catch (Exception e) {
            logger.error("❌ Error en getTotalVehicleVolume: {}", e.getMessage());
            return getDefaultTotalVolumeData();
        }
    }

    public Map<String, Map<String, Integer>> getVehicleVolumeByLane() {
        logger.debug("🔍 Consultando volumen por carril");
        
        try {
            List<Detection> detections = detectionRepository.findAll();
            
            if (detections.isEmpty()) {
                return getDefaultLaneData();
            }
            
            Map<String, Map<String, Integer>> laneData = new HashMap<>();
            
            for (Detection detection : detections) {
                try {
                    if (isValidJson(detection.getObjectsByLane())) {
                        Map<String, Map<String, Integer>> lanes = parseJson(detection.getObjectsByLane(), 
                            new TypeReference<Map<String, Map<String, Integer>>>() {});
                        
                        if (lanes != null) {
                            lanes.forEach((lane, vehicles) -> {
                                if (vehicles != null) {
                                    laneData.computeIfAbsent(lane, k -> new HashMap<>());
                                    vehicles.forEach((vehicleType, count) -> {
                                        if (count != null && count > 0) {
                                            laneData.get(lane).merge(vehicleType, count, Integer::sum);
                                        }
                                    });
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    logger.debug("⚠️ Error procesando carril para detección ID {}: {}", detection.getId(), e.getMessage());
                }
            }
            
            logger.info("✅ Datos de carril calculados para {} carriles", laneData.size());
            return laneData.isEmpty() ? getDefaultLaneData() : laneData;
            
        } catch (Exception e) {
            logger.error("❌ Error en getVehicleVolumeByLane: {}", e.getMessage());
            return getDefaultLaneData();
        }
    }

    public Map<String, Integer> getHourlyPatterns() {
        logger.debug("🔍 Consultando patrones horarios");
        
        try {
            List<Detection> detections = detectionRepository.findAll();
            
            if (detections.isEmpty()) {
                return getDefaultHourlyPattern();
            }
            
            Map<String, Integer> hourlyPattern = new HashMap<>();
            
            for (Detection detection : detections) {
                try {
                    String hour = extractHourFromDate(detection.getDate());
                    if (hour != null && isValidJson(detection.getObjectsTotal())) {
                        Map<String, Integer> objects = parseJson(detection.getObjectsTotal(), 
                            new TypeReference<Map<String, Integer>>() {});
                        
                        if (objects != null) {
                            int totalVehicles = objects.values().stream()
                                .filter(Objects::nonNull)
                                .mapToInt(Integer::intValue)
                                .sum();
                            if (totalVehicles > 0) {
                                hourlyPattern.merge(hour, totalVehicles, Integer::sum);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.debug("⚠️ Error procesando patrón horario para detección ID {}: {}", detection.getId(), e.getMessage());
                }
            }
            
            logger.info("✅ Patrones horarios calculados para {} horas", hourlyPattern.size());
            return hourlyPattern.isEmpty() ? getDefaultHourlyPattern() : hourlyPattern;
            
        } catch (Exception e) {
            logger.error("❌ Error en getHourlyPatterns: {}", e.getMessage());
            return getDefaultHourlyPattern();
        }
    }

    public Map<String, Double> getAvgSpeedByLane() {
        logger.debug("🔍 Consultando velocidades por carril");
        
        try {
            List<Detection> detections = detectionRepository.findAll();
            
            if (detections.isEmpty()) {
                return getDefaultSpeedData();
            }
            
            Map<String, List<Double>> speedsByLane = new HashMap<>();
            
            for (Detection detection : detections) {
                try {
                    if (isValidJson(detection.getAvgSpeedByLane())) {
                        Map<String, Double> speeds = parseJson(detection.getAvgSpeedByLane(), 
                            new TypeReference<Map<String, Double>>() {});
                        
                        if (speeds != null) {
                            speeds.forEach((lane, speed) -> {
                                if (speed != null && speed > 0) {
                                    speedsByLane.computeIfAbsent(lane, k -> new ArrayList<>()).add(speed);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    logger.debug("⚠️ Error procesando velocidad para detección ID {}: {}", detection.getId(), e.getMessage());
                }
            }
            
            Map<String, Double> avgSpeeds = new HashMap<>();
            speedsByLane.forEach((lane, speeds) -> {
                if (!speeds.isEmpty()) {
                    double average = speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    avgSpeeds.put(lane, Math.round(average * 100.0) / 100.0);
                }
            });
            
            logger.info("✅ Velocidades calculadas para {} carriles", avgSpeeds.size());
            return avgSpeeds.isEmpty() ? getDefaultSpeedData() : avgSpeeds;
            
        } catch (Exception e) {
            logger.error("❌ Error en getAvgSpeedByLane: {}", e.getMessage());
            return getDefaultSpeedData();
        }
    }

    public Object[] getBottlenecks() {
        try {
            Map<String, Double> avgSpeeds = getAvgSpeedByLane();
            Map<String, Map<String, Integer>> laneData = getVehicleVolumeByLane();
            
            List<Map<String, Object>> bottlenecks = new ArrayList<>();
            
            avgSpeeds.forEach((lane, avgSpeed) -> {
                if (avgSpeed < 15.0) {
                    Map<String, Integer> vehicles = laneData.getOrDefault(lane, new HashMap<>());
                    int totalVehicles = vehicles.values().stream()
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .sum();
                    
                    Map<String, Object> bottleneck = new HashMap<>();
                    bottleneck.put("lane", lane);
                    bottleneck.put("avgSpeed", avgSpeed);
                    bottleneck.put("totalVehicles", totalVehicles);
                    
                    bottlenecks.add(bottleneck);
                }
            });
            
            return bottlenecks.toArray();
            
        } catch (Exception e) {
            logger.error("❌ Error en getBottlenecks: {}", e.getMessage());
            return new Object[0];
        }
    }

    public Map<String, Object> getTrafficEvolution() {
        try {
            List<Detection> detections = detectionRepository.findAll();
            detections.sort(Comparator.comparing(Detection::getTimestampMs));
            
            List<String> timestamps = new ArrayList<>();
            List<Integer> carCounts = new ArrayList<>();
            List<Integer> busCounts = new ArrayList<>();
            List<Integer> truckCounts = new ArrayList<>();
            
            for (Detection detection : detections) {
                timestamps.add(detection.getDate() != null ? detection.getDate() : "N/A");
                
                try {
                    if (isValidJson(detection.getObjectsTotal())) {
                        Map<String, Integer> objects = parseJson(detection.getObjectsTotal(), 
                            new TypeReference<Map<String, Integer>>() {});
                        
                        if (objects != null) {
                            carCounts.add(objects.getOrDefault("car", 0));
                            busCounts.add(objects.getOrDefault("bus", 0));
                            truckCounts.add(objects.getOrDefault("truck", 0));
                        } else {
                            addZeroCounts(carCounts, busCounts, truckCounts);
                        }
                    } else {
                        addZeroCounts(carCounts, busCounts, truckCounts);
                    }
                } catch (Exception e) {
                    addZeroCounts(carCounts, busCounts, truckCounts);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("timestamps", timestamps);
            result.put("car", carCounts);
            result.put("bus", busCounts);
            result.put("truck", truckCounts);
            
            return result;
            
        } catch (Exception e) {
            logger.error("❌ Error en getTrafficEvolution: {}", e.getMessage());
            return getDefaultTrafficEvolution();
        }
    }

    public Map<String, Object> getSpeedEvolution() {
        try {
            List<Detection> detections = detectionRepository.findAll();
            detections.sort(Comparator.comparing(Detection::getTimestampMs));
            
            List<String> timestamps = new ArrayList<>();
            List<Double> lane1Speeds = new ArrayList<>();
            List<Double> lane2Speeds = new ArrayList<>();
            List<Double> lane3Speeds = new ArrayList<>();
            
            for (Detection detection : detections) {
                timestamps.add(detection.getDate() != null ? detection.getDate() : "N/A");
                
                try {
                    if (isValidJson(detection.getAvgSpeedByLane())) {
                        Map<String, Double> speeds = parseJson(detection.getAvgSpeedByLane(), 
                            new TypeReference<Map<String, Double>>() {});
                        
                        if (speeds != null) {
                            lane1Speeds.add(speeds.getOrDefault("lane_1", 0.0));
                            lane2Speeds.add(speeds.getOrDefault("lane_2", 0.0));
                            lane3Speeds.add(speeds.getOrDefault("lane_3", 0.0));
                        } else {
                            addZeroSpeeds(lane1Speeds, lane2Speeds, lane3Speeds);
                        }
                    } else {
                        addZeroSpeeds(lane1Speeds, lane2Speeds, lane3Speeds);
                    }
                } catch (Exception e) {
                    addZeroSpeeds(lane1Speeds, lane2Speeds, lane3Speeds);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("timestamps", timestamps);
            result.put("lane_1", lane1Speeds);
            result.put("lane_2", lane2Speeds);
            result.put("lane_3", lane3Speeds);
            
            return result;
            
        } catch (Exception e) {
            logger.error("❌ Error en getSpeedEvolution: {}", e.getMessage());
            return getDefaultSpeedEvolution();
        }
    }

    public Map<String, Double> getVehicleTypeDominance() {
        try {
            Map<String, Object> totalVolume = getTotalVehicleVolume();
            @SuppressWarnings("unchecked")
            Map<String, Integer> totals = (Map<String, Integer>) totalVolume.get("total");
            
            if (totals == null || totals.isEmpty()) {
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
                        double percentage = Math.round((count.doubleValue() / totalVehicles) * 10000.0) / 100.0;
                        dominance.put(type, percentage);
                    }
                });
            }
            
            return dominance.isEmpty() ? getDefaultDominanceData() : dominance;
            
        } catch (Exception e) {
            logger.error("❌ Error en getVehicleTypeDominance: {}", e.getMessage());
            return getDefaultDominanceData();
        }
    }

    public long getTotalDetections() {
        try {
            return detectionRepository.count();
        } catch (Exception e) {
            logger.error("❌ Error obteniendo conteo total: {}", e.getMessage());
            return 0L;
        }
    }

    public Map<String, Object> getAnalysisSummary() {
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
            
            return summary;
            
        } catch (Exception e) {
            logger.error("❌ Error en getAnalysisSummary: {}", e.getMessage());
            Map<String, Object> errorSummary = new HashMap<>();
            errorSummary.put("error", "Unable to generate summary");
            errorSummary.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            return errorSummary;
        }
    }

    // Métodos para estructuras de datos
    public int[] getArrayData() {
        try {
            List<Detection> detections = detectionRepository.findAll();
            return detections.stream()
                    .limit(10)
                    .mapToInt(d -> d.getTimestampMs() != null ? d.getTimestampMs().intValue() % 100 : 0)
                    .toArray();
        } catch (Exception e) {
            return new int[]{45, 23, 78, 12, 90, 32, 56, 67, 89, 15};
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
            
            return root;
            
        } catch (Exception e) {
            return Map.of("value", "Error", "children", Collections.emptyList());
        }
    }

    // Métodos auxiliares
    private Object[] getListStructureData() {
        try {
            List<Detection> detections = detectionRepository.findAll();
            return detections.stream()
                    .limit(8)
                    .map(d -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", d.getId() != null ? d.getId() : 0L);
                        item.put("date", d.getDate() != null ? d.getDate() : "N/A");
                        return item;
                    })
                    .toArray();
        } catch (Exception e) {
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

    private boolean isValidJson(String json) {
        return json != null && !json.trim().isEmpty() && !json.equals("{}") && !json.equals("null");
    }

    private <T> T parseJson(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            logger.debug("⚠️ Error parsing JSON: {}", e.getMessage());
            return null;
        }
    }

    private String extractHourFromDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            String[] parts = dateStr.split(" ");
            if (parts.length > 1) {
                String timePart = parts[1];
                String[] timeParts = timePart.split(":");
                if (timeParts.length > 0) {
                    return timeParts[0] + ":00";
                }
            }
        } catch (Exception e) {
            logger.debug("⚠️ Error extrayendo hora de fecha '{}': {}", dateStr, e.getMessage());
        }
        return null;
    }

    private void addZeroCounts(List<Integer> carCounts, List<Integer> busCounts, List<Integer> truckCounts) {
        carCounts.add(0);
        busCounts.add(0);
        truckCounts.add(0);
    }

    private void addZeroSpeeds(List<Double> lane1Speeds, List<Double> lane2Speeds, List<Double> lane3Speeds) {
        lane1Speeds.add(0.0);
        lane2Speeds.add(0.0);
        lane3Speeds.add(0.0);
    }

    private int getTotalVehicleCount(Map<String, Integer> totals) {
        return totals.values().stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    // Métodos para datos por defecto
    private Map<String, Integer> getDefaultTotals() {
        return Map.of("car", 0, "bus", 0, "truck", 0);
    }

    private Map<String, Object> getDefaultTotalVolumeData() {
        Map<String, Object> defaultData = new HashMap<>();
        defaultData.put("total", getDefaultTotals());
        defaultData.put("hourly", Map.of("08:00", 0, "09:00", 0, "10:00", 0));
        defaultData.put("daily", Map.of("weekday", 0, "weekend", 0));
        return defaultData;
    }

    private Map<String, Map<String, Integer>> getDefaultLaneData() {
        Map<String, Map<String, Integer>> defaultData = new HashMap<>();
        defaultData.put("lane_1", getDefaultTotals());
        defaultData.put("lane_2", getDefaultTotals());
        defaultData.put("lane_3", getDefaultTotals());
        return defaultData;
    }

    private Map<String, Integer> getDefaultHourlyPattern() {
        Map<String, Integer> defaultPattern = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            defaultPattern.put(String.format("%02d:00", i), 0);
        }
        return defaultPattern;
    }

    private Map<String, Double> getDefaultSpeedData() {
        return Map.of("lane_1", 0.0, "lane_2", 0.0, "lane_3", 0.0);
    }

    private Map<String, Double> getDefaultDominanceData() {
        return Map.of("car", 0.0, "bus", 0.0, "truck", 0.0);
    }

    private Map<String, Object> getDefaultTrafficEvolution() {
        Map<String, Object> defaultEvolution = new HashMap<>();
        defaultEvolution.put("timestamps", Arrays.asList("08:00", "09:00", "10:00"));
        defaultEvolution.put("car", Arrays.asList(0, 0, 0));
        defaultEvolution.put("bus", Arrays.asList(0, 0, 0));
        defaultEvolution.put("truck", Arrays.asList(0, 0, 0));
        return defaultEvolution;
    }

    private Map<String, Object> getDefaultSpeedEvolution() {
        Map<String, Object> defaultEvolution = new HashMap<>();
        defaultEvolution.put("timestamps", Arrays.asList("08:00", "09:00", "10:00"));
        defaultEvolution.put("lane_1", Arrays.asList(0.0, 0.0, 0.0));
        defaultEvolution.put("lane_2", Arrays.asList(0.0, 0.0, 0.0));
        defaultEvolution.put("lane_3", Arrays.asList(0.0, 0.0, 0.0));
        return defaultEvolution;
    }
}