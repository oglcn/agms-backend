package com.agms.backend.controller;

import com.agms.backend.model.FileUploadResponse;
import com.agms.backend.service.FileStorageService;
import com.agms.backend.dto.FileResponse;
import com.agms.backend.model.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Management", description = "APIs for file upload and download")
@SecurityRequirement(name = "bearerAuth")
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    @Autowired
    private FileStorageService fileStorageService;

    @Operation(summary = "Upload a file", description = "Upload a PDF or PNG file (max 10MB)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File uploaded successfully",
            content = @Content(schema = @Schema(implementation = FileUploadResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or file too large"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(description = "File to upload (PDF or PNG, max 10MB)") 
            @RequestParam("file") MultipartFile file) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            String filename = fileStorageService.saveFile(file, user);
            FileUploadResponse response = new FileUploadResponse(
                filename,
                "File uploaded successfully",
                "/api/files/download/" + filename
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload file");
        }
    }

    @Operation(summary = "Download a file", description = "Download a previously uploaded file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Name of the file to download") 
            @PathVariable String filename) {
        try {
            Resource file = fileStorageService.loadFile(filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(file);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + filename);
        }
    }

    @Operation(summary = "Delete a file", description = "Delete a previously uploaded file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "File deleted successfully"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{filename:.+}")
    public ResponseEntity<Void> deleteFile(
            @Parameter(description = "Name of the file to delete") 
            @PathVariable String filename) {
        log.info("Received delete request for file: {}", filename);
        try {
            fileStorageService.deleteFile(filename);
            log.info("Successfully deleted file: {}", filename);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("Failed to delete file {}: {}", filename, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete file: " + e.getMessage());
        }
    }

    @Operation(summary = "Get user's files", description = "Get all files uploaded by the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Files retrieved successfully",
            content = @Content(schema = @Schema(implementation = FileResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/my-files")
    public ResponseEntity<List<FileResponse>> getMyFiles() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            List<FileResponse> files = fileStorageService.getFilesByUser(user);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve files");
        }
    }
}

