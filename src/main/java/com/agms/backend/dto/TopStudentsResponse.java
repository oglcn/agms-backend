package com.agms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopStudentsResponse {
    
    // Top 3 students overall (for the user's scope)
    private List<TopStudentInfo> topStudents;
    
    // Top 3 students from each department (for Dean Officer and Student Affairs)
    private List<TopDepartmentInfo> topStudentsFromDepartments;
    
    // Top 3 students from each faculty (for Student Affairs only)
    private List<TopFacultyInfo> topStudentsFromFaculties;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopStudentInfo {
        private String studentNumber;
        private String firstName;
        private String lastName;
        private String email;
        private String department;
        private String faculty;
        private Double gpa;
        private Integer totalCredits;
        private Integer semester;
        private Integer rank;
        private String advisorName;
        private String advisorEmpId;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopDepartmentInfo {
        private String departmentName;
        private String faculty;
        private Double averageGpa;
        private Integer totalStudents;
        private Integer rank;
        private List<TopStudentInfo> topStudents;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopFacultyInfo {
        private String facultyName;
        private Double averageGpa;
        private Integer totalStudents;
        private Integer totalDepartments;
        private Integer rank;
        private List<TopDepartmentInfo> topStudentsFromDepartments;
    }
} 