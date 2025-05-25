package com.agms.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.agms.backend.model.File;
import com.agms.backend.model.users.User;
import com.agms.backend.repository.FileRepository;
import com.agms.backend.dto.FileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.agms.backend.model.Submission;
import com.agms.backend.repository.SubmissionRepository;
import com.agms.backend.model.users.*;
import com.agms.backend.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.max-size:10485760}") // 10MB default
    private long maxFileSize;

    private static final Pattern INVALID_FILENAME_CHARS = Pattern.compile("[^a-zA-Z0-9.-]");

    private final FileRepository fileRepository;
    private final SubmissionRepository submissionRepository;

    public FileStorageService(FileRepository fileRepository, SubmissionRepository submissionRepository) {
        this.fileRepository = fileRepository;
        this.submissionRepository = submissionRepository;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload folder!");
        }
    }

    public String saveFile(MultipartFile file, User uploader, String submissionId) {
        validateFile(file);
        String originalFilename = file.getOriginalFilename();
        String sanitizedFilename = sanitizeFilename(originalFilename);
        String uniqueFilename = generateUniqueFilename(sanitizedFilename);

        try {
            // Get the submission
            Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + submissionId));

            // Check if user has access to this submission
            if (!hasAccessToSubmission(submission, uploader)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to this submission");
            }

            // Save the physical file
            Path targetPath = Paths.get(uploadDir).resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Save file metadata to database
            File fileEntity = File.builder()
                .fileName(originalFilename)
                .fileType(file.getContentType())
                .uploadDate(new Timestamp(System.currentTimeMillis()))
                .uploader(uploader)
                .filePath(uniqueFilename)
                .submission(submission)
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

    @Transactional
    public void deleteFile(String filename) {
        log.info("Starting file deletion process for: {}", filename);
        
        try {
            // First, find the file in the database
            File fileEntity = fileRepository.findByFilePath(filename)
                .orElseThrow(() -> new RuntimeException("File not found in database: " + filename));
            
            log.info("Found file in database with ID: {}", fileEntity.getFileId());
            
            // Delete the physical file
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            boolean fileDeleted = Files.deleteIfExists(filePath);
            log.info("Physical file deletion result: {}", fileDeleted ? "success" : "file not found");
            
            // Delete the database record
            fileRepository.deleteById(fileEntity.getFileId());
            log.info("Database record deleted successfully for file ID: {}", fileEntity.getFileId());
            
            // Verify deletion
            boolean stillExists = fileRepository.existsById(fileEntity.getFileId());
            if (stillExists) {
                log.error("Database record still exists after deletion for file ID: {}", fileEntity.getFileId());
                throw new RuntimeException("Failed to delete database record");
            }
            log.info("Verified database record deletion for file ID: {}", fileEntity.getFileId());
            
        } catch (IOException e) {
            log.error("Error deleting physical file: {}", e.getMessage());
            throw new RuntimeException("Error deleting physical file: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error during file deletion process: {}", e.getMessage());
            throw new RuntimeException("Error during file deletion: " + e.getMessage());
        }
    }

    public List<FileResponse> getFilesBySubmission(String submissionId, User user) {
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + submissionId));

        // Check if user has access to this submission
        if (!hasAccessToSubmission(submission, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to this submission");
        }

        List<File> files = submission.getFiles();
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

    @Transactional
    public void deleteFileFromSubmission(String submissionId, String filename, User user) {
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + submissionId));

        // Check if user has access to this submission
        if (!hasAccessToSubmission(submission, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to this submission");
        }

        // Find the file in the submission
        File fileToDelete = submission.getFiles().stream()
            .filter(file -> file.getFilePath().equals(filename))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("File not found in submission: " + filename));

        // Check if user has permission to delete this file
        if (!canDeleteFile(fileToDelete, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to delete this file");
        }

        // Delete the file
        deleteFile(filename);
    }

    private boolean hasAccessToSubmission(Submission submission, User user) {
        if (user instanceof Student) {
            // Students can access their own submissions
            return submission.getStudent().getStudentNumber().equals(((Student) user).getStudentNumber());
        } else if (user instanceof Advisor) {
            // Advisors can access submissions they are advising
            return submission.getAdvisorList() != null && 
                   submission.getAdvisorList().getAdvisor().equals(user);
        } else if (user instanceof DepartmentSecretary) {
            // Department secretaries can access submissions from their department
            return submission.getAdvisorList() != null && 
                   submission.getAdvisorList().getDepartmentList() != null &&
                   submission.getAdvisorList().getDepartmentList().getSecretary().equals(user);
        } else if (user instanceof DeanOfficer || user instanceof StudentAffairs) {
            // Dean officers and student affairs can access all submissions
            return true;
        }
        return false;
    }

    private boolean canDeleteFile(File file, User user) {
        // All users with access to the submission can delete files
        // This is because files are now submission attachments rather than student files
        return hasAccessToSubmission(file.getSubmission(), user);
    }
}
