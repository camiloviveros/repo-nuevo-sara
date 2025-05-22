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
    private static final long FILE_PROCESSING_DELAY_MS = 1000; // 1 segundo de espera
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, LocalDateTime> pendingFiles = new ConcurrentHashMap<>();
    private volatile boolean isWatching = true;

    @PostConstruct
    public void init() {
        // Cargar datos iniciales si existe el archivo
        try {
            String fullPath = DIRECTORY_PATH + "/" + FILE_NAME;
            if (new java.io.File(fullPath).exists()) {
                logger.info("Cargando datos iniciales desde: {}", fullPath);
                jsonLoader.loadJsonAndSaveToDb(fullPath);
            } else {
                logger.warn("Archivo de detecciones no encontrado: {}", fullPath);
            }
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido cargando datos iniciales";
            logger.error("Error cargando datos iniciales: {}", safeMessage);
        }
        
        // Iniciar monitoreo de archivos
        watchFileChanges();
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

    @Async
    public void watchFileChanges() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            Path path = Paths.get(DIRECTORY_PATH);
            
            // Crear el directorio si no existe
            if (!path.toFile().exists()) {
                logger.warn("Directorio no existe, creando: {}", path.toAbsolutePath());
                boolean created = path.toFile().mkdirs();
                if (!created) {
                    logger.error("❌ No se pudo crear el directorio: {}", path.toAbsolutePath());
                    return;
                }
            }
            
            path.register(watchService, 
                         StandardWatchEventKinds.ENTRY_CREATE, 
                         StandardWatchEventKinds.ENTRY_MODIFY);

            logger.info("🔍 Observando cambios en: {}", path.toAbsolutePath());

            while (isWatching) {
                WatchKey key;
                try {
                    // Usar poll con timeout en lugar de take() bloqueante
                    key = watchService.poll(1, TimeUnit.SECONDS);
                    if (key == null) {
                        continue; // Continuar el loop si no hay eventos
                    }
                } catch (InterruptedException e) {
                    logger.info("Monitoreo de archivos interrumpido.");
                    Thread.currentThread().interrupt();
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    // Ignorar eventos de overflow
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    Path changedFile = (Path) event.context();

                    if (changedFile.toString().equals(FILE_NAME)) {
                        logger.info("📄 Archivo {} {}", FILE_NAME, 
                                  (kind == StandardWatchEventKinds.ENTRY_MODIFY ? "modificado" : "creado"));

                        // Usar scheduler en lugar de Thread.sleep
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
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de E/O configurando monitoreo";
            logger.error("Error configurando el monitoreo de archivos: {}", safeMessage);
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido en monitoreo";
            logger.error("Error inesperado en el monitoreo de archivos: {}", safeMessage);
        }
        
        logger.info("🔍 Monitoreo de archivos finalizado");
    }

    /**
     * Programa el procesamiento de un archivo con retraso para evitar procesarlo
     * mientras aún se está escribiendo
     */
    private void scheduleFileProcessing(String filePath) {
        // Cancelar procesamiento previo si existe
        LocalDateTime now = LocalDateTime.now();
        pendingFiles.put(filePath, now);
        
        // Programar el procesamiento con retraso
        scheduler.schedule(() -> {
            try {
                // Verificar si este es el evento más reciente para este archivo
                LocalDateTime eventTime = pendingFiles.get(filePath);
                if (eventTime != null && eventTime.equals(now)) {
                    processFile(filePath);
                    pendingFiles.remove(filePath);
                } else {
                    logger.debug("🕒 Evento de archivo más reciente detectado, omitiendo procesamiento anterior");
                }
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                String safeMessage = errorMessage != null ? errorMessage : "Error desconocido en procesamiento programado";
                logger.error("❌ Error en procesamiento programado del archivo: {}", safeMessage);
                pendingFiles.remove(filePath);
            }
        }, FILE_PROCESSING_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Procesa el archivo JSON y actualiza la base de datos
     */
    private void processFile(String filePath) {
        try {
            logger.info("🔄 Procesando archivo: {}", filePath);
            
            // Verificar que el archivo existe y es legible
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                logger.warn("⚠️ El archivo ya no existe: {}", filePath);
                return;
            }
            
            if (!file.canRead()) {
                logger.warn("⚠️ No se puede leer el archivo: {}", filePath);
                return;
            }
            
            // Cargar y procesar el archivo
            jsonLoader.loadJsonAndSaveToDb(filePath);
            logger.info("✅ Datos actualizados en la base de datos desde: {}", filePath);
            
        } catch (IOException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de E/O procesando archivo";
            logger.error("❌ Error de E/O procesando archivo {}: {}", filePath, safeMessage);
            
            // Intentar reintento si es un error temporal
            if (isRetryableError(e)) {
                logger.info("🔄 Programando reintento para archivo: {}", filePath);
                scheduler.schedule(() -> processFile(filePath), 5, TimeUnit.SECONDS);
            }
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error de ejecución procesando archivo";
            logger.error("❌ Error de ejecución procesando archivo {}: {}", filePath, safeMessage);
            
            // Intentar reintento si es un error temporal
            if (isRetryableError(e)) {
                logger.info("🔄 Programando reintento para archivo: {}", filePath);
                scheduler.schedule(() -> processFile(filePath), 5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String safeMessage = errorMessage != null ? errorMessage : "Error desconocido procesando archivo";
            logger.error("❌ Error general procesando archivo {}: {}", filePath, safeMessage);
            
            // Intentar reintento si es un error temporal
            if (isRetryableError(e)) {
                logger.info("🔄 Programando reintento para archivo: {}", filePath);
                scheduler.schedule(() -> processFile(filePath), 5, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Determina si un error es temporal y vale la pena reintentar
     */
    private boolean isRetryableError(Exception e) {
        // Errores que típicamente son temporales
        if (e instanceof IOException) {
            return true;
        }
        
        // Verificación segura del mensaje de error
        String message = e.getMessage();
        if (message != null) {
            String lowerCaseMessage = message.toLowerCase();
            return lowerCaseMessage.contains("file is locked") ||
                   lowerCaseMessage.contains("access denied") ||
                   lowerCaseMessage.contains("being used by another process") ||
                   lowerCaseMessage.contains("resource temporarily unavailable") ||
                   lowerCaseMessage.contains("sharing violation");
        }
        
        // Si no hay mensaje, verificar por tipo de excepción
        return e instanceof java.nio.file.AccessDeniedException ||
               e instanceof java.nio.file.FileSystemException ||
               e instanceof java.io.FileNotFoundException;
    }

    /**
     * Método público para forzar el procesamiento inmediato del archivo
     */
    public void forceProcessFile() {
        String fullPath = DIRECTORY_PATH + "/" + FILE_NAME;
        logger.info("🔧 Forzando procesamiento inmediato de: {}", fullPath);
        processFile(fullPath);
    }

    /**
     * Método para verificar el estado del servicio
     */
    public boolean isWatching() {
        return isWatching && !scheduler.isShutdown();
    }

    /**
     * Método para obtener estadísticas del servicio
     */
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