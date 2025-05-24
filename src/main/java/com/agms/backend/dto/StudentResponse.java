package com.agms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    
    private String studentNumber;
    private String firstName;
    private String lastName;
    private String email;
    private double gpa;
    private int totalCredit;
    private int semester;
    
    // Advisor info (without sensitive data)
    private AdvisorInfo advisor;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdvisorInfo {
        private String empId;
        private String firstName;
        private String lastName;
        private String email;
    }
} 