package com.app.medicalquality.evidence.service;

import com.app.medicalquality.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(StorageProperties a_properties) {
        this.root = Path.of(a_properties.getPath()).toAbsolutePath();
    }

    public Stored store(MultipartFile a_file, String a_folder) {
        String storedName = UUID.randomUUID() + extension(a_file.getOriginalFilename());
        Path folder = root.resolve(a_folder);
        try {
            Files.createDirectories(folder);
            try (InputStream in = a_file.getInputStream()) {
                Files.copy(in, folder.resolve(storedName));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot store file", ex);
        }
        return new Stored(storedName, a_file.getOriginalFilename(), a_file.getContentType(), a_file.getSize());
    }

    public Resource load(String a_folder, String a_storedName) {
        return new FileSystemResource(root.resolve(a_folder).resolve(a_storedName));
    }

    public void delete(String a_folder, String a_storedName) {
        try {
            Files.deleteIfExists(root.resolve(a_folder).resolve(a_storedName));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot delete file", ex);
        }
    }

    public void move(String a_fromFolder, String a_toFolder, String a_storedName) {
        Path source = root.resolve(a_fromFolder).resolve(a_storedName);
        Path targetFolder = root.resolve(a_toFolder);
        try {
            Files.createDirectories(targetFolder);
            Files.move(source, targetFolder.resolve(a_storedName));
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot move file", ex);
        }
    }

    private String extension(String a_name) {
        if (a_name == null || !a_name.contains(".")) {
            return "";
        }
        return a_name.substring(a_name.lastIndexOf('.'));
    }

    public record Stored(String storedName, String originalName, String mimeType, long sizeBytes) {}
}
