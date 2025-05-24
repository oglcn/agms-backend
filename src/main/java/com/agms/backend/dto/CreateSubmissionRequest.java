package com.agms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubmissionRequest {
    
    @NotBlank(message = "Student number is required")
    private String studentNumber;
    
    private String content;
    
    // Optional: if we want to allow specifying submission type in the future
    private String submissionType;
} 