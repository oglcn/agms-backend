package com.agms.backend.dto;

import com.agms.backend.model.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {

    private String studentNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private double gpa;
    private int totalCredit;
    private int semester;
    private List<Course> courses;
    private boolean isCurriculumCompleted;

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