package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class ProjectbackApplication {

    private static final Logger logger = LoggerFactory.getLogger(ProjectbackApplication.class);

    public static void main(String[] args) {
        try {
            // Configurar propiedades del sistema antes del arranque
            configureSystemProperties();
            
            // Iniciar la aplicación Spring Boot
            ConfigurableApplicationContext context = SpringApplication.run(ProjectbackApplication.class, args);
            
            // Verificar que el contexto se inició correctamente
            if (context.isActive()) {
                logSuccessfulStartup();
            } else {
                logger.error("❌ El contexto de Spring no está activo");
                System.exit(1);
            }
            
        } catch (IllegalStateException e) {
            logger.error("❌ Error de configuración o estado de la aplicación: {}", e.getMessage());
            System.err.println("❌ Error de configuración. Revisa application.properties y que el puerto 8080 esté libre");
            System.exit(1);
        } catch (IllegalArgumentException e) {
            logger.error("❌ Error en argumentos de configuración: {}", e.getMessage());
            System.err.println("❌ Error en parámetros de configuración de la aplicación");
            System.exit(1);
        } catch (RuntimeException e) {
            logger.error("❌ Error de runtime iniciando la aplicación: {}", e.getMessage(), e);
            System.err.println("❌ Error iniciando la aplicación: " + e.getMessage());
            System.exit(1);
        } catch (Error e) {
            logger.error("❌ Error crítico del sistema: {}", e.getMessage(), e);
            System.err.println("❌ Error crítico del sistema: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Configura propiedades del sistema antes del arranque
     */
    private static void configureSystemProperties() {
        // Configurar timezone por defecto si no está configurado
        if (System.getProperty("user.timezone") == null) {
            System.setProperty("user.timezone", "America/Bogota");
        }
        
        // Configurar encoding por defecto
        if (System.getProperty("file.encoding") == null) {
            System.setProperty("file.encoding", "UTF-8");
        }
        
        // Deshabilitar banner de Spring Boot en logs de error
        System.setProperty("spring.main.banner-mode", "console");
    }

    /**
     * Registra mensajes de éxito de arranque usando el logger
     */
    private static void logSuccessfulStartup() {
        logger.info("=================================");
        logger.info("🚀 Servidor Spring Boot iniciado correctamente");
        logger.info("📡 API disponible en: http://localhost:8080/api");
        logger.info("🔍 Endpoints principales:");
        logger.info("   - GET /api/detections/test");
        logger.info("   - GET /api/detections/health");
        logger.info("   - GET /api/detections/count");
        logger.info("   - GET /api/detections/volume/total");
        logger.info("   - GET /api/detections/volume/by-lane");
        logger.info("   - GET /api/detections/patterns/hourly");
        logger.info("   - GET /api/detections/lanes/speed");
        logger.info("   - GET /api/detections/analysis/summary");
        logger.info("   - POST /api/detections/load-json");
        logger.info("🧪 Prueba la API con: http://localhost:8080/api/detections/test");
        logger.info("=================================");
        
        // También mostrar en consola para visibilidad inmediata usando text block (Java 17+)
        System.out.println("""
            
            =================================
            🚀 Servidor Spring Boot iniciado correctamente
            📡 API disponible en: http://localhost:8080/api
            🧪 Prueba la API con: http://localhost:8080/api/detections/test
            =================================
            """);
    }
}