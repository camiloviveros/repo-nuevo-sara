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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, LocalDateTime> pendingFiles = new ConcurrentHashMap<>();
    private volatile boolean isWatching = false;

    /**
     * Se ejecuta DESPUÉS de que la aplicación esté completamente inicializada
     * Esto evita problemas de dependencias circulares o beans no inicializados
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        logger.info("🚀 Aplicación completamente iniciada. Configurando FileWatcherService...");
        
        // Dar tiempo extra para que todo esté listo
        scheduler.schedule(() -> {
            try {
                startFileWatching();
            } catch (Exception e) {
                logger.warn("⚠️ Error iniciando FileWatcher (no crítico): {}", e.getMessage());
            }
        }, 3, TimeUnit.SECONDS);
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
            logger.info("✅ FileWatcherService cerrado correctamente");
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void startFileWatching() {
        try {
            // Primero intentar cargar datos existentes
            loadInitialDataSafely();
            
            // Luego iniciar el monitoreo
            startWatchingAsync();
            
            logger.info("✅ FileWatcherService iniciado correctamente");
        } catch (Exception e) {
            logger.warn("⚠️ Error en startFileWatching: {}", e.getMessage());
        }
    }

    private void loadInitialDataSafely() {
        try {
            String fullPath = DIRECTORY_PATH + "/" + FILE_NAME;
            java.io.File file = new java.io.File(fullPath);
            
            if (file.exists() && file.canRead() && file.length() > 0) {
                logger.info("📂 Cargando datos iniciales desde: {}", fullPath);
                jsonLoader.loadJsonAndSaveToDbSafely(fullPath);
                logger.info("✅ Datos iniciales cargados exitosamente");
            } else {
                logger.info("📂 No se encontraron datos iniciales en: {}", fullPath);
            }
        } catch (Exception e) {
            logger.warn("⚠️ Error cargando datos iniciales: {}", e.getMessage());
        }
    }

    @Async("taskExecutor")
    public void startWatchingAsync() {
        if (isWatching) {
            logger.info("🔍 FileWatcher ya está ejecutándose");
            return;
        }

        isWatching = true;
        logger.info("🔍 Iniciando monitoreo de archivos...");

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            Path path = Paths.get(DIRECTORY_PATH);
            
            // Crear directorio si no existe
            if (!path.toFile().exists()) {
                logger.info("📁 Creando directorio: {}", path.toAbsolutePath());
                path.toFile().mkdirs();
            }
            
            // Registrar eventos de watch
            path.register(watchService, 
                         StandardWatchEventKinds.ENTRY_CREATE, 
                         StandardWatchEventKinds.ENTRY_MODIFY);

            logger.info("🔍 Monitoreando: {}", path.toAbsolutePath());

            // Loop principal de monitoreo
            watchLoop(watchService);

        } catch (IOException e) {
            logger.error("❌ Error configurando monitoreo de archivos: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Error inesperado en monitoreo: {}", e.getMessage());
        } finally {
            isWatching = false;
            logger.info("🔍 Monitoreo de archivos finalizado");
        }
    }

    private void watchLoop(WatchService watchService) {
        while (isWatching) {
            WatchKey key;
            try {
                // Esperar por eventos con timeout
                key = watchService.poll(2, TimeUnit.SECONDS);
                if (key == null) {
                    continue; // No hay eventos, continúa el loop
                }
            } catch (InterruptedException e) {
                logger.info("⏸️ Monitoreo interrumpido");
                Thread.currentThread().interrupt();
                break;
            }

            // Procesar eventos
            processWatchEvents(key);

            // Reset del key
            boolean valid = key.reset();
            if (!valid) {
                logger.warn("⚠️ WatchKey inválido. Finalizando monitoreo.");
                break;
            }
        }
    }

    private void processWatchEvents(WatchKey key) {
        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();
            
            if (kind == StandardWatchEventKinds.OVERFLOW) {
                continue;
            }
            
            Path changedFile = (Path) event.context();

            if (changedFile.toString().equals(FILE_NAME)) {
                String eventType = (kind == StandardWatchEventKinds.ENTRY_MODIFY) ? "modificado" : "creado";
                logger.info("📄 Archivo {} {}", FILE_NAME, eventType);

                scheduleFileProcessing(DIRECTORY_PATH + "/" + FILE_NAME);
            }
        }
    }

    private void scheduleFileProcessing(String filePath) {
        LocalDateTime now = LocalDateTime.now();
        pendingFiles.put(filePath, now);
        
        // Programar procesamiento con delay para evitar múltiples procesamiento del mismo archivo
        scheduler.schedule(() -> {
            try {
                LocalDateTime eventTime = pendingFiles.get(filePath);
                if (eventTime != null && eventTime.equals(now)) {
                    processFile(filePath);
                    pendingFiles.remove(filePath);
                }
            } catch (Exception e) {
                logger.error("❌ Error en procesamiento programado: {}", e.getMessage());
                pendingFiles.remove(filePath);
            }
        }, FILE_PROCESSING_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void processFile(String filePath) {
        try {
            logger.info("🔄 Procesando archivo: {}", filePath);
            
            java.io.File file = new java.io.File(filePath);
            if (!file.exists() || !file.canRead()) {
                logger.warn("⚠️ Archivo no disponible: {}", filePath);
                return;
            }
            
            if (file.length() == 0) {
                logger.warn("⚠️ Archivo vacío: {}", filePath);
                return;
            }
            
            jsonLoader.loadJsonAndSaveToDbSafely(filePath);
            logger.info("✅ Archivo procesado exitosamente: {}", filePath);
            
        } catch (Exception e) {
            logger.error("❌ Error procesando archivo {}: {}", filePath, e.getMessage());
        }
    }

    // Métodos públicos para control manual
    public void forceProcessFile() {
        String fullPath = DIRECTORY_PATH + "/" + FILE_NAME;
        logger.info("🔧 Procesamiento manual de: {}", fullPath);
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
        status.put("lastCheck", LocalDateTime.now().toString());
        return status;
    }

    public void restartWatching() {
        logger.info("🔄 Reiniciando FileWatcher...");
        if (isWatching) {
            isWatching = false;
            // Esperar a que termine el loop actual
            scheduler.schedule(() -> startWatchingAsync(), 2, TimeUnit.SECONDS);
        } else {
            startWatchingAsync();
        }
    }
}