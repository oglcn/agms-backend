package com.agms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudentResponse {
    
    private String studentNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String message;
    
} 