package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "detections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Detection {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "timestamp_ms")
    private Long timestampMs;
    
    @Column(name = "date")
    private String date;
    
    @Column(name = "objects_total", columnDefinition = "JSON")
    private String objectsTotal;
    
    @Column(name = "objects_by_lane", columnDefinition = "JSON")
    private String objectsByLane;
    
    @Column(name = "avg_speed_by_lane", columnDefinition = "JSON")
    private String avgSpeedByLane;
}