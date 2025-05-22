package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class ProjectbackApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(ProjectbackApplication.class, args);
            
            System.out.println("=================================");
            System.out.println("🚀 Servidor Spring Boot iniciado correctamente");
            System.out.println("📡 API disponible en: http://localhost:8080/api");
            System.out.println("🔍 Endpoints principales:");
            System.out.println("   - GET /api/detections/test");
            System.out.println("   - GET /api/detections/health");
            System.out.println("   - GET /api/detections/count");
            System.out.println("   - GET /api/detections/volume/total");
            System.out.println("   - GET /api/detections/volume/by-lane");
            System.out.println("   - GET /api/detections/patterns/hourly");
            System.out.println("   - GET /api/detections/lanes/speed");
            System.out.println("   - GET /api/detections/analysis/summary");
            System.out.println("   - POST /api/detections/load-json");
            System.out.println("🧪 Prueba la API con: http://localhost:8080/api/detections/test");
            System.out.println("=================================");
            
        } catch (Exception e) {
            System.err.println("❌ Error iniciando la aplicación: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}