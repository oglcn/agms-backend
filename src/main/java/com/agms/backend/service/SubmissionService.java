package com.agms.backend.service;

import com.agms.backend.dto.CreateSubmissionRequest;
import com.agms.backend.dto.RegularGraduationTrackResponse;
import com.agms.backend.dto.SubmissionResponse;
import com.agms.backend.dto.SubordinateStatusResponse;
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

    SubmissionResponse updateSubmissionStatusByAdvisor(String submissionId, SubmissionStatus status, String rejectionReason);
    
    List<SubmissionResponse> getSubmissionsByDepartmentSecretary(String deptSecretaryEmpId);
    SubmissionResponse updateSubmissionStatusByDepartmentSecretary(String submissionId, SubmissionStatus status, String rejectionReason);
    
    List<SubmissionResponse> getSubmissionsByDeanOfficer(String deanOfficerEmpId);
    SubmissionResponse updateSubmissionStatusByDeanOfficer(String submissionId, SubmissionStatus status, String rejectionReason);
    
    /**
     * Get submissions for student affairs
     */
    List<SubmissionResponse> getSubmissionsByStudentAffairs(String studentAffairsEmpId);
    SubmissionResponse updateSubmissionStatusByStudentAffairs(String submissionId, SubmissionStatus status, String rejectionReason);
    
    // Helper method to get submissions pending for a specific role
    List<SubmissionResponse> getSubmissionsPendingForRole(String empId, String role);

    /**
     * Role-agnostic approval - automatically determines correct status based on authenticated user's role
     */
    SubmissionResponse approveSubmission(String submissionId);

    /**
     * Role-agnostic rejection - automatically determines correct status based on authenticated user's role
     */
    SubmissionResponse rejectSubmission(String submissionId, String rejectionReason);

    /**
     * Get all submissions for the current authenticated user (role-agnostic)
     * - STUDENT: returns their own submissions
     * - ADVISOR: returns all submissions assigned to them
     * - DEPARTMENT_SECRETARY: returns all submissions under their department
     * - DEAN_OFFICER: returns all submissions under their faculty
     * - STUDENT_AFFAIRS: returns all submissions in the system
     */
    List<SubmissionResponse> getMySubmissions();

    /**
     * Get pending submissions for the current authenticated user (role-agnostic)
     * - ADVISOR: returns submissions with status PENDING
     * - DEPARTMENT_SECRETARY: returns submissions with status APPROVED_BY_ADVISOR
     * - DEAN_OFFICER: returns submissions with status APPROVED_BY_DEPT
     * - STUDENT_AFFAIRS: returns submissions with status APPROVED_BY_DEAN
     */
    List<SubmissionResponse> getMyPendingSubmissions();

    /**
     * Start regular graduation process - creates submissions for all eligible students
     * This method is called by Student Affairs to initiate the regular graduation workflow
     */
    List<SubmissionResponse> startRegularGraduation(String term);

    /**
     * Track regular graduation process status for a specific term
     * Returns information about whether regular graduation has been started and its current status
     */
    RegularGraduationTrackResponse trackRegularGraduation(String term);

    // List finalization methods
    
    /**
     * Finalize list for current authenticated user
     * - ADVISOR: finalizes their advisor list
     * - DEPARTMENT_SECRETARY: finalizes their department list 
     * - DEAN_OFFICER: finalizes their faculty list
     * - STUDENT_AFFAIRS: finalizes graduation list and creates graduation object
     */
    boolean finalizeMyList();
    
    /**
     * Check if current user's list is finalized
     */
    boolean isMyListFinalized();
    
    /**
     * Check if all prerequisite lists are finalized for current user's role
     */
    boolean arePrerequisiteListsFinalized();
    
    /**
     * Get finalization status of subordinates for current authenticated user
     * - DEPARTMENT_SECRETARY: returns finalization status of advisors under their department
     * - DEAN_OFFICER: returns finalization status of department secretaries under their faculty
     * - STUDENT_AFFAIRS: returns finalization status of dean officers
     */
    List<SubordinateStatusResponse> getSubordinateFinalizationStatus();
} 