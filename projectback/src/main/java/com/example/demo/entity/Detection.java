package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    
    @Column(name = "date", length = 50)
    private String date;
    
    @Column(name = "objects_total", columnDefinition = "TEXT")
    private String objectsTotal;
    
    @Column(name = "objects_by_lane", columnDefinition = "TEXT")
    private String objectsByLane;
    
    @Column(name = "avg_speed_by_lane", columnDefinition = "TEXT")
    private String avgSpeedByLane;
}