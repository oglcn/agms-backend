package com.agms.backend.service;

import com.agms.backend.dto.CreateStudentRequest;
import com.agms.backend.model.users.Student;
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

    Student updateStudent(String studentNumber, Student studentDetails);

    void deleteStudent(String studentNumber);

    /**
     * Student Queries
     */
    List<Student> getAllStudents();

    Optional<Student> getStudentByStudentNumber(String studentNumber);

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
