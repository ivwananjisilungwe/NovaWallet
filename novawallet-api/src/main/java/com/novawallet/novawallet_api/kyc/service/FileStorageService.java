package com.novawallet.novawallet_api.kyc.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path uploadDir;

    public FileStorageService(@Value("${app.kyc.upload-dir:uploads/kyc}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadDir);
            log.info("KYC upload directory: {}", uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create KYC upload directory: " + uploadDir, e);
        }
    }

    public String storeFile(MultipartFile file, UUID userId, UUID documentId) {
        String userDir = userId.toString();
        String fileName = documentId.toString() + "_" + sanitizeFileName(file.getOriginalFilename());

        Path targetDir = uploadDir.resolve(userDir);
        try {
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored KYC document {} for user {} at {}", documentId, userId, targetPath);
            return targetPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store KYC document: " + fileName, e);
        }
    }

    public byte[] loadFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!path.normalize().startsWith(uploadDir)) {
                throw new SecurityException("Access denied: file outside upload directory");
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read KYC document: " + filePath, e);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
            log.info("Deleted KYC document: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete KYC document: {}", filePath);
        }
    }

    private String sanitizeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) return "unnamed";
        return originalName.replaceAll("[^a-zA-Z0-9\\.\\-_]", "_");
    }
}
