package com.agms.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.agms.backend.model.File;
import com.agms.backend.model.users.User;
import com.agms.backend.repository.FileRepository;
import com.agms.backend.dto.FileResponse;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.max-size:10485760}") // 10MB default
    private long maxFileSize;

    private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile("[^a-zA-Z0-9.-]");

    private final FileRepository fileRepository;

    public FileStorageService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload folder!");
        }
    }

    public String saveFile(MultipartFile file, User uploader) {
        validateFile(file);
        String originalFilename = file.getOriginalFilename();
        String sanitizedFilename = sanitizeFilename(originalFilename);
        String uniqueFilename = generateUniqueFilename(sanitizedFilename);

        try {
            // Save the physical file
            Path targetPath = Paths.get(uploadDir).resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Save file metadata to database
            File fileEntity = File.builder()
                .fileName(originalFilename)
                .fileType(file.getContentType())
                .uploadDate(LocalDate.now())
                .uploader(uploader)
                .filePath(uniqueFilename)
                .build();

            fileRepository.save(fileEntity);
            return uniqueFilename;
        } catch (IOException e) {
            throw new RuntimeException("Could not store the file: " + uniqueFilename, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds the maximum limit of 10MB");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        if (!filename.endsWith(".pdf") && !filename.endsWith(".png")) {
            throw new IllegalArgumentException("Only PDF and PNG files are allowed");
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "";
        // Remove invalid characters
        String sanitized = INVALID_FILENAME_CHARS.matcher(filename).replaceAll("_");
        // Ensure the filename isn't too long
        int maxLength = 255;
        if (sanitized.length() > maxLength) {
            int lastDot = sanitized.lastIndexOf(".");
            if (lastDot > 0) {
                sanitized = sanitized.substring(0, maxLength - (sanitized.length() - lastDot)) + 
                           sanitized.substring(lastDot);
            } else {
                sanitized = sanitized.substring(0, maxLength);
            }
        }
        return sanitized;
    }

    private String generateUniqueFilename(String originalFilename) {
        String extension = "";
        int lastDot = originalFilename.lastIndexOf(".");
        if (lastDot > 0) {
            extension = originalFilename.substring(lastDot);
        }
        return UUID.randomUUID().toString() + extension;
    }

    public Resource loadFile(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + filename);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error loading file: " + filename, e);
        }
    }

    public void deleteFile(String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Error deleting file: " + filename, e);
        }
    }

    public List<FileResponse> getFilesByUser(User user) {
        List<File> files = fileRepository.findByUploader(user);
        return files.stream()
            .map(file -> FileResponse.builder()
                .fileId(file.getFileId().toString())
                .fileName(file.getFileName())
                .fileType(file.getFileType())
                .uploadDate(file.getUploadDate())
                .uploaderName(file.getUploader().getFirstName() + " " + file.getUploader().getLastName())
                .downloadUrl("/api/files/download/" + file.getFilePath())
                .build())
            .collect(Collectors.toList());
    }
}
