package com.example.demo.service;

import com.example.demo.entity.Detection;
import com.example.demo.repository.DetectionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DetectionAnalysisService {

    private final DetectionRepository detectionRepository;
    private final ObjectMapper objectMapper;

    public Map<String, Object> getTotalVehicleVolume() {
        List<Detection> detections = detectionRepository.findAllOrderByTimestamp();
        
        Map<String, Integer> totalCounts = new HashMap<>();
        Map<String, Integer> hourlyCounts = new HashMap<>();
        Map<String, Integer> dailyCounts = new HashMap<>();
        
        for (Detection detection : detections) {
            try {
                if (detection.getObjectsTotal() != null && !detection.getObjectsTotal().trim().isEmpty()) {
                    Map<String, Integer> objects = objectMapper.readValue(
                        detection.getObjectsTotal(), 
                        new TypeReference<Map<String, Integer>>() {}
                    );
                    
                    // Sumar totales
                    objects.forEach((key, value) -> 
                        totalCounts.merge(key, value, Integer::sum)
                    );
                    
                    // Análisis horario
                    String hour = extractHourFromDate(detection.getDate());
                    if (hour != null) {
                        int hourlyTotal = objects.values().stream().mapToInt(Integer::intValue).sum();
                        hourlyCounts.merge(hour, hourlyTotal, Integer::sum);
                    }
                    
                    // Análisis diario (simplificado)
                    String dayType = getDayType(detection.getDate());
                    int dailyTotal = objects.values().stream().mapToInt(Integer::intValue).sum();
                    dailyCounts.merge(dayType, dailyTotal, Integer::sum);
                }
            } catch (Exception e) {
                // Log error but continue processing
                System.err.println("Error processing detection: " + e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("total", totalCounts.isEmpty() ? Map.of("car", 0, "bus", 0, "truck", 0) : totalCounts);
        result.put("hourly", hourlyCounts);
        result.put("daily", dailyCounts);
        
        return result;
    }

    public Map<String, Map<String, Integer>> getVehicleVolumeByLane() {
        List<Detection> detections = detectionRepository.findTop50ByOrderByTimestampMsDesc();
        Map<String, Map<String, Integer>> laneData = new HashMap<>();
        
        for (Detection detection : detections) {
            try {
                if (detection.getObjectsByLane() != null && !detection.getObjectsByLane().trim().isEmpty()) {
                    Map<String, Map<String, Integer>> lanes = objectMapper.readValue(
                        detection.getObjectsByLane(), 
                        new TypeReference<Map<String, Map<String, Integer>>>() {}
                    );
                    
                    lanes.forEach((lane, vehicles) -> {
                        laneData.computeIfAbsent(lane, k -> new HashMap<>());
                        vehicles.forEach((vehicleType, count) -> 
                            laneData.get(lane).merge(vehicleType, count, Integer::sum)
                        );
                    });
                }
            } catch (Exception e) {
                System.err.println("Error processing lane data: " + e.getMessage());
            }
        }
        
        return laneData.isEmpty() ? getDefaultLaneData() : laneData;
    }

    public Map<String, Integer> getHourlyPatterns() {
        List<Detection> detections = detectionRepository.findAllOrderByTimestamp();
        Map<String, Integer> hourlyPattern = new HashMap<>();
        
        for (Detection detection : detections) {
            try {
                String hour = extractHourFromDate(detection.getDate());
                if (hour != null && detection.getObjectsTotal() != null) {
                    Map<String, Integer> objects = objectMapper.readValue(
                        detection.getObjectsTotal(), 
                        new TypeReference<Map<String, Integer>>() {}
                    );
                    int totalVehicles = objects.values().stream().mapToInt(Integer::intValue).sum();
                    hourlyPattern.merge(hour, totalVehicles, Integer::sum);
                }
            } catch (Exception e) {
                System.err.println("Error processing hourly pattern: " + e.getMessage());
            }
        }
        
        return hourlyPattern.isEmpty() ? getDefaultHourlyPattern() : hourlyPattern;
    }

    public Map<String, Double> getAvgSpeedByLane() {
        List<Detection> detections = detectionRepository.findTop50ByOrderByTimestampMsDesc();
        Map<String, List<Double>> speedsByLane = new HashMap<>();
        
        for (Detection detection : detections) {
            try {
                if (detection.getAvgSpeedByLane() != null && !detection.getAvgSpeedByLane().trim().isEmpty()) {
                    Map<String, Double> speeds = objectMapper.readValue(
                        detection.getAvgSpeedByLane(), 
                        new TypeReference<Map<String, Double>>() {}
                    );
                    
                    speeds.forEach((lane, speed) -> {
                        speedsByLane.computeIfAbsent(lane, k -> new ArrayList<>()).add(speed);
                    });
                }
            } catch (Exception e) {
                System.err.println("Error processing speed data: " + e.getMessage());
            }
        }
        
        Map<String, Double> avgSpeeds = new HashMap<>();
        speedsByLane.forEach((lane, speeds) -> {
            double average = speeds.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            avgSpeeds.put(lane, average);
        });
        
        return avgSpeeds.isEmpty() ? getDefaultSpeedData() : avgSpeeds;
    }

    public Object[] getBottlenecks() {
        Map<String, Double> avgSpeeds = getAvgSpeedByLane();
        Map<String, Map<String, Integer>> laneData = getVehicleVolumeByLane();
        
        List<Map<String, Object>> bottlenecks = new ArrayList<>();
        
        avgSpeeds.forEach((lane, avgSpeed) -> {
            if (avgSpeed < 15.0) { // Threshold for bottleneck
                Map<String, Integer> vehicles = laneData.getOrDefault(lane, new HashMap<>());
                int totalVehicles = vehicles.values().stream().mapToInt(Integer::intValue).sum();
                int heavyVehicles = vehicles.getOrDefault("truck", 0) + vehicles.getOrDefault("bus", 0);
                
                Map<String, Object> bottleneck = new HashMap<>();
                bottleneck.put("lane", lane);
                bottleneck.put("avgSpeed", avgSpeed);
                bottleneck.put("totalVehicles", totalVehicles);
                bottleneck.put("heavyVehicles", heavyVehicles);
                
                bottlenecks.add(bottleneck);
            }
        });
        
        return bottlenecks.toArray();
    }

    public Map<String, Object> getTrafficEvolution() {
        List<Detection> detections = detectionRepository.findTop50ByOrderByTimestampMsDesc();
        
        List<String> timestamps = new ArrayList<>();
        List<Integer> carCounts = new ArrayList<>();
        List<Integer> busCounts = new ArrayList<>();
        List<Integer> truckCounts = new ArrayList<>();
        
        for (Detection detection : detections) {
            try {
                timestamps.add(detection.getDate());
                
                if (detection.getObjectsTotal() != null && !detection.getObjectsTotal().trim().isEmpty()) {
                    Map<String, Integer> objects = objectMapper.readValue(
                        detection.getObjectsTotal(), 
                        new TypeReference<Map<String, Integer>>() {}
                    );
                    
                    carCounts.add(objects.getOrDefault("car", 0));
                    busCounts.add(objects.getOrDefault("bus", 0));
                    truckCounts.add(objects.getOrDefault("truck", 0));
                } else {
                    carCounts.add(0);
                    busCounts.add(0);
                    truckCounts.add(0);
                }
            } catch (Exception e) {
                System.err.println("Error processing traffic evolution: " + e.getMessage());
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
        
        return result;
    }

    public Map<String, Object> getSpeedEvolution() {
        List<Detection> detections = detectionRepository.findTop50ByOrderByTimestampMsDesc();
        
        List<String> timestamps = new ArrayList<>();
        List<Double> lane1Speeds = new ArrayList<>();
        List<Double> lane2Speeds = new ArrayList<>();
        List<Double> lane3Speeds = new ArrayList<>();
        
        for (Detection detection : detections) {
            try {
                timestamps.add(detection.getDate());
                
                if (detection.getAvgSpeedByLane() != null && !detection.getAvgSpeedByLane().trim().isEmpty()) {
                    Map<String, Double> speeds = objectMapper.readValue(
                        detection.getAvgSpeedByLane(), 
                        new TypeReference<Map<String, Double>>() {}
                    );
                    
                    lane1Speeds.add(speeds.getOrDefault("lane_1", 0.0));
                    lane2Speeds.add(speeds.getOrDefault("lane_2", 0.0));
                    lane3Speeds.add(speeds.getOrDefault("lane_3", 0.0));
                } else {
                    lane1Speeds.add(0.0);
                    lane2Speeds.add(0.0);
                    lane3Speeds.add(0.0);
                }
            } catch (Exception e) {
                System.err.println("Error processing speed evolution: " + e.getMessage());
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
        
        return result;
    }

    public Map<String, Double> getVehicleTypeDominance() {
        Map<String, Object> totalVolume = getTotalVehicleVolume();
        @SuppressWarnings("unchecked")
        Map<String, Integer> totals = (Map<String, Integer>) totalVolume.get("total");
        
        int totalVehicles = totals.values().stream().mapToInt(Integer::intValue).sum();
        
        Map<String, Double> dominance = new HashMap<>();
        if (totalVehicles > 0) {
            totals.forEach((type, count) -> {
                double percentage = (count.doubleValue() / totalVehicles) * 100;
                dominance.put(type, percentage);
            });
        }
        
        return dominance.isEmpty() ? getDefaultDominanceData() : dominance;
    }

    public long getTotalDetections() {
        return detectionRepository.countAllDetections();
    }

    public Map<String, Object> getAnalysisSummary() {
        long totalDetections = detectionRepository.countAllDetections();
        Map<String, Object> totalVolume = getTotalVehicleVolume();
        Map<String, Double> avgSpeeds = getAvgSpeedByLane();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDetections", totalDetections);
        summary.put("totalVolume", totalVolume.get("total"));
        summary.put("avgSpeedByLane", avgSpeeds);
        summary.put("lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        return summary;
    }

    // Métodos para estructuras de datos
    public int[] getArrayData() {
        List<Detection> detections = detectionRepository.findTop50ByOrderByTimestampMsDesc();
        return detections.stream()
                .limit(10)
                .mapToInt(d -> d.getTimestampMs().intValue() % 100)
                .toArray();
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
    }

    // Métodos auxiliares
    private Object[] getListStructureData() {
        List<Detection> detections = detectionRepository.findTop50ByOrderByTimestampMsDesc();
        return detections.stream()
                .limit(8)
                .map(d -> Map.of(
                    "id", d.getId(),
                    "date", d.getDate() != null ? d.getDate() : "N/A"
                ))
                .toArray();
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
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return null;
    }

    private String getDayType(String dateStr) {
        // Simplificado: retorna "weekday" o "weekend"
        return "weekday"; // Placeholder
    }

    // Datos por defecto cuando no hay datos en la BD
    private Map<String, Map<String, Integer>> getDefaultLaneData() {
        Map<String, Map<String, Integer>> defaultData = new HashMap<>();
        defaultData.put("lane_1", Map.of("car", 0, "bus", 0, "truck", 0));
        defaultData.put("lane_2", Map.of("car", 0, "bus", 0, "truck", 0));
        defaultData.put("lane_3", Map.of("car", 0, "bus", 0, "truck", 0));
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
        Map<String, Double> defaultSpeeds = new HashMap<>();
        defaultSpeeds.put("lane_1", 0.0);
        defaultSpeeds.put("lane_2", 0.0);
        defaultSpeeds.put("lane_3", 0.0);
        return defaultSpeeds;
    }

    private Map<String, Double> getDefaultDominanceData() {
        Map<String, Double> defaultDominance = new HashMap<>();
        defaultDominance.put("car", 0.0);
        defaultDominance.put("bus", 0.0);
        defaultDominance.put("truck", 0.0);
        return defaultDominance;
    }
}