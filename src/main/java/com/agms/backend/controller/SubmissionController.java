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
import com.agms.backend.dto.StartRegularGraduationRequest;
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
     * Get all submissions for the current authenticated user (role-agnostic)
     */
    @GetMapping("/my-submissions")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADVISOR') or hasRole('DEPARTMENT_SECRETARY') or hasRole('DEAN_OFFICER') or hasRole('STUDENT_AFFAIRS')")
    @Operation(summary = "Get all submissions for current user - automatically detects role and returns appropriate submissions")
    public ResponseEntity<List<SubmissionResponse>> getMySubmissions() {
        log.debug("Getting submissions for current authenticated user");

        try {
            List<SubmissionResponse> submissions = submissionService.getMySubmissions();
            return ResponseEntity.ok(submissions);
        } catch (Exception e) {
            log.error("Error getting submissions for current user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get pending submissions for the current authenticated user (role-agnostic)
     */
    @GetMapping("/my-pending")
    @PreAuthorize("hasRole('ADVISOR') or hasRole('DEPARTMENT_SECRETARY') or hasRole('DEAN_OFFICER') or hasRole('STUDENT_AFFAIRS')")
    @Operation(summary = "Get pending submissions for current user - automatically detects role and returns submissions awaiting review")
    public ResponseEntity<List<SubmissionResponse>> getMyPendingSubmissions() {
        log.debug("Getting pending submissions for current authenticated user");

        try {
            List<SubmissionResponse> submissions = submissionService.getMyPendingSubmissions();
            return ResponseEntity.ok(submissions);
        } catch (Exception e) {
            log.error("Error getting pending submissions for current user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get submissions pending for department secretary review
     */
    @GetMapping("/department-secretary/{deptSecretaryEmpId}/pending")
    @PreAuthorize("hasRole('DEPARTMENT_SECRETARY')")
    @Operation(summary = "Get submissions pending for department secretary review")
    @Deprecated
    public ResponseEntity<List<SubmissionResponse>> getPendingSubmissionsForDepartmentSecretary(
            @PathVariable String deptSecretaryEmpId) {
        log.debug("Getting pending submissions for department secretary: {}", deptSecretaryEmpId);
        List<SubmissionResponse> submissions = submissionService.getSubmissionsPendingForRole(deptSecretaryEmpId,
                "DEPARTMENT_SECRETARY");
        return ResponseEntity.ok(submissions);
    }

    /**
     * Get submissions pending for dean officer review
     */
    @GetMapping("/dean-officer/{deanOfficerEmpId}/pending")
    @PreAuthorize("hasRole('DEAN_OFFICER')")
    @Operation(summary = "Get submissions pending for dean officer review")
    @Deprecated
    public ResponseEntity<List<SubmissionResponse>> getPendingSubmissionsForDeanOfficer(
            @PathVariable String deanOfficerEmpId) {
        log.debug("Getting pending submissions for dean officer: {}", deanOfficerEmpId);
        List<SubmissionResponse> submissions = submissionService.getSubmissionsPendingForRole(deanOfficerEmpId,
                "DEAN_OFFICER");
        return ResponseEntity.ok(submissions);
    }

    /**
     * Get submissions pending for student affairs review
     */
    @GetMapping("/student-affairs/{studentAffairsEmpId}/pending")
    @PreAuthorize("hasRole('STUDENT_AFFAIRS')")
    @Operation(summary = "Get submissions pending for student affairs review")
    @Deprecated
    public ResponseEntity<List<SubmissionResponse>> getPendingSubmissionsForStudentAffairs(
            @PathVariable String studentAffairsEmpId) {
        log.debug("Getting pending submissions for student affairs: {}", studentAffairsEmpId);
        List<SubmissionResponse> submissions = submissionService.getSubmissionsPendingForRole(studentAffairsEmpId,
                "STUDENT_AFFAIRS");
        return ResponseEntity.ok(submissions);
    }

    /**
     * Approve submission (role-agnostic - automatically determines correct approval
     * status based on user role)
     */
    @PutMapping("/{submissionId}/approve")
    @PreAuthorize("hasRole('ADVISOR') or hasRole('DEPARTMENT_SECRETARY') or hasRole('DEAN_OFFICER') or hasRole('STUDENT_AFFAIRS')")
    @Operation(summary = "Approve submission - automatically handles workflow progression based on user role")
    public ResponseEntity<SubmissionResponse> approveSubmission(@PathVariable String submissionId) {
        log.info("Approving submission: {}", submissionId);

        try {
            SubmissionResponse response = submissionService.approveSubmission(submissionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error approving submission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Reject submission (role-agnostic - automatically determines correct rejection
     * status based on user role)
     */
    @PutMapping("/{submissionId}/reject")
    @PreAuthorize("hasRole('ADVISOR') or hasRole('DEPARTMENT_SECRETARY') or hasRole('DEAN_OFFICER') or hasRole('STUDENT_AFFAIRS')")
    @Operation(summary = "Reject submission - automatically handles workflow termination based on user role")
    public ResponseEntity<SubmissionResponse> rejectSubmission(
            @PathVariable String submissionId,
            @RequestParam(required = false) String rejectionReason) {
        try {
            SubmissionResponse response = submissionService.rejectSubmission(submissionId, rejectionReason);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error rejecting submission: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get submissions pending for advisor review
     */
    @GetMapping("/advisor/{advisorEmpId}/pending")
    @PreAuthorize("hasRole('ADVISOR')")
    @Operation(summary = "Get submissions pending for advisor review")
    @Deprecated
    public ResponseEntity<List<SubmissionResponse>> getPendingSubmissionsForAdvisor(@PathVariable String advisorEmpId) {
        log.debug("Getting pending submissions for advisor: {}", advisorEmpId);
        List<SubmissionResponse> submissions = submissionService.getSubmissionsPendingForRole(advisorEmpId, "ADVISOR");
        return ResponseEntity.ok(submissions);
    }

    /**
     * Start regular graduation process - creates submissions for all eligible students
     */
    @PostMapping("/regular-graduation/start")
    @PreAuthorize("hasRole('STUDENT_AFFAIRS')")
    @Operation(summary = "Start regular graduation process - automatically creates submissions for all eligible students")
    public ResponseEntity<List<SubmissionResponse>> startRegularGraduation(
            @Valid @RequestBody StartRegularGraduationRequest request) {
        log.info("Starting regular graduation process for term: {}", request.getTerm());

        try {
            List<SubmissionResponse> createdSubmissions = submissionService.startRegularGraduation(request.getTerm());
            log.info("Regular graduation process completed. Created {} submissions", createdSubmissions.size());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSubmissions);
        } catch (IllegalStateException e) {
            log.warn("Cannot start regular graduation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error starting regular graduation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Finalize list for current authenticated user
     */
    @PostMapping("/finalize-my-list")
    @PreAuthorize("hasRole('ADVISOR') or hasRole('DEPARTMENT_SECRETARY') or hasRole('DEAN_OFFICER') or hasRole('STUDENT_AFFAIRS')")
    @Operation(summary = "Finalize list for current user - marks completion of review process for their level")
    public ResponseEntity<Boolean> finalizeMyList() {
        log.info("Finalizing list for current authenticated user");

        try {
            boolean finalized = submissionService.finalizeMyList();
            if (finalized) {
                log.info("List successfully finalized for current user");
                return ResponseEntity.ok(true);
            } else {
                log.warn("Cannot finalize list - prerequisites not met");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(false);
            }
        } catch (Exception e) {
            log.error("Error finalizing list: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    /**
     * Check if current user's list is finalized
     */
    @GetMapping("/my-list/finalized")
    @PreAuthorize("hasRole('ADVISOR') or hasRole('DEPARTMENT_SECRETARY') or hasRole('DEAN_OFFICER') or hasRole('STUDENT_AFFAIRS')")
    @Operation(summary = "Check if current user's list is finalized")
    public ResponseEntity<Boolean> isMyListFinalized() {
        log.debug("Checking if current user's list is finalized");

        try {
            boolean finalized = submissionService.isMyListFinalized();
            return ResponseEntity.ok(finalized);
        } catch (Exception e) {
            log.error("Error checking list finalization status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    /**
     * Check if all prerequisite lists are finalized for current user's role
     */
    @GetMapping("/prerequisite-lists/finalized")
    @PreAuthorize("hasRole('DEPARTMENT_SECRETARY') or hasRole('DEAN_OFFICER') or hasRole('STUDENT_AFFAIRS')")
    @Operation(summary = "Check if all prerequisite lists are finalized for current user's role")
    public ResponseEntity<Boolean> arePrerequisiteListsFinalized() {
        log.debug("Checking if prerequisite lists are finalized for current user");

        try {
            boolean finalized = submissionService.arePrerequisiteListsFinalized();
            return ResponseEntity.ok(finalized);
        } catch (Exception e) {
            log.error("Error checking prerequisite lists finalization: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }
}