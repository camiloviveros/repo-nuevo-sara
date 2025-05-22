package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ProjectbackApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectbackApplication.class, args);
        System.out.println("=================================");
        System.out.println("🚀 Servidor Spring Boot iniciado");
        System.out.println("📡 API disponible en: http://localhost:8080/api");
        System.out.println("🔍 Endpoints principales:");
        System.out.println("   - GET /api/detections/volume/total");
        System.out.println("   - GET /api/detections/volume/by-lane");
        System.out.println("   - GET /api/detections/patterns/hourly");
        System.out.println("   - GET /api/detections/lanes/speed");
        System.out.println("   - GET /api/detections/analysis/summary");
        System.out.println("📊 Dashboard disponible en: http://localhost:3000");
        System.out.println("=================================");
    }
}