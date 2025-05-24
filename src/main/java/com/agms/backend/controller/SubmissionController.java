package com.agms.backend.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.agms.backend.dto.CreateSubmissionRequest;
import com.agms.backend.dto.SubmissionResponse;
import com.agms.backend.model.SubmissionStatus;
import com.agms.backend.service.SubmissionService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Validated
public class SubmissionController {

    private final SubmissionService submissionService;

    /**
     * Create a new graduation submission (for students)
     */
    @PostMapping("/graduation")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Create a new graduation submission for a student")
    public ResponseEntity<SubmissionResponse> createGraduationSubmission(
            @Valid @RequestBody CreateSubmissionRequest request) {
        log.info("Creating graduation submission for student: {}", request.getStudentNumber());
        
        try {
            SubmissionResponse response = submissionService.createGraduationSubmission(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            log.warn("Cannot create submission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error creating submission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all submissions for a student
     */
    @GetMapping("/student/{studentNumber}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADVISOR') or hasRole('DEPARTMENT_SECRETARY')")
    @Operation(summary = "Get all submissions for a specific student")
    public ResponseEntity<List<SubmissionResponse>> getSubmissionsByStudent(@PathVariable String studentNumber) {
        log.debug("Getting submissions for student: {}", studentNumber);
        
        List<SubmissionResponse> submissions = submissionService.getSubmissionsByStudent(studentNumber);
        return ResponseEntity.ok(submissions);
    }

    /**
     * Get all submissions for an advisor
     */
    @GetMapping("/advisor/{advisorEmpId}")
    @PreAuthorize("hasRole('ADVISOR')")
    @Operation(summary = "Get all submissions for a specific advisor")
    public ResponseEntity<List<SubmissionResponse>> getSubmissionsByAdvisor(@PathVariable String advisorEmpId) {
        log.debug("Getting submissions for advisor: {}", advisorEmpId);
        
        List<SubmissionResponse> submissions = submissionService.getSubmissionsByAdvisor(advisorEmpId);
        return ResponseEntity.ok(submissions);
    }

    /**
     * Get a specific submission by ID
     */
    @GetMapping("/{submissionId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADVISOR') or hasRole('DEPARTMENT_SECRETARY')")
    @Operation(summary = "Get a specific submission by its ID")
    public ResponseEntity<SubmissionResponse> getSubmissionById(@PathVariable String submissionId) {
        log.debug("Getting submission by ID: {}", submissionId);
        
        Optional<SubmissionResponse> submission = submissionService.getSubmissionById(submissionId);
        return submission.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get submissions by status
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADVISOR') or hasRole('DEPARTMENT_SECRETARY') or hasRole('DEAN_OFFICER')")
    @Operation(summary = "Get all submissions filtered by a specific status")
    public ResponseEntity<List<SubmissionResponse>> getSubmissionsByStatus(@PathVariable SubmissionStatus status) {
        log.debug("Getting submissions with status: {}", status);
        
        List<SubmissionResponse> submissions = submissionService.getSubmissionsByStatus(status);
        return ResponseEntity.ok(submissions);
    }

    /**
     * Check if student has active pending submission
     */
    @GetMapping("/student/{studentNumber}/has-pending")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADVISOR')")
    @Operation(summary = "Check if a student has any active pending submissions")
    public ResponseEntity<Boolean> hasActivePendingSubmission(@PathVariable String studentNumber) {
        log.debug("Checking if student {} has active pending submission", studentNumber);
        
        boolean hasPending = submissionService.hasActivePendingSubmission(studentNumber);
        return ResponseEntity.ok(hasPending);
    }

    /**
     * Get latest submission for a student
     */
    @GetMapping("/student/{studentNumber}/latest")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADVISOR')")
    @Operation(summary = "Get the latest submission for a specific student")
    public ResponseEntity<SubmissionResponse> getLatestSubmissionByStudent(@PathVariable String studentNumber) {
        log.debug("Getting latest submission for student: {}", studentNumber);
        
        Optional<SubmissionResponse> submission = submissionService.getLatestSubmissionByStudent(studentNumber);
        return submission.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a submission (admin operation)
     */
    @DeleteMapping("/{submissionId}")
    @PreAuthorize("hasRole('DEPARTMENT_SECRETARY') or hasRole('DEAN_OFFICER')")
    @Operation(summary = "Delete a specific submission (admin operation)")
    public ResponseEntity<Void> deleteSubmission(@PathVariable String submissionId) {
        log.info("Deleting submission: {}", submissionId);
        
        try {
            submissionService.deleteSubmission(submissionId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting submission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get submissions pending for advisor review
     */
    @GetMapping("/advisor/{advisorEmpId}/pending")
    @PreAuthorize("hasRole('ADVISOR')")
    @Operation(summary = "Get submissions pending for advisor review")
    public ResponseEntity<List<SubmissionResponse>> getPendingSubmissionsForAdvisor(@PathVariable String advisorEmpId) {
        log.debug("Getting pending submissions for advisor: {}", advisorEmpId);
        List<SubmissionResponse> submissions = submissionService.getSubmissionsPendingForRole(advisorEmpId, "ADVISOR");
        return ResponseEntity.ok(submissions);
    }

    /**
     * Update submission status by advisor
     */
    @PutMapping("/{submissionId}/advisor-review")
    @PreAuthorize("hasRole('ADVISOR')")
    @Operation(summary = "Update submission status by advisor")
    public ResponseEntity<SubmissionResponse> reviewSubmissionByAdvisor(
            @PathVariable String submissionId,
            @RequestParam SubmissionStatus status) {
        log.info("Advisor reviewing submission {} with status: {}", submissionId, status);
        
        try {
            SubmissionResponse response = submissionService.updateSubmissionStatusByAdvisor(submissionId, status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating submission status by advisor: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get submissions pending for department secretary review
     */
    @GetMapping("/department-secretary/{deptSecretaryEmpId}/pending")
    @PreAuthorize("hasRole('DEPARTMENT_SECRETARY')")
    @Operation(summary = "Get submissions pending for department secretary review")
    public ResponseEntity<List<SubmissionResponse>> getPendingSubmissionsForDepartmentSecretary(@PathVariable String deptSecretaryEmpId) {
        log.debug("Getting pending submissions for department secretary: {}", deptSecretaryEmpId);
        List<SubmissionResponse> submissions = submissionService.getSubmissionsPendingForRole(deptSecretaryEmpId, "DEPARTMENT_SECRETARY");
        return ResponseEntity.ok(submissions);
    }

    /**
     * Update submission status by department secretary
     */
    @PutMapping("/{submissionId}/department-review")
    @PreAuthorize("hasRole('DEPARTMENT_SECRETARY')")
    @Operation(summary = "Update submission status by department secretary")
    public ResponseEntity<SubmissionResponse> reviewSubmissionByDepartmentSecretary(
            @PathVariable String submissionId,
            @RequestParam SubmissionStatus status) {
        log.info("Department secretary reviewing submission {} with status: {}", submissionId, status);
        
        try {
            SubmissionResponse response = submissionService.updateSubmissionStatusByDepartmentSecretary(submissionId, status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating submission status by department secretary: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get submissions pending for dean officer review
     */
    @GetMapping("/dean-officer/{deanOfficerEmpId}/pending")
    @PreAuthorize("hasRole('DEAN_OFFICER')")
    @Operation(summary = "Get submissions pending for dean officer review")
    public ResponseEntity<List<SubmissionResponse>> getPendingSubmissionsForDeanOfficer(@PathVariable String deanOfficerEmpId) {
        log.debug("Getting pending submissions for dean officer: {}", deanOfficerEmpId);
        List<SubmissionResponse> submissions = submissionService.getSubmissionsPendingForRole(deanOfficerEmpId, "DEAN_OFFICER");
        return ResponseEntity.ok(submissions);
    }

    /**
     * Update submission status by dean officer
     */
    @PutMapping("/{submissionId}/dean-review")
    @PreAuthorize("hasRole('DEAN_OFFICER')")
    @Operation(summary = "Update submission status by dean officer")
    public ResponseEntity<SubmissionResponse> reviewSubmissionByDeanOfficer(
            @PathVariable String submissionId,
            @RequestParam SubmissionStatus status) {
        log.info("Dean officer reviewing submission {} with status: {}", submissionId, status);
        
        try {
            SubmissionResponse response = submissionService.updateSubmissionStatusByDeanOfficer(submissionId, status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating submission status by dean officer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get submissions pending for student affairs review
     */
    @GetMapping("/student-affairs/{studentAffairsEmpId}/pending")
    @PreAuthorize("hasRole('STUDENT_AFFAIRS')")
    @Operation(summary = "Get submissions pending for student affairs review")
    public ResponseEntity<List<SubmissionResponse>> getPendingSubmissionsForStudentAffairs(@PathVariable String studentAffairsEmpId) {
        log.debug("Getting pending submissions for student affairs: {}", studentAffairsEmpId);
        List<SubmissionResponse> submissions = submissionService.getSubmissionsPendingForRole(studentAffairsEmpId, "STUDENT_AFFAIRS");
        return ResponseEntity.ok(submissions);
    }

    /**
     * Update submission status by student affairs (final review)
     */
    @PutMapping("/{submissionId}/final-review")
    @PreAuthorize("hasRole('STUDENT_AFFAIRS')")
    @Operation(summary = "Final review of submission by student affairs")
    public ResponseEntity<SubmissionResponse> finalReviewByStudentAffairs(
            @PathVariable String submissionId,
            @RequestParam SubmissionStatus status) {
        log.info("Student affairs final review of submission {} with status: {}", submissionId, status);
        
        try {
            SubmissionResponse response = submissionService.updateSubmissionStatusByStudentAffairs(submissionId, status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in final review by student affairs: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}