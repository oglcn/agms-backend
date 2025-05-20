package com.agms.backend.service;

import com.agms.backend.dto.CreateStudentRequest;
import com.agms.backend.entity.Student;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing student operations.
 */
public interface StudentService {
    /**
     * Student Creation and Profile Management
     */
    Student createStudent(CreateStudentRequest request);
    Student updateStudent(String studentId, Student studentDetails);
    void deleteStudent(String studentId);
    
    /**
     * Student Queries
     */
    List<Student> getAllStudents();
    Optional<Student> getStudentByStudentId(String studentId);
    
    /**
     * Graduation Management
     */
    void updateGraduationRequestStatus(String studentId, String status);
    
    /**
     * Advisor Assignment
     */
    void assignAdvisor(String studentId, String advisorId);
    void removeAdvisor(String studentId);
}
