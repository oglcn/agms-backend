package com.agms.backend.service;

import com.agms.backend.dto.CreateStudentRequest;
import com.agms.backend.dto.CreateStudentResponse;
import com.agms.backend.dto.StudentProfileResponse;
import com.agms.backend.dto.StudentResponse;
import com.agms.backend.model.users.Student;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing student operations.
 * All public methods return safe DTOs without sensitive information.
 */
public interface StudentService {
    /**
     * Student Creation and Profile Management
     */
    CreateStudentResponse createStudent(CreateStudentRequest request);

    StudentResponse updateStudent(String studentNumber, Student studentDetails);

    void deleteStudent(String studentNumber);

    /**
     * Student Queries
     */
    List<StudentResponse> getAllStudents();

    Optional<StudentResponse> getStudentByStudentNumber(String studentNumber);

    /**
     * Get student profile information by email (for authenticated user)
     */
    StudentProfileResponse getStudentProfileByEmail(String email);

    /**
     * Graduation Management
     */
    void updateGraduationRequestStatus(String studentNumber, String status);

    /**
     * Advisor Assignment
     */
    void assignAdvisor(String studentNumber, String advisorId);

    void removeAdvisor(String studentNumber);
}
