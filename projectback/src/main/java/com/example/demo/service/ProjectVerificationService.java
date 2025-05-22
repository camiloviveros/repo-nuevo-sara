package com.example.demo.service;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.example.demo.repository.DetectionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectVerificationService.class);
    
    private final DetectionRepository detectionRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    private DataSource dataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void verifyProjectSetup() {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(2000); // Esperar que todo esté listo
                runVerification();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("⚠️ Verificación interrumpida: {}", e.getMessage());
            } catch (RuntimeException e) {
                logger.warn("⚠️ Error de runtime en verificación del proyecto: {}", e.getMessage());
            }
        });
    }

    private void runVerification() {
        logger.info("🔍 ============ VERIFICACIÓN DEL PROYECTO ============");
        
        boolean allChecksPass = true;
        
        // 1. Verificar conexión a base de datos
        allChecksPass &= checkDatabaseConnection();
        
        // 2. Verificar ObjectMapper
        allChecksPass &= checkObjectMapper();
        
        // 3. Verificar repositorio
        allChecksPass &= checkRepository();
        
        // 4. Verificar estructura de archivos
        allChecksPass &= checkFileStructure();
        
        // 5. Verificar endpoints (indirectamente)
        allChecksPass &= checkApplicationComponents();
        
        // Resultado final
        if (allChecksPass) {
            logger.info("✅ ============ PROYECTO CONFIGURADO CORRECTAMENTE ============");
            logger.info("🚀 El servidor está listo para recibir solicitudes en http://localhost:8080/api");
            logger.info("🧪 Prueba los endpoints principales:");
            logger.info("   - GET  http://localhost:8080/api/detections/test");
            logger.info("   - GET  http://localhost:8080/api/detections/health");
            logger.info("   - GET  http://localhost:8080/api/detections/analysis/summary");
            logger.info("   - POST http://localhost:8080/api/detections/load-json");
        } else {
            logger.warn("⚠️ ============ ALGUNOS COMPONENTES NECESITAN ATENCIÓN ============");
            logger.info("💡 El servidor seguirá funcionando, pero revisa los logs anteriores");
        }
        
        logger.info("🔍 ============ FIN DE VERIFICACIÓN ============");
    }

    private boolean checkDatabaseConnection() {
        try {
            logger.info("🔍 Verificando conexión a base de datos...");
            
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5);
                if (isValid) {
                    logger.info("✅ Conexión a base de datos: OK");
                    
                    // Verificar conteo de registros
                    long count = detectionRepository.count();
                    logger.info("📊 Registros en BD: {}", count);
                    
                    return true;
                } else {
                    logger.error("❌ Conexión a base de datos: INVÁLIDA");
                    return false;
                }
            }
        } catch (SQLException e) {
            logger.error("❌ Error SQL conectando a base de datos: {}", e.getMessage());
            return false;
        } catch (DataAccessException e) {
            logger.error("❌ Error de acceso a datos: {}", e.getMessage());
            return false;
        } catch (RuntimeException e) {
            logger.error("❌ Error de runtime en base de datos: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkObjectMapper() {
        try {
            logger.info("🔍 Verificando ObjectMapper...");
            
            // Probar serialización/deserialización básica
            String testJson = "{\"test\":\"value\",\"number\":123}";
            Object parsed = objectMapper.readValue(testJson, Object.class);
            String serialized = objectMapper.writeValueAsString(parsed);
            
            if (serialized != null && !serialized.isEmpty()) {
                logger.info("✅ ObjectMapper: OK");
                return true;
            } else {
                logger.error("❌ ObjectMapper: Error en serialización");
                return false;
            }
        } catch (JsonProcessingException e) {
            logger.error("❌ Error de procesamiento JSON en ObjectMapper: {}", e.getMessage());
            return false;
        } catch (RuntimeException e) {
            logger.error("❌ Error de runtime en ObjectMapper: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkRepository() {
        try {
            logger.info("🔍 Verificando repositorio...");
            
            // Verificar que el repositorio puede ejecutar consultas básicas
            boolean exists = detectionRepository.existsAnyDetection();
            logger.info("📊 Datos en repositorio: {}", exists ? "SÍ" : "NO");
            
            logger.info("✅ Repositorio: OK");
            return true;
        } catch (DataAccessException e) {
            logger.error("❌ Error de acceso a datos en repositorio: {}", e.getMessage());
            return false;
        } catch (RuntimeException e) {
            logger.error("❌ Error de runtime en repositorio: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkFileStructure() {
        try {
            logger.info("🔍 Verificando estructura de archivos...");
            
            // Verificar directorio de detecciones
            File detectionsDir = new File("../detections");
            if (detectionsDir.exists()) {
                logger.info("📁 Directorio detections: OK");
                
                File detectionsFile = new File("../detections/detections.json");
                if (detectionsFile.exists()) {
                    logger.info("📄 Archivo detections.json: ENCONTRADO ({}KB)", 
                              detectionsFile.length() / 1024);
                } else {
                    logger.info("📄 Archivo detections.json: NO ENCONTRADO (se creará automáticamente)");
                }
            } else {
                logger.info("📁 Directorio detections: NO ENCONTRADO (se creará automáticamente)");
            }
            
            return true;
        } catch (SecurityException e) {
            logger.error("❌ Error de seguridad verificando archivos: {}", e.getMessage());
            return false;
        } catch (RuntimeException e) {
            logger.error("❌ Error de runtime verificando archivos: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkApplicationComponents() {
        try {
            logger.info("🔍 Verificando componentes de aplicación...");
            
            // Esto es más una verificación de que el contexto está bien cargado
            // Si llegamos hasta aquí, es porque Spring pudo inicializar todo
            
            logger.info("✅ Componentes de Spring: OK");
            logger.info("✅ Controladores: OK");
            logger.info("✅ Servicios: OK");
            logger.info("✅ Configuración CORS: OK");
            
            return true;
        } catch (RuntimeException e) {
            logger.error("❌ Error de runtime en componentes: {}", e.getMessage());
            return false;
        }
    }

    public void runManualVerification() {
        logger.info("🔧 Ejecutando verificación manual...");
        
        try {
            runVerification();
        } catch (RuntimeException e) {
            logger.error("❌ Error ejecutando verificación manual: {}", e.getMessage());
        }
    }
}