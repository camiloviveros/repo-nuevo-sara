package com.example.demo.service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileWatcherService {

    private static final Logger logger = LoggerFactory.getLogger(FileWatcherService.class);
    private final JsonLoader jsonLoader;

    private static final String DIRECTORY_PATH = "../detections";  
    private static final String FILE_NAME = "detections.json";
    private static final long FILE_PROCESSING_DELAY_MS = 1000;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, LocalDateTime> pendingFiles = new ConcurrentHashMap<>();
    private volatile boolean isWatching = true;

    @PostConstruct
    public void init() {
        logger.info("🚀 Iniciando FileWatcherService...");
        
        // NO cargar datos iniciales para evitar que falle el arranque
        // Solo iniciar el monitoreo de archivos de forma asíncrona
        scheduler.schedule(this::startWatching, 5, TimeUnit.SECONDS);
        
        logger.info("✅ FileWatcherService iniciado correctamente");
    }

    @PreDestroy
    public void shutdown() {
        logger.info("🛑 Cerrando FileWatcherService...");
        isWatching = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void startWatching() {
        try {
            // Intentar cargar datos iniciales DESPUÉS del arranque
            loadInitialDataSafely();
            
            // Iniciar monitoreo de archivos
            watchFileChanges();
        } catch (Exception e) {
            logger.warn("⚠️ Error iniciando monitoreo de archivos: {}", e.getMessage());
        }
    }

    private void loadInitialDataSafely() {
        try {
            String fullPath = DIRECTORY_PATH + "/" + FILE_NAME;
            java.io.File file = new java.io.File(fullPath);
            
            if (file.exists() && file.canRead()) {
                logger.info("📂 Archivo de detecciones encontrado: {}", fullPath);
                jsonLoader.loadJsonAndSaveToDbSafely(fullPath);
            } else {
                logger.info("📂 Archivo de detecciones no encontrado o no legible: {}", fullPath);
            }
        } catch (Exception e) {
            logger.warn("⚠️ Error cargando datos iniciales (continuando con el arranque): {}", e.getMessage());
        }
    }

    @Async
    public void watchFileChanges() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            Path path = Paths.get(DIRECTORY_PATH);
            
            if (!path.toFile().exists()) {
                logger.info("📁 Creando directorio: {}", path.toAbsolutePath());
                path.toFile().mkdirs();
            }
            
            path.register(watchService, 
                         StandardWatchEventKinds.ENTRY_CREATE, 
                         StandardWatchEventKinds.ENTRY_MODIFY);

            logger.info("🔍 Observando cambios en: {}", path.toAbsolutePath());

            while (isWatching) {
                WatchKey key;
                try {
                    key = watchService.poll(1, TimeUnit.SECONDS);
                    if (key == null) {
                        continue;
                    }
                } catch (InterruptedException e) {
                    logger.info("Monitoreo de archivos interrumpido.");
                    Thread.currentThread().interrupt();
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    Path changedFile = (Path) event.context();

                    if (changedFile.toString().equals(FILE_NAME)) {
                        logger.info("📄 Archivo {} {}", FILE_NAME, 
                                  (kind == StandardWatchEventKinds.ENTRY_MODIFY ? "modificado" : "creado"));

                        scheduleFileProcessing(DIRECTORY_PATH + "/" + FILE_NAME);
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    logger.warn("WatchKey inválido. Terminando observación.");
                    break;
                }
            }

        } catch (IOException e) {
            logger.error("Error configurando el monitoreo de archivos: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado en el monitoreo de archivos: {}", e.getMessage());
        }
        
        logger.info("🔍 Monitoreo de archivos finalizado");
    }

    private void scheduleFileProcessing(String filePath) {
        LocalDateTime now = LocalDateTime.now();
        pendingFiles.put(filePath, now);
        
        scheduler.schedule(() -> {
            try {
                LocalDateTime eventTime = pendingFiles.get(filePath);
                if (eventTime != null && eventTime.equals(now)) {
                    processFile(filePath);
                    pendingFiles.remove(filePath);
                }
            } catch (Exception e) {
                logger.error("❌ Error en procesamiento programado del archivo: {}", e.getMessage());
                pendingFiles.remove(filePath);
            }
        }, FILE_PROCESSING_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void processFile(String filePath) {
        try {
            logger.info("🔄 Procesando archivo: {}", filePath);
            
            java.io.File file = new java.io.File(filePath);
            if (!file.exists() || !file.canRead()) {
                logger.warn("⚠️ El archivo no existe o no es legible: {}", filePath);
                return;
            }
            
            jsonLoader.loadJsonAndSaveToDbSafely(filePath);
            logger.info("✅ Datos actualizados en la base de datos desde: {}", filePath);
            
        } catch (Exception e) {
            logger.error("❌ Error procesando archivo {}: {}", filePath, e.getMessage());
        }
    }

    public void forceProcessFile() {
        String fullPath = DIRECTORY_PATH + "/" + FILE_NAME;
        logger.info("🔧 Forzando procesamiento inmediato de: {}", fullPath);
        processFile(fullPath);
    }

    public boolean isWatching() {
        return isWatching && !scheduler.isShutdown();
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        status.put("isWatching", isWatching());
        status.put("directoryPath", DIRECTORY_PATH);
        status.put("fileName", FILE_NAME);
        status.put("pendingFiles", pendingFiles.size());
        status.put("schedulerActive", !scheduler.isShutdown());
        return status;
    }
}