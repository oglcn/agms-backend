package com.agms.backend.dto;

import com.agms.backend.model.users.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfileResponse {
    private String studentNumber;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private String department;
    private String faculty;
    private AdvisorInfo advisor;
    private Double gpa;
    private Integer totalCredits;
    private Integer semester;

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