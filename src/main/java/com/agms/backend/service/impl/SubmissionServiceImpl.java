package com.agms.backend.service.impl;

import com.agms.backend.dto.CreateSubmissionRequest;
import com.agms.backend.dto.SubmissionResponse;
import com.agms.backend.exception.ResourceNotFoundException;
import com.agms.backend.model.AdvisorList;
import com.agms.backend.model.DepartmentList;
import com.agms.backend.model.FacultyList;
import com.agms.backend.model.File;
import com.agms.backend.model.Submission;
import com.agms.backend.model.SubmissionStatus;
import com.agms.backend.model.users.Advisor;
import com.agms.backend.model.users.DeanOfficer;
import com.agms.backend.model.users.DepartmentSecretary;
import com.agms.backend.model.users.Student;
import com.agms.backend.model.users.StudentAffairs;
import com.agms.backend.repository.AdvisorListRepository;
import com.agms.backend.repository.AdvisorRepository;
import com.agms.backend.repository.DeanOfficerRepository;
import com.agms.backend.repository.DepartmentSecretaryRepository;
import com.agms.backend.repository.StudentAffairsRepository;
import com.agms.backend.repository.StudentRepository;
import com.agms.backend.repository.SubmissionRepository;
import com.agms.backend.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final StudentRepository studentRepository;
    private final AdvisorRepository advisorRepository;
    private final AdvisorListRepository advisorListRepository;
    private final DeanOfficerRepository deanOfficerRepository;
    private final StudentAffairsRepository studentAffairsRepository;
    private final DepartmentSecretaryRepository departmentSecretaryRepository;
    private final com.agms.backend.repository.UserRepository userRepository;

    @Override
    @Transactional
    public SubmissionResponse createGraduationSubmission(CreateSubmissionRequest request) {
        log.info("Creating graduation submission for student: {}", request.getStudentNumber());

        // Find the student
        Student student = studentRepository.findByStudentNumber(request.getStudentNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student not found with number: " + request.getStudentNumber()));

        // Check if student already has a pending submission
        if (hasActivePendingSubmission(request.getStudentNumber())) {
            throw new IllegalStateException("Student already has an active pending graduation submission");
        }

        // Find the student's advisor
        Advisor advisor = student.getAdvisor();
        if (advisor == null) {
            throw new IllegalStateException("Student does not have an assigned advisor");
        }

        // Find the advisor's advisor list
        AdvisorList advisorList = advisor.getAdvisorList();
        if (advisorList == null) {
            throw new IllegalStateException("Advisor does not have an advisor list configured");
        }

        // Create the submission (ID will be generated)
        String submissionId = generateSubmissionId();
        Submission submission = Submission.builder()
                .submissionId(submissionId)
                .submissionDate(LocalDate.now())
                .content(request.getContent())
                .status(SubmissionStatus.PENDING)
                .student(student)
                .advisorList(advisorList)
                .build();

        // Save the submission
        Submission savedSubmission = submissionRepository.save(submission);

        log.info("Created graduation submission with ID: {} for student: {}", savedSubmission.getSubmissionId(),
                request.getStudentNumber());

        return convertToResponse(savedSubmission);
    }

    @Override
    public List<SubmissionResponse> getSubmissionsByStudent(String studentNumber) {
        log.debug("Getting submissions for student: {}", studentNumber);

        List<Submission> submissions = submissionRepository.findByStudentNumber(studentNumber);
        return submissions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SubmissionResponse> getSubmissionsByAdvisor(String advisorEmpId) {
        log.debug("Getting submissions for advisor: {}", advisorEmpId);

        // Find the advisor
        Advisor advisor = advisorRepository.findByEmpId(advisorEmpId)
                .orElseThrow(() -> new ResourceNotFoundException("Advisor not found with empId: " + advisorEmpId));

        // Find the advisor's advisor list
        AdvisorList advisorList = advisor.getAdvisorList();
        if (advisorList == null) {
            log.warn("Advisor {} does not have an advisor list", advisorEmpId);
            return List.of();
        }

        List<Submission> submissions = submissionRepository.findByAdvisorListId(advisorList.getAdvisorListId());
        return submissions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<SubmissionResponse> getSubmissionById(String submissionId) {
        log.debug("Getting submission by ID: {}", submissionId);

        return submissionRepository.findById(submissionId)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional
    public SubmissionResponse updateSubmissionStatus(String submissionId, SubmissionStatus status) {
        log.info("Updating submission {} status to: {}", submissionId, status);

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + submissionId));

        submission.setStatus(status);
        Submission updatedSubmission = submissionRepository.save(submission);

        log.info("Updated submission {} status to: {}", submissionId, status);

        return convertToResponse(updatedSubmission);
    }

    @Override
    public List<SubmissionResponse> getSubmissionsByStatus(SubmissionStatus status) {
        log.debug("Getting submissions with status: {}", status);

        List<Submission> submissions = submissionRepository.findByStatus(status);
        return submissions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasActivePendingSubmission(String studentNumber) {
        return submissionRepository.hasActivePendingSubmission(studentNumber);
    }

    @Override
    public Optional<SubmissionResponse> getLatestSubmissionByStudent(String studentNumber) {
        log.debug("Getting latest submission for student: {}", studentNumber);

        List<Submission> submissions = submissionRepository.findByStudentNumberOrderBySubmissionDateDesc(studentNumber);
        if (submissions.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(convertToResponse(submissions.get(0)));
    }

    @Override
    @Transactional
    public void deleteSubmission(String submissionId) {
        log.info("Deleting submission: {}", submissionId);

        if (!submissionRepository.existsById(submissionId)) {
            throw new ResourceNotFoundException("Submission not found with ID: " + submissionId);
        }

        submissionRepository.deleteById(submissionId);

        log.info("Deleted submission: {}", submissionId);
    }

    @Override
    public List<SubmissionResponse> getSubmissionsPendingForRole(String empId, String role) {
        switch (role) {
            case "ADVISOR":
                return getSubmissionsForAdvisor(empId, SubmissionStatus.PENDING);
            case "DEPARTMENT_SECRETARY":
                return getSubmissionsForDepartmentSecretary(empId, SubmissionStatus.APPROVED_BY_ADVISOR);
            case "DEAN_OFFICER":
                return getSubmissionsForDeanOfficer(empId, SubmissionStatus.APPROVED_BY_DEPT);
            case "STUDENT_AFFAIRS":
                return getSubmissionsForStudentAffairs(empId, SubmissionStatus.APPROVED_BY_DEAN);
            default:
                throw new IllegalArgumentException("Invalid role: " + role);
        }
    }

    @Override
    @Transactional
    public SubmissionResponse updateSubmissionStatusByAdvisor(String submissionId, SubmissionStatus status,
            String rejectionReason) {
        validateAdvisorStatusTransition(status);

        // If rejecting and rejection reason is provided, update the content field
        if (isRejectionStatus(status) && rejectionReason != null && !rejectionReason.trim().isEmpty()) {
            return updateSubmissionWithWorkflowAndReason(submissionId, status, "ADVISOR", rejectionReason);
        } else {
            return updateSubmissionWithWorkflow(submissionId, status, "ADVISOR");
        }
    }

    @Override
    @Transactional
    public SubmissionResponse updateSubmissionStatusByDepartmentSecretary(String submissionId, SubmissionStatus status,
            String rejectionReason) {
        validateDepartmentSecretaryStatusTransition(status);

        // If rejecting and rejection reason is provided, update the content field
        if (isRejectionStatus(status) && rejectionReason != null && !rejectionReason.trim().isEmpty()) {
            return updateSubmissionWithWorkflowAndReason(submissionId, status, "DEPARTMENT_SECRETARY", rejectionReason);
        } else {
            return updateSubmissionWithWorkflow(submissionId, status, "DEPARTMENT_SECRETARY");
        }
    }

    @Override
    @Transactional
    public SubmissionResponse updateSubmissionStatusByDeanOfficer(String submissionId, SubmissionStatus status,
            String rejectionReason) {
        validateDeanOfficerStatusTransition(status);

        // If rejecting and rejection reason is provided, update the content field
        if (isRejectionStatus(status) && rejectionReason != null && !rejectionReason.trim().isEmpty()) {
            return updateSubmissionWithWorkflowAndReason(submissionId, status, "DEAN_OFFICER", rejectionReason);
        } else {
            return updateSubmissionWithWorkflow(submissionId, status, "DEAN_OFFICER");
        }
    }

    @Override
    @Transactional
    public SubmissionResponse updateSubmissionStatusByStudentAffairs(String submissionId, SubmissionStatus status,
            String rejectionReason) {
        validateStudentAffairsStatusTransition(status);

        // If rejecting and rejection reason is provided, update the content field
        if (isRejectionStatus(status) && rejectionReason != null && !rejectionReason.trim().isEmpty()) {
            return updateSubmissionWithWorkflowAndReason(submissionId, status, "STUDENT_AFFAIRS", rejectionReason);
        } else {
            return updateSubmissionWithWorkflow(submissionId, status, "STUDENT_AFFAIRS");
        }
    }

    private SubmissionResponse updateSubmissionWithWorkflow(String submissionId, SubmissionStatus newStatus,
            String reviewerRole) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + submissionId));

        SubmissionStatus oldStatus = submission.getStatus();
        submission.setStatus(newStatus);
        Submission updatedSubmission = submissionRepository.save(submission);

        handleWorkflowProgression(updatedSubmission, oldStatus, newStatus, reviewerRole);

        log.info("Updated submission {} status from {} to {} by {}", submissionId, oldStatus, newStatus, reviewerRole);
        return convertToResponse(updatedSubmission);
    }

    private SubmissionResponse updateSubmissionWithWorkflowAndReason(String submissionId, SubmissionStatus newStatus,
            String reviewerRole, String rejectionReason) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with ID: " + submissionId));

        SubmissionStatus oldStatus = submission.getStatus();
        submission.setStatus(newStatus);
        submission.setContent(rejectionReason);
        Submission updatedSubmission = submissionRepository.save(submission);

        handleWorkflowProgression(updatedSubmission, oldStatus, newStatus, reviewerRole);

        log.info("Updated submission {} status from {} to {} by {} with reason: {}", submissionId, oldStatus, newStatus,
                reviewerRole, rejectionReason);
        return convertToResponse(updatedSubmission);
    }

    private void handleWorkflowProgression(Submission submission, SubmissionStatus oldStatus,
            SubmissionStatus newStatus, String reviewerRole) {
        if (isApprovalStatus(newStatus)) {
            forwardToNextLevel(submission, newStatus);
        } else if (isRejectionStatus(newStatus)) {
            handleRejection(submission, reviewerRole);
        }
    }

    private boolean isApprovalStatus(SubmissionStatus status) {
        return status == SubmissionStatus.APPROVED_BY_ADVISOR ||
                status == SubmissionStatus.APPROVED_BY_DEPT ||
                status == SubmissionStatus.APPROVED_BY_DEAN ||
                status == SubmissionStatus.FINAL_APPROVED;
    }

    private boolean isRejectionStatus(SubmissionStatus status) {
        return status == SubmissionStatus.REJECTED_BY_ADVISOR ||
                status == SubmissionStatus.REJECTED_BY_DEPT ||
                status == SubmissionStatus.REJECTED_BY_DEAN ||
                status == SubmissionStatus.FINAL_REJECTED;
    }

    private void forwardToNextLevel(Submission submission, SubmissionStatus currentStatus) {
        String nextLevel = determineNextLevel(currentStatus);
        log.info("Forwarding submission {} to {}", submission.getSubmissionId(), nextLevel);

        // Here you could add notification logic, workflow tracking, etc.
        switch (nextLevel) {
            case "DEPARTMENT_SECRETARY":
                notifyDepartmentSecretary(submission);
                break;
            case "DEAN_OFFICER":
                notifyDeanOfficer(submission);
                break;
            case "STUDENT_AFFAIRS":
                notifyStudentAffairs(submission);
                break;
            case "COMPLETED":
                handleFinalApproval(submission);
                break;
        }
    }

    private String determineNextLevel(SubmissionStatus status) {
        switch (status) {
            case APPROVED_BY_ADVISOR:
                return "DEPARTMENT_SECRETARY";
            case APPROVED_BY_DEPT:
                return "DEAN_OFFICER";
            case APPROVED_BY_DEAN:
                return "STUDENT_AFFAIRS";
            case FINAL_APPROVED:
                return "COMPLETED";
            default:
                return "UNKNOWN";
        }
    }

    // Validation methods for each role
    private void validateAdvisorStatusTransition(SubmissionStatus status) {
        if (status != SubmissionStatus.APPROVED_BY_ADVISOR && status != SubmissionStatus.REJECTED_BY_ADVISOR) {
            throw new IllegalArgumentException("Advisor can only approve or reject submissions");
        }
    }

    private void validateDepartmentSecretaryStatusTransition(SubmissionStatus status) {
        if (status != SubmissionStatus.APPROVED_BY_DEPT && status != SubmissionStatus.REJECTED_BY_DEPT) {
            throw new IllegalArgumentException("Department Secretary can only approve or reject submissions");
        }
    }

    private void validateDeanOfficerStatusTransition(SubmissionStatus status) {
        if (status != SubmissionStatus.APPROVED_BY_DEAN && status != SubmissionStatus.REJECTED_BY_DEAN) {
            throw new IllegalArgumentException("Dean Officer can only approve or reject submissions");
        }
    }

    private void validateStudentAffairsStatusTransition(SubmissionStatus status) {
        if (status != SubmissionStatus.FINAL_APPROVED && status != SubmissionStatus.FINAL_REJECTED) {
            throw new IllegalArgumentException("Student Affairs can only give final approval or rejection");
        }
    }

    // Notification/workflow methods (implement as needed)
    private void notifyDepartmentSecretary(Submission submission) {
        log.info("Notifying department secretary about submission: {}", submission.getSubmissionId());
        // Implement notification logic
    }

    private void notifyDeanOfficer(Submission submission) {
        log.info("Notifying dean officer about submission: {}", submission.getSubmissionId());
        // Implement notification logic
    }

    private void notifyStudentAffairs(Submission submission) {
        log.info("Notifying student affairs about submission: {}", submission.getSubmissionId());
        // Implement notification logic
    }

    private void handleFinalApproval(Submission submission) {
        log.info("Submission {} has received final approval", submission.getSubmissionId());
        // Implement final approval logic (e.g., generate graduation certificate, notify
        // student, etc.)
    }

    private void handleRejection(Submission submission, String reviewerRole) {
        log.info("Submission {} rejected by {}", submission.getSubmissionId(), reviewerRole);
        // Implement rejection notification logic
    }

    // Helper methods to get submissions for each role with specific status
    private List<SubmissionResponse> getSubmissionsForAdvisor(String advisorEmpId, SubmissionStatus status) {
        // Implementation similar to existing getSubmissionsByAdvisor but filtered by
        // status
        Advisor advisor = advisorRepository.findByEmpId(advisorEmpId)
                .orElseThrow(() -> new ResourceNotFoundException("Advisor not found with empId: " + advisorEmpId));

        AdvisorList advisorList = advisor.getAdvisorList();
        if (advisorList == null) {
            return List.of();
        }

        List<Submission> submissions = submissionRepository.findByAdvisorListIdAndStatus(
                advisorList.getAdvisorListId(), status);
        return submissions.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    private List<SubmissionResponse> getSubmissionsForDepartmentSecretary(String deptSecretaryEmpId,
            SubmissionStatus status) {
        DepartmentSecretary departmentSecretary = departmentSecretaryRepository.findByEmpId(deptSecretaryEmpId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department Secretary not found with empId: " + deptSecretaryEmpId));

        // Simple approach: Find all advisors under this department secretary and get
        // their submissions
        List<Advisor> advisors = advisorRepository.findByDepartmentSecretaryEmpId(deptSecretaryEmpId);
        List<Submission> allSubmissions = new ArrayList<>();

        log.debug("Department Secretary {} has {} advisors", deptSecretaryEmpId, advisors.size());

        for (Advisor advisor : advisors) {
            if (advisor.getAdvisorList() != null) {
                List<Submission> advisorSubmissions = submissionRepository.findByAdvisorListIdAndStatus(
                        advisor.getAdvisorList().getAdvisorListId(), status);
                log.debug("Advisor {} (AdvisorList {}) has {} submissions with status {}",
                        advisor.getEmpId(), advisor.getAdvisorList().getAdvisorListId(),
                        advisorSubmissions.size(), status);
                allSubmissions.addAll(advisorSubmissions);
            }
        }

        log.debug("Total submissions found for Department Secretary {}: {}", deptSecretaryEmpId, allSubmissions.size());
        return allSubmissions.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    private List<SubmissionResponse> getSubmissionsForDeanOfficer(String deanOfficerEmpId, SubmissionStatus status) {
        DeanOfficer deanOfficer = deanOfficerRepository.findByEmpId(deanOfficerEmpId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Dean Officer not found with empId: " + deanOfficerEmpId));

        // Simple approach: Find all department secretaries under this dean officer,
        // then their advisors
        List<DepartmentSecretary> departmentSecretaries = departmentSecretaryRepository
                .findByDeanOfficerEmpId(deanOfficerEmpId);
        List<Submission> allSubmissions = new ArrayList<>();

        log.debug("Dean Officer {} has {} department secretaries", deanOfficerEmpId, departmentSecretaries.size());

        for (DepartmentSecretary deptSecretary : departmentSecretaries) {
            List<Advisor> advisors = advisorRepository.findByDepartmentSecretaryEmpId(deptSecretary.getEmpId());
            for (Advisor advisor : advisors) {
                if (advisor.getAdvisorList() != null) {
                    List<Submission> advisorSubmissions = submissionRepository.findByAdvisorListIdAndStatus(
                            advisor.getAdvisorList().getAdvisorListId(), status);
                    allSubmissions.addAll(advisorSubmissions);
                }
            }
        }

        log.debug("Total submissions found for Dean Officer {}: {}", deanOfficerEmpId, allSubmissions.size());
        return allSubmissions.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    private List<SubmissionResponse> getSubmissionsForStudentAffairs(String studentAffairsEmpId,
            SubmissionStatus status) {
        StudentAffairs studentAffairs = studentAffairsRepository.findByEmpId(studentAffairsEmpId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Student Affairs not found with empId: " + studentAffairsEmpId));

        // Student Affairs sees all submissions that have been approved by dean officers
        List<Submission> submissions = submissionRepository.findByStatus(status);
        return submissions.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    private SubmissionResponse convertToResponse(Submission submission) {
        Student student = submission.getStudent();
        String studentName = student.getFirstName() + " " + student.getLastName();

        List<SubmissionResponse.FileInfo> fileInfos = List.of();
        if (submission.getFiles() != null) {
            fileInfos = submission.getFiles().stream()
                    .map(file -> SubmissionResponse.FileInfo.builder()
                            .fileId(file.getFileId().toString())
                            .fileName(file.getFileName())
                            .fileType(file.getFileType())
                            .uploadDate(file.getUploadDate())
                            .uploaderName(file.getUploader().getFirstName() + " " + file.getUploader().getLastName())
                            .build())
                    .collect(Collectors.toList());
        }

        return SubmissionResponse.builder()
                .submissionId(submission.getSubmissionId())
                .submissionDate(submission.getSubmissionDate())
                .content(submission.getContent())
                .status(submission.getStatus())
                .studentNumber(student.getStudentNumber())
                .studentName(studentName)
                .advisorListId(
                        submission.getAdvisorList() != null ? submission.getAdvisorList().getAdvisorListId() : null)
                .files(fileInfos)
                .build();
    }

    private String generateSubmissionId() {
        // Generate a submission ID using UUID for guaranteed uniqueness
        return "SUB_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    @Override
    public List<SubmissionResponse> getSubmissionsByDepartmentSecretary(String deptSecretaryEmpId) {
        return getSubmissionsForDepartmentSecretary(deptSecretaryEmpId, SubmissionStatus.APPROVED_BY_ADVISOR);
    }

    @Override
    public List<SubmissionResponse> getSubmissionsByDeanOfficer(String deanOfficerEmpId) {
        return getSubmissionsForDeanOfficer(deanOfficerEmpId, SubmissionStatus.APPROVED_BY_DEPT);
    }

    @Override
    public List<SubmissionResponse> getSubmissionsByStudentAffairs(String studentAffairsEmpId) {
        return getSubmissionsForStudentAffairs(studentAffairsEmpId, SubmissionStatus.APPROVED_BY_DEAN);
    }

    @Override
    @Transactional
    public SubmissionResponse approveSubmission(String submissionId) {
        // Get current user role from Spring Security context
        String userRole = getCurrentUserRole();
        String userEmpId = getCurrentUserEmpId();

        log.info("User with role {} and empId {} approving submission: {}", userRole, userEmpId, submissionId);

        switch (userRole) {
            case "ADVISOR":
                return updateSubmissionStatusByAdvisor(submissionId, SubmissionStatus.APPROVED_BY_ADVISOR, null);
            case "DEPARTMENT_SECRETARY":
                return updateSubmissionStatusByDepartmentSecretary(submissionId, SubmissionStatus.APPROVED_BY_DEPT,
                        null);
            case "DEAN_OFFICER":
                return updateSubmissionStatusByDeanOfficer(submissionId, SubmissionStatus.APPROVED_BY_DEAN, null);
            case "STUDENT_AFFAIRS":
                return updateSubmissionStatusByStudentAffairs(submissionId, SubmissionStatus.FINAL_APPROVED, null);
            default:
                throw new IllegalArgumentException(
                        "User role " + userRole + " is not authorized to approve submissions");
        }
    }

    @Override
    @Transactional
    public SubmissionResponse rejectSubmission(String submissionId, String rejectionReason) {
        // Get current user role from Spring Security context
        String userRole = getCurrentUserRole();
        String userEmpId = getCurrentUserEmpId();

        log.info("User with role {} and empId {} rejecting submission: {}", userRole, userEmpId, submissionId);

        switch (userRole) {
            case "ADVISOR":
                return updateSubmissionStatusByAdvisor(submissionId, SubmissionStatus.REJECTED_BY_ADVISOR,
                        rejectionReason);
            case "DEPARTMENT_SECRETARY":
                return updateSubmissionStatusByDepartmentSecretary(submissionId, SubmissionStatus.REJECTED_BY_DEPT,
                        rejectionReason);
            case "DEAN_OFFICER":
                return updateSubmissionStatusByDeanOfficer(submissionId, SubmissionStatus.REJECTED_BY_DEAN,
                        rejectionReason);
            case "STUDENT_AFFAIRS":
                return updateSubmissionStatusByStudentAffairs(submissionId, SubmissionStatus.FINAL_REJECTED,
                        rejectionReason);
            default:
                throw new IllegalArgumentException("Role " + userRole + " is not authorized to reject submissions");
        }
    }

    @Override
    public List<SubmissionResponse> getMySubmissions() {
        String userRole = getCurrentUserRole();
        String userEmail = getCurrentUserEmail();
        
        log.debug("Getting all submissions for user with role {} and email {}", userRole, userEmail);
        
        switch (userRole) {
            case "STUDENT":
                // For students, get their own submissions using email to find student
                Student student = studentRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
                return getSubmissionsByStudent(student.getStudentNumber());
                
            case "ADVISOR":
                String advisorEmpId = getCurrentUserEmpId();
                return getSubmissionsByAdvisor(advisorEmpId);
                
            case "DEPARTMENT_SECRETARY":
                String deptEmpId = getCurrentUserEmpId();
                return getSubmissionsByDepartmentSecretary(deptEmpId);
                
            case "DEAN_OFFICER":
                String deanEmpId = getCurrentUserEmpId();
                return getSubmissionsByDeanOfficer(deanEmpId);
                
            case "STUDENT_AFFAIRS":
                String saEmpId = getCurrentUserEmpId();
                return getSubmissionsByStudentAffairs(saEmpId);
                
            default:
                throw new IllegalArgumentException("Unsupported role for getting submissions: " + userRole);
        }
    }

    @Override
    public List<SubmissionResponse> getMyPendingSubmissions() {
        String userRole = getCurrentUserRole();
        String userEmpId = getCurrentUserEmpId();
        
        log.debug("Getting pending submissions for user with role {} and empId {}", userRole, userEmpId);
        
        switch (userRole) {
            case "ADVISOR":
                // Advisors see submissions with status PENDING
                return getSubmissionsPendingForRole(userEmpId, "ADVISOR");
                
            case "DEPARTMENT_SECRETARY":
                // Department Secretaries see submissions with status APPROVED_BY_ADVISOR
                return getSubmissionsPendingForRole(userEmpId, "DEPARTMENT_SECRETARY");
                
            case "DEAN_OFFICER":
                // Dean Officers see submissions with status APPROVED_BY_DEPT
                return getSubmissionsPendingForRole(userEmpId, "DEAN_OFFICER");
                
            case "STUDENT_AFFAIRS":
                // Student Affairs see submissions with status APPROVED_BY_DEAN
                return getSubmissionsPendingForRole(userEmpId, "STUDENT_AFFAIRS");
                
            default:
                throw new IllegalArgumentException("Role " + userRole + " does not have pending submissions to review");
        }
    }

    private String getCurrentUserRole() {
        // Get the current authentication from Spring Security context
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        // Extract role from authorities
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No valid role found for current user"));
    }

    private String getCurrentUserEmpId() {
        // Get the current user's email from Spring Security context
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        String userEmail = authentication.getName();
        String userRole = getCurrentUserRole();

        // Find the user by email using UserRepository
        com.agms.backend.model.users.User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // Extract empId based on role
        switch (userRole) {
            case "ADVISOR":
                if (user instanceof com.agms.backend.model.users.Advisor) {
                    return ((com.agms.backend.model.users.Advisor) user).getEmpId();
                }
                throw new IllegalStateException("User is not an Advisor but has ADVISOR role");
            case "DEPARTMENT_SECRETARY":
                if (user instanceof com.agms.backend.model.users.DepartmentSecretary) {
                    return ((com.agms.backend.model.users.DepartmentSecretary) user).getEmpId();
                }
                throw new IllegalStateException("User is not a DepartmentSecretary but has DEPARTMENT_SECRETARY role");
            case "DEAN_OFFICER":
                if (user instanceof com.agms.backend.model.users.DeanOfficer) {
                    return ((com.agms.backend.model.users.DeanOfficer) user).getEmpId();
                }
                throw new IllegalStateException("User is not a DeanOfficer but has DEAN_OFFICER role");
            case "STUDENT_AFFAIRS":
                if (user instanceof com.agms.backend.model.users.StudentAffairs) {
                    return ((com.agms.backend.model.users.StudentAffairs) user).getEmpId();
                }
                throw new IllegalStateException("User is not StudentAffairs but has STUDENT_AFFAIRS role");
            default:
                throw new IllegalArgumentException("Unsupported role for empId lookup: " + userRole);
        }
    }

    private String getCurrentUserEmail() {
        org.springframework.security.core.Authentication authentication = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        
        return authentication.getName();
    }
} 
