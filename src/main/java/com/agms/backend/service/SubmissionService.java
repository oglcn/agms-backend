package com.agms.backend.service;

import com.agms.backend.dto.CreateSubmissionRequest;
import com.agms.backend.dto.SubmissionResponse;
import com.agms.backend.model.Submission;
import com.agms.backend.model.SubmissionStatus;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing submission operations.
 */
public interface SubmissionService {
    
    /**
     * Create a new graduation submission for a student
     */
    SubmissionResponse createGraduationSubmission(CreateSubmissionRequest request);
    
    /**
     * Get all submissions for a specific student
     */
    List<SubmissionResponse> getSubmissionsByStudent(String studentNumber);
    
    /**
     * Get all submissions for a specific advisor (via advisor list)
     */
    List<SubmissionResponse> getSubmissionsByAdvisor(String advisorEmpId);
    
    /**
     * Get a specific submission by ID
     */
    Optional<SubmissionResponse> getSubmissionById(String submissionId);
    
    /**
     * Update submission status (for advisors to approve/reject)
     */
    SubmissionResponse updateSubmissionStatus(String submissionId, SubmissionStatus status);
    
    /**
     * Get all submissions with a specific status
     */
    List<SubmissionResponse> getSubmissionsByStatus(SubmissionStatus status);
    
    /**
     * Check if a student has an active pending submission
     */
    boolean hasActivePendingSubmission(String studentNumber);
    
    /**
     * Get the latest submission for a student
     */
    Optional<SubmissionResponse> getLatestSubmissionByStudent(String studentNumber);
    
    /**
     * Delete a submission (if needed for administrative purposes)
     */
    void deleteSubmission(String submissionId);
    
    // Workflow methods for each role

    SubmissionResponse updateSubmissionStatusByAdvisor(String submissionId, SubmissionStatus status);
    
    List<SubmissionResponse> getSubmissionsByDepartmentSecretary(String deptSecretaryEmpId);
    SubmissionResponse updateSubmissionStatusByDepartmentSecretary(String submissionId, SubmissionStatus status);
    
    List<SubmissionResponse> getSubmissionsByDeanOfficer(String deanOfficerEmpId);
    SubmissionResponse updateSubmissionStatusByDeanOfficer(String submissionId, SubmissionStatus status);
    
    List<SubmissionResponse> getSubmissionsByStudentAffairs(String studentAffairsEmpId);
    SubmissionResponse updateSubmissionStatusByStudentAffairs(String submissionId, SubmissionStatus status);
    
    // Helper method to get submissions pending for a specific role
    List<SubmissionResponse> getSubmissionsPendingForRole(String empId, String role);
} 