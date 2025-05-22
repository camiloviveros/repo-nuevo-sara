package com.example.demo.service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileWatcherService {

    private static final Logger logger = LoggerFactory.getLogger(FileWatcherService.class);
    private final JsonLoader jsonLoader;

    private static final String DIRECTORY_PATH = "../detections";  
    private static final String FILE_NAME = "detections.json";  

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
            logger.error("Error cargando datos iniciales: {}", e.getMessage());
        }
        
        // Iniciar monitoreo de archivos
        watchFileChanges();
    }

    @Async
    public void watchFileChanges() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(DIRECTORY_PATH);
            
            // Crear el directorio si no existe
            if (!path.toFile().exists()) {
                logger.warn("Directorio no existe, creando: {}", path.toAbsolutePath());
                path.toFile().mkdirs();
            }
            
            path.register(watchService, 
                         StandardWatchEventKinds.ENTRY_CREATE, 
                         StandardWatchEventKinds.ENTRY_MODIFY);

            logger.info("🔍 Observando cambios en: {}", path.toAbsolutePath());

            while (true) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException e) {
                    logger.info("Monitoreo de archivos interrumpido.");
                    Thread.currentThread().interrupt();
                    break;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path changedFile = (Path) event.context();

                    if (changedFile.toString().equals(FILE_NAME)) {
                        logger.info("📄 Archivo {} {}", FILE_NAME, 
                                  (kind == StandardWatchEventKinds.ENTRY_MODIFY ? "modificado" : "creado"));

                        try {
                            // Esperar un momento para asegurar que el archivo se haya terminado de escribir
                            Thread.sleep(1000);
                            
                            jsonLoader.loadJsonAndSaveToDb(DIRECTORY_PATH + "/" + FILE_NAME);
                            logger.info("✅ Datos actualizados en la base de datos");
                        } catch (Exception e) {
                            logger.error("❌ Error procesando archivo actualizado: {}", e.getMessage());
                        }
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
    }
}