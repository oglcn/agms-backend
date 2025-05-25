package com.agms.backend.service.impl;

import com.agms.backend.dto.CreateSubmissionRequest;
import com.agms.backend.dto.RegularGraduationTrackResponse;
import com.agms.backend.dto.SubmissionResponse;
import com.agms.backend.dto.SubordinateStatusResponse;
import com.agms.backend.dto.TopStudentsResponse;
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
import com.agms.backend.repository.DepartmentListRepository;
import com.agms.backend.repository.DepartmentSecretaryRepository;
import com.agms.backend.repository.FacultyListRepository;
import com.agms.backend.repository.GraduationListRepository;
import com.agms.backend.repository.GraduationRepository;
import com.agms.backend.repository.StudentAffairsRepository;
import com.agms.backend.repository.StudentRepository;
import com.agms.backend.repository.SubmissionRepository;
import com.agms.backend.service.SubmissionService;
import com.agms.backend.service.UbysService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final DepartmentListRepository departmentListRepository;
    private final FacultyListRepository facultyListRepository;
    private final GraduationListRepository graduationListRepository;
    private final GraduationRepository graduationRepository;
    private final com.agms.backend.repository.UserRepository userRepository;
    private final UbysService ubysService;

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
                .submissionDate(new Timestamp(System.currentTimeMillis()))
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

        // Extract role from authorities and strip ROLE_ prefix
        String role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No valid role found for current user"));
        
        // Remove ROLE_ prefix if present
        if (role.startsWith("ROLE_")) {
            return role.substring(5); // Remove "ROLE_" prefix
        }
        return role;
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
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }

        return authentication.getName();
    }

    @Override
    @Transactional
    public List<SubmissionResponse> startRegularGraduation(String term) {
        log.info("Starting regular graduation process for term: {}", term);

        // Verify that the current user is Student Affairs
        String userRole = getCurrentUserRole();
        if (!"STUDENT_AFFAIRS".equals(userRole)) {
            throw new IllegalStateException("Only Student Affairs can start regular graduation process");
        }

        // Check if regular graduation has already been started for this term
        if (graduationRepository.existsByTermAndStatus(term, "IN_PROGRESS")) {
            throw new IllegalStateException("Regular graduation process has already been started for term: " + term);
        }

        // Create graduation hierarchy if it doesn't exist for this term
        // This is done in a separate method to ensure it's completed before processing students
        ensureGraduationHierarchyExists(term);
        
        // Force flush to ensure graduation hierarchy is persisted before processing students
        graduationRepository.flush();

        // Get all students from the database
        List<Student> allStudents = studentRepository.findAll();
        List<SubmissionResponse> createdSubmissions = new ArrayList<>();
        int eligibleCount = 0;
        int skippedCount = 0;

        for (Student student : allStudents) {
            try {
                // Get the student with academic data from UBYS
                Student enhancedStudent = ubysService.getStudentWithTransientAttributes(student.getStudentNumber());
                
                // Check if student is eligible for graduation
                if (enhancedStudent.isEligibleForGraduation()) {
                    // Check if student already has a pending submission (skip if they do)
                    if (hasActivePendingSubmission(student.getStudentNumber())) {
                        log.debug("Skipping student {} - already has active pending submission", student.getStudentNumber());
                        skippedCount++;
                        continue;
                    }

                    // Verify the student has an advisor
                    if (student.getAdvisor() == null) {
                        log.warn("Skipping student {} - no assigned advisor", student.getStudentNumber());
                        skippedCount++;
                        continue;
                    }

                    // Verify the advisor has an advisor list
                    AdvisorList advisorList = student.getAdvisor().getAdvisorList();
                    if (advisorList == null) {
                        log.warn("Skipping student {} - advisor {} has no advisor list", 
                            student.getStudentNumber(), student.getAdvisor().getEmpId());
                        skippedCount++;
                        continue;
                    }

                    log.debug("Creating submission for student {} assigned to advisor {} with advisor list {}", 
                        student.getStudentNumber(), student.getAdvisor().getEmpId(), advisorList.getAdvisorListId());

                    // Create the submission for this eligible student
                    String submissionId = generateSubmissionId();
                    String submissionContent = String.format(
                        "Regular graduation application for %s term. " +
                        "GPA: %.2f, Total Credits: %d, Curriculum Completed: %s",
                        term, 
                        enhancedStudent.getGpa(),
                        enhancedStudent.getTotalCredit(),
                        enhancedStudent.isCurriculumCompleted() ? "Yes" : "No"
                    );

                    Submission submission = Submission.builder()
                            .submissionId(submissionId)
                            .submissionDate(new Timestamp(System.currentTimeMillis()))
                            .content(submissionContent)
                            .status(SubmissionStatus.PENDING)
                            .student(student)
                            .advisorList(advisorList)
                            .build();

                    // Save the submission
                    Submission savedSubmission = submissionRepository.save(submission);
                    createdSubmissions.add(convertToResponse(savedSubmission));
                    eligibleCount++;

                    log.debug("Created regular graduation submission for student: {} (GPA: {}, Credits: {})",
                            student.getStudentNumber(), enhancedStudent.getGpa(), enhancedStudent.getTotalCredit());

                } else {
                    log.debug("Student {} is not eligible for graduation", student.getStudentNumber());
                }

            } catch (Exception e) {
                log.warn("Error processing student {} for regular graduation: {}", 
                    student.getStudentNumber(), e.getMessage());
                skippedCount++;
            }
        }

        // Auto-finalize advisor lists that have no submissions
        autoFinalizeEmptyAdvisorLists();

        log.info("Regular graduation process completed for term: {}. Created {} submissions, skipped {} students",
            term, eligibleCount, skippedCount);

        return createdSubmissions;
    }

    @Override
    public RegularGraduationTrackResponse trackRegularGraduation(String term) {
        log.debug("Tracking regular graduation process for term: {}", term);

        Optional<com.agms.backend.model.Graduation> graduationOpt = graduationRepository.findByTerm(term);
        
        if (graduationOpt.isPresent()) {
            com.agms.backend.model.Graduation graduation = graduationOpt.get();
            return RegularGraduationTrackResponse.builder()
                    .isStarted(true)
                    .term(graduation.getTerm())
                    .status(graduation.getStatus())
                    .graduationId(graduation.getGraduationId())
                    .requestDate(graduation.getRequestDate())
                    .studentAffairsEmpId(graduation.getStudentAffairs().getEmpId())
                    .build();
        } else {
            return RegularGraduationTrackResponse.builder()
                    .isStarted(false)
                    .term(term)
                    .status(null)
                    .graduationId(null)
                    .requestDate(null)
                    .studentAffairsEmpId(null)
                    .build();
        }
    }

    /**
     * Ensure graduation hierarchy exists for a specific term
     */
    private void ensureGraduationHierarchyExists(String term) {
        log.debug("Ensuring graduation hierarchy exists for term: {}", term);
        
        // Check if graduation already exists for this term
        Optional<com.agms.backend.model.Graduation> existingGraduation = graduationRepository.findByTerm(term);
        if (existingGraduation.isPresent()) {
            log.debug("Graduation hierarchy already exists for term: {}", term);
            return;
        }
        
        // Create the hierarchy
        createGraduationHierarchyForTerm(term);
    }

    /**
     * Create graduation hierarchy for a specific term if it doesn't exist
     */
    private void createGraduationHierarchyForTerm(String term) {
        log.debug("Creating graduation hierarchy for term: {}", term);

        // Check if graduation already exists for this term (double-check to prevent race conditions)
        Optional<com.agms.backend.model.Graduation> existingGraduation = graduationRepository.findByTerm(term);
        if (existingGraduation.isPresent()) {
            log.debug("Graduation hierarchy already exists for term: {}", term);
            return;
        }

        // Double-check with the specific status to be extra safe
        if (graduationRepository.existsByTermAndStatus(term, "IN_PROGRESS")) {
            log.debug("Graduation with IN_PROGRESS status already exists for term: {}", term);
            return;
        }

        // Get current Student Affairs user
        String currentUserEmpId = getCurrentUserEmpId();
        StudentAffairs studentAffairs = studentAffairsRepository.findByEmpId(currentUserEmpId)
                .orElseThrow(() -> new ResourceNotFoundException("Student Affairs not found with empId: " + currentUserEmpId));

        // Create graduation object with try-catch for constraint violations
        String graduationId = "GRAD_" + term.replace(" ", "_").replace("-", "_").toUpperCase();
        com.agms.backend.model.Graduation graduation;
        
        try {
            graduation = com.agms.backend.model.Graduation.builder()
                    .graduationId(graduationId)
                    .requestDate(new Timestamp(System.currentTimeMillis()))
                    .term(term)
                    .status("IN_PROGRESS")
                    .studentAffairs(studentAffairs)
                    .build();
            graduation = graduationRepository.save(graduation);
            
            log.info("Created graduation object with ID: {} for term: {}", graduationId, term);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // If graduation already exists (race condition), find and use existing
            log.warn("Graduation object already exists for term {}, using existing one", term);
            Optional<com.agms.backend.model.Graduation> existingGrad = graduationRepository.findByTerm(term);
            if (existingGrad.isPresent()) {
                log.info("Using existing graduation object for term: {}", term);
                return; // Exit early since hierarchy already exists
            } else {
                throw new RuntimeException("Failed to create or find graduation for term: " + term, e);
            }
        } catch (Exception e) {
            log.error("Unexpected error creating graduation for term {}: {}", term, e.getMessage());
            throw e;
        }

        // Check if graduation list already exists (from DataInitializer)
        List<com.agms.backend.model.GraduationList> existingGraduationLists = graduationListRepository.findAll();
        com.agms.backend.model.GraduationList graduationList = null;
        
        if (!existingGraduationLists.isEmpty()) {
            // Reuse existing graduation list and update its graduation reference
            graduationList = existingGraduationLists.get(0);
            graduationList.setGraduation(graduation);
            graduationList = graduationListRepository.save(graduationList);
            log.info("Reusing existing graduation list: {} for term: {}", graduationList.getListId(), term);
        } else {
            // Create new graduation list
            String graduationListId = "GL_" + graduationId;
            graduationList = com.agms.backend.model.GraduationList.builder()
                    .listId(graduationListId)
                    .creationDate(new Timestamp(System.currentTimeMillis()))
                    .graduation(graduation)
                    .build();
            graduationList = graduationListRepository.save(graduationList);
            log.info("Created new graduation list: {} for term: {}", graduationListId, term);
        }

        // Check if faculty lists already exist (from DataInitializer)
        List<FacultyList> existingFacultyLists = facultyListRepository.findAll();
        
        if (!existingFacultyLists.isEmpty()) {
            // Reuse existing faculty lists and update their graduation list reference
            for (FacultyList existingFacultyList : existingFacultyLists) {
                existingFacultyList.setGraduationList(graduationList);
                facultyListRepository.save(existingFacultyList);
                log.info("Reusing existing faculty list: {} for faculty: {}", 
                    existingFacultyList.getFacultyListId(), existingFacultyList.getFaculty());
            }
        } else {
            // Create new faculty lists for each dean officer
            List<DeanOfficer> deanOfficers = deanOfficerRepository.findAll();
            for (DeanOfficer deanOfficer : deanOfficers) {
                String facultyListId = "FL_" + graduationId + "_" + deanOfficer.getEmpId();
                FacultyList facultyList = FacultyList.builder()
                        .facultyListId(facultyListId)
                        .creationDate(new Timestamp(System.currentTimeMillis()))
                        .faculty(deanOfficer.getFaculty())
                        .deanOfficer(deanOfficer)
                        .graduationList(graduationList)
                        .build();
                facultyList = facultyListRepository.save(facultyList);

                // Create department lists for each department secretary under this dean officer
                List<DepartmentSecretary> departmentSecretaries = departmentSecretaryRepository.findByDeanOfficerEmpId(deanOfficer.getEmpId());
                for (DepartmentSecretary departmentSecretary : departmentSecretaries) {
                    String departmentListId = "DL_" + graduationId + "_" + departmentSecretary.getEmpId();
                    DepartmentList departmentList = DepartmentList.builder()
                            .deptListId(departmentListId)
                            .creationDate(new Timestamp(System.currentTimeMillis()))
                            .department(departmentSecretary.getDepartment())
                            .secretary(departmentSecretary)
                            .facultyList(facultyList)
                            .build();
                    departmentList = departmentListRepository.save(departmentList);

                    // Create advisor lists for each advisor under this department secretary
                    List<Advisor> advisors = advisorRepository.findByDepartmentSecretaryEmpId(departmentSecretary.getEmpId());
                    for (Advisor advisor : advisors) {
                        // Check if advisor already has an advisor list
                        if (advisor.getAdvisorList() == null) {
                            // Check if there's an existing advisor list for this advisor (from DataInitializer)
                            Optional<AdvisorList> existingAdvisorListOpt = advisorListRepository.findByAdvisorEmpId(advisor.getEmpId());
                            
                            if (existingAdvisorListOpt.isPresent()) {
                                // Reuse existing advisor list and update its department list reference
                                AdvisorList existingAdvisorList = existingAdvisorListOpt.get();
                                existingAdvisorList.setDepartmentList(departmentList);
                                existingAdvisorList = advisorListRepository.save(existingAdvisorList);
                                
                                // Set the bidirectional relationship
                                advisor.setAdvisorList(existingAdvisorList);
                                advisorRepository.save(advisor);
                                
                                log.info("Reusing existing advisor list: {} for advisor: {}", 
                                    existingAdvisorList.getAdvisorListId(), advisor.getEmpId());
                            } else {
                                // Create new advisor list
                                String advisorListId = "AL_" + graduationId + "_" + advisor.getEmpId();
                                AdvisorList advisorList = AdvisorList.builder()
                                        .advisorListId(advisorListId)
                                        .creationDate(new Timestamp(System.currentTimeMillis()))
                                        .advisor(advisor)
                                        .departmentList(departmentList)
                                        .build();
                                advisorList = advisorListRepository.save(advisorList);

                                // Update advisor with the new advisor list
                                advisor.setAdvisorList(advisorList);
                                advisorRepository.save(advisor);
                                
                                log.info("Created new advisor list: {} for advisor: {}", advisorListId, advisor.getEmpId());
                            }
                        }
                    }
                }
            }
        }

        log.info("Created graduation hierarchy for term: {} with graduation ID: {}", term, graduationId);
    }

    /**
     * Automatically finalize advisor lists that have no submissions assigned to them.
     * This prevents the workflow from being blocked by advisors who have no students to review.
     */
    private void autoFinalizeEmptyAdvisorLists() {
        log.debug("Checking for empty advisor lists to auto-finalize...");
        
        List<AdvisorList> allAdvisorLists = advisorListRepository.findAll();
        int autoFinalizedCount = 0;
        
        for (AdvisorList advisorList : allAdvisorLists) {
            // Skip if already finalized
            if (advisorList.getIsFinalized()) {
                continue;
            }
            
            // Check if this advisor list has any submissions
            List<Submission> submissions = submissionRepository.findByAdvisorListId(advisorList.getAdvisorListId());
            
            if (submissions.isEmpty()) {
                // Auto-finalize empty advisor list
                int updated = advisorListRepository.updateFinalizationStatus(advisorList.getAdvisorListId(), true);
                if (updated > 0) {
                    autoFinalizedCount++;
                    log.info("Auto-finalized empty advisor list: {} (advisor: {})", 
                        advisorList.getAdvisorListId(), advisorList.getAdvisor().getEmpId());
                }
            }
        }
        
        if (autoFinalizedCount > 0) {
            log.info("Auto-finalized {} empty advisor lists", autoFinalizedCount);
        } else {
            log.debug("No empty advisor lists found to auto-finalize");
        }
    }

    @Override
    @Transactional
    public boolean finalizeMyList() {
        String userRole = getCurrentUserRole();
        String userEmpId = getCurrentUserEmpId();

        log.info("User with role {} and empId {} finalizing their list", userRole, userEmpId);

        switch (userRole) {
            case "ADVISOR":
                return finalizeAdvisorList(userEmpId);
            case "DEPARTMENT_SECRETARY":
                return finalizeDepartmentList(userEmpId);
            case "DEAN_OFFICER":
                return finalizeFacultyList(userEmpId);
            case "STUDENT_AFFAIRS":
                return finalizeGraduationList(userEmpId);
            default:
                throw new IllegalArgumentException("Role " + userRole + " cannot finalize lists");
        }
    }

    @Override
    public boolean isMyListFinalized() {
        String userRole = getCurrentUserRole();
        String userEmpId = getCurrentUserEmpId();

        log.debug("Checking if list is finalized for user with role {} and empId {}", userRole, userEmpId);

        switch (userRole) {
            case "ADVISOR":
                return advisorListRepository.findByAdvisorEmpId(userEmpId)
                    .map(AdvisorList::getIsFinalized)
                    .orElse(false);
            case "DEPARTMENT_SECRETARY":
                return departmentListRepository.findBySecretaryEmpId(userEmpId)
                    .map(DepartmentList::getIsFinalized)
                    .orElse(false);
            case "DEAN_OFFICER":
                return facultyListRepository.findByDeanOfficerEmpId(userEmpId)
                    .map(FacultyList::getIsFinalized)
                    .orElse(false);
            case "STUDENT_AFFAIRS":
                // For student affairs, check if any graduation list is finalized
                return graduationListRepository.findAll().stream()
                    .anyMatch(gl -> gl.getIsFinalized());
            default:
                throw new IllegalArgumentException("Role " + userRole + " does not have lists to check");
        }
    }

    @Override
    public boolean arePrerequisiteListsFinalized() {
        String userRole = getCurrentUserRole();
        String userEmpId = getCurrentUserEmpId();

        log.debug("Checking prerequisite lists finalization for user with role {} and empId {}", userRole, userEmpId);

        switch (userRole) {
            case "DEPARTMENT_SECRETARY":
                // Check if all advisor lists under this department are finalized
                DepartmentList departmentList = departmentListRepository.findBySecretaryEmpId(userEmpId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department list not found for secretary: " + userEmpId));
                
                List<AdvisorList> advisorLists = advisorListRepository.findByDepartmentListDeptListId(
                    departmentList.getDeptListId());
                
                return !advisorLists.isEmpty() && advisorLists.stream()
                    .allMatch(AdvisorList::getIsFinalized);

            case "DEAN_OFFICER":
                // Check if all department lists under this faculty are finalized
                FacultyList facultyList = facultyListRepository.findByDeanOfficerEmpId(userEmpId)
                    .orElseThrow(() -> new ResourceNotFoundException("Faculty list not found for dean officer: " + userEmpId));
                
                List<DepartmentList> departmentLists = departmentListRepository.findByFacultyListFacultyListId(
                    facultyList.getFacultyListId());
                
                return !departmentLists.isEmpty() && departmentLists.stream()
                    .allMatch(DepartmentList::getIsFinalized);

            case "STUDENT_AFFAIRS":
                // Check if all faculty lists are finalized
                List<FacultyList> facultyLists = facultyListRepository.findAll();
                
                return !facultyLists.isEmpty() && facultyLists.stream()
                    .allMatch(FacultyList::getIsFinalized);

            case "ADVISOR":
                // Advisors don't have prerequisite lists
                return true;

            default:
                throw new IllegalArgumentException("Role " + userRole + " does not have prerequisite lists to check");
        }
    }

    private boolean finalizeAdvisorList(String advisorEmpId) {
        AdvisorList advisorList = advisorListRepository.findByAdvisorEmpId(advisorEmpId)
            .orElseThrow(() -> new ResourceNotFoundException("Advisor list not found for advisor: " + advisorEmpId));

        log.debug("Attempting to finalize advisor list {} for advisor {}", advisorList.getAdvisorListId(), advisorEmpId);

        // Get all submissions assigned to this advisor list (regardless of status)
        List<Submission> allSubmissions = submissionRepository.findByAdvisorListId(advisorList.getAdvisorListId());
        log.debug("Found {} total submissions for advisor list {}", allSubmissions.size(), advisorList.getAdvisorListId());
        
        // Check if all submissions in this advisor list are processed (approved or rejected)
        List<Submission> pendingSubmissions = submissionRepository.findByAdvisorListIdAndStatus(
            advisorList.getAdvisorListId(), SubmissionStatus.PENDING);
        log.debug("Found {} pending submissions for advisor list {}", pendingSubmissions.size(), advisorList.getAdvisorListId());

        if (!pendingSubmissions.isEmpty()) {
            log.warn("Cannot finalize advisor list {} - {} pending submissions remain", 
                advisorList.getAdvisorListId(), pendingSubmissions.size());
            return false;
        }

        // If advisor has no submissions at all, they can finalize (nothing to process)
        if (allSubmissions.isEmpty()) {
            log.info("Advisor list {} has no submissions - allowing finalization", advisorList.getAdvisorListId());
        } else {
            log.info("Advisor list {} has {} total submissions, all processed - allowing finalization", 
                advisorList.getAdvisorListId(), allSubmissions.size());
        }

        int updated = advisorListRepository.updateFinalizationStatus(advisorList.getAdvisorListId(), true);
        log.info("Finalized advisor list {} for advisor {}", advisorList.getAdvisorListId(), advisorEmpId);
        return updated > 0;
    }

    private boolean finalizeDepartmentList(String secretaryEmpId) {
        DepartmentList departmentList = departmentListRepository.findBySecretaryEmpId(secretaryEmpId)
            .orElseThrow(() -> new ResourceNotFoundException("Department list not found for secretary: " + secretaryEmpId));

        // Check if all prerequisite advisor lists are finalized
        if (!arePrerequisiteListsFinalized()) {
            log.warn("Cannot finalize department list {} - not all advisor lists are finalized", 
                departmentList.getDeptListId());
            return false;
        }

        // Check if all submissions in this department are processed
        List<SubmissionResponse> pendingSubmissions = getSubmissionsForDepartmentSecretary(secretaryEmpId, SubmissionStatus.APPROVED_BY_ADVISOR);
        if (!pendingSubmissions.isEmpty()) {
            log.warn("Cannot finalize department list {} - {} pending submissions remain", 
                departmentList.getDeptListId(), pendingSubmissions.size());
            return false;
        }

        int updated = departmentListRepository.updateFinalizationStatus(departmentList.getDeptListId(), true);
        log.info("Finalized department list {} for secretary {}", departmentList.getDeptListId(), secretaryEmpId);
        return updated > 0;
    }

    private boolean finalizeFacultyList(String deanOfficerEmpId) {
        FacultyList facultyList = facultyListRepository.findByDeanOfficerEmpId(deanOfficerEmpId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty list not found for dean officer: " + deanOfficerEmpId));

        // Check if all prerequisite department lists are finalized
        if (!arePrerequisiteListsFinalized()) {
            log.warn("Cannot finalize faculty list {} - not all department lists are finalized", 
                facultyList.getFacultyListId());
            return false;
        }

        // Check if all submissions in this faculty are processed
        List<SubmissionResponse> pendingSubmissions = getSubmissionsForDeanOfficer(deanOfficerEmpId, SubmissionStatus.APPROVED_BY_DEPT);
        if (!pendingSubmissions.isEmpty()) {
            log.warn("Cannot finalize faculty list {} - {} pending submissions remain", 
                facultyList.getFacultyListId(), pendingSubmissions.size());
            return false;
        }

        int updated = facultyListRepository.updateFinalizationStatus(facultyList.getFacultyListId(), true);
        log.info("Finalized faculty list {} for dean officer {}", facultyList.getFacultyListId(), deanOfficerEmpId);
        return updated > 0;
    }

    private boolean finalizeGraduationList(String studentAffairsEmpId) {
        // Check if all prerequisite faculty lists are finalized
        if (!arePrerequisiteListsFinalized()) {
            log.warn("Cannot finalize graduation list - not all faculty lists are finalized");
            return false;
        }

        // Check if all submissions are processed
        List<SubmissionResponse> pendingSubmissions = getSubmissionsForStudentAffairs(studentAffairsEmpId, SubmissionStatus.APPROVED_BY_DEAN);
        if (!pendingSubmissions.isEmpty()) {
            log.warn("Cannot finalize graduation list - {} pending submissions remain", pendingSubmissions.size());
            return false;
        }

        // Find the graduation list to finalize
        List<com.agms.backend.model.GraduationList> graduationLists = graduationListRepository.findAll();
        if (graduationLists.isEmpty()) {
            log.warn("No graduation list found to finalize");
            return false;
        }

        com.agms.backend.model.GraduationList graduationList = graduationLists.get(0);
        int updated = graduationListRepository.updateFinalizationStatus(graduationList.getListId(), true);
        
        if (updated > 0) {
            log.info("Finalized graduation list {} - graduation process completed", graduationList.getListId());
            // Here you could create the final graduation object or trigger additional workflow
            createFinalGraduationRecord(graduationList);
        }
        
        return updated > 0;
    }

    private void createFinalGraduationRecord(com.agms.backend.model.GraduationList graduationList) {
        log.info("Creating final graduation record for graduation list: {}", graduationList.getListId());
        
        try {
            // Get the graduation associated with this list
            com.agms.backend.model.Graduation graduation = graduationList.getGraduation();
            if (graduation == null) {
                log.error("No graduation found for graduation list: {}", graduationList.getListId());
                return;
            }

            // Get all approved submissions from all advisor lists in the hierarchy
            List<Submission> approvedSubmissions = getAllApprovedSubmissions(graduationList);
            
            log.info("Found {} approved submissions for graduation", approvedSubmissions.size());

            // Update graduation status to completed
            graduation.setStatus("COMPLETED");
            graduationRepository.save(graduation);

            log.info("Final graduation process completed successfully:");
            log.info("- Graduation ID: {}", graduation.getGraduationId());
            log.info("- Term: {}", graduation.getTerm());
            log.info("- Status: {}", graduation.getStatus());
            log.info("- Total approved submissions: {}", approvedSubmissions.size());
            log.info("- Graduation list finalized: {}", graduationList.getListId());

            // Send completion notifications
            sendGraduationCompletionNotifications(graduation, approvedSubmissions);

        } catch (Exception e) {
            log.error("Error creating final graduation record: {}", e.getMessage());
            throw new RuntimeException("Failed to create final graduation record", e);
        }
    }

    private List<Submission> getAllApprovedSubmissions(com.agms.backend.model.GraduationList graduationList) {
        List<Submission> approvedSubmissions = new ArrayList<>();
        
        // Get all faculty lists under this graduation list
        List<FacultyList> facultyLists = facultyListRepository.findByGraduationListListId(graduationList.getListId());
        
        for (FacultyList facultyList : facultyLists) {
            // Get all department lists under each faculty
            List<DepartmentList> departmentLists = departmentListRepository.findByFacultyListFacultyListId(
                facultyList.getFacultyListId());
            
            for (DepartmentList departmentList : departmentLists) {
                // Get all advisor lists under each department
                List<AdvisorList> advisorLists = advisorListRepository.findByDepartmentListDeptListId(
                    departmentList.getDeptListId());
                
                for (AdvisorList advisorList : advisorLists) {
                    // Get all finally approved submissions from each advisor list
                    List<Submission> submissions = submissionRepository.findByAdvisorListIdAndStatus(
                        advisorList.getAdvisorListId(), SubmissionStatus.FINAL_APPROVED);
                    approvedSubmissions.addAll(submissions);
                }
            }
        }
        
        return approvedSubmissions;
    }

    private void sendGraduationCompletionNotifications(com.agms.backend.model.Graduation graduation, 
            List<Submission> approvedSubmissions) {
        log.info("Sending graduation completion notifications for {} approved submissions", approvedSubmissions.size());
        
        // In a real system, this would:
        // 1. Send email notifications to all graduated students
        // 2. Send summary report to Student Affairs
        // 3. Send notifications to advisors, department secretaries, dean officers
        // 4. Update external systems (registrar, alumni database, etc.)
        // 5. Generate graduation ceremony lists
        
        // For now, we'll just log the notifications
        for (Submission submission : approvedSubmissions) {
            Student student = submission.getStudent();
            log.info("📧 Notification sent to student: {} ({}) - GRADUATION COMPLETED", 
                student.getEmail(), student.getStudentNumber());
        }
        
        log.info("📧 Summary report sent to Student Affairs for graduation: {}", graduation.getGraduationId());
        log.info("🎓 Graduation process fully completed for term: {}", graduation.getTerm());
    }

    @Override
    public List<SubordinateStatusResponse> getSubordinateFinalizationStatus() {
        String userRole = getCurrentUserRole();
        String userEmpId = getCurrentUserEmpId();

        log.debug("Getting subordinate finalization status for user with role {} and empId {}", userRole, userEmpId);

        switch (userRole) {
            case "DEPARTMENT_SECRETARY":
                return getAdvisorFinalizationStatusForDepartmentSecretary(userEmpId);
            case "DEAN_OFFICER":
                return getDepartmentSecretaryFinalizationStatusForDeanOfficer(userEmpId);
            case "STUDENT_AFFAIRS":
                return getDeanOfficerFinalizationStatusForStudentAffairs();
            default:
                throw new IllegalArgumentException("Role " + userRole + " does not have subordinates to check");
        }
    }

    private List<SubordinateStatusResponse> getAdvisorFinalizationStatusForDepartmentSecretary(String secretaryEmpId) {
        log.debug("Getting advisor finalization status for department secretary: {}", secretaryEmpId);

        // Find the department list for this secretary
        DepartmentList departmentList = departmentListRepository.findBySecretaryEmpId(secretaryEmpId)
            .orElseThrow(() -> new ResourceNotFoundException("Department list not found for secretary: " + secretaryEmpId));

        // Get all advisor lists under this department
        List<AdvisorList> advisorLists = advisorListRepository.findByDepartmentListDeptListId(
            departmentList.getDeptListId());

        List<SubordinateStatusResponse> responses = new ArrayList<>();
        for (AdvisorList advisorList : advisorLists) {
            Advisor advisor = advisorList.getAdvisor();
            
            SubordinateStatusResponse response = SubordinateStatusResponse.builder()
                .empId(advisor.getEmpId())
                .name(advisor.getFirstName() + " " + advisor.getLastName())
                .email(advisor.getEmail())
                .department(advisor.getDepartment())
                .isFinalized(advisorList.getIsFinalized())
                .listId(advisorList.getAdvisorListId())
                .role("ADVISOR")
                .build();
            
            responses.add(response);
        }

        log.debug("Found {} advisors under department secretary {}", responses.size(), secretaryEmpId);
        return responses;
    }

    private List<SubordinateStatusResponse> getDepartmentSecretaryFinalizationStatusForDeanOfficer(String deanOfficerEmpId) {
        log.debug("Getting department secretary finalization status for dean officer: {}", deanOfficerEmpId);

        // Find the faculty list for this dean officer
        FacultyList facultyList = facultyListRepository.findByDeanOfficerEmpId(deanOfficerEmpId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty list not found for dean officer: " + deanOfficerEmpId));

        // Get all department lists under this faculty
        List<DepartmentList> departmentLists = departmentListRepository.findByFacultyListFacultyListId(
            facultyList.getFacultyListId());

        List<SubordinateStatusResponse> responses = new ArrayList<>();
        for (DepartmentList departmentList : departmentLists) {
            DepartmentSecretary secretary = departmentList.getSecretary();
            
            SubordinateStatusResponse response = SubordinateStatusResponse.builder()
                .empId(secretary.getEmpId())
                .name(secretary.getFirstName() + " " + secretary.getLastName())
                .email(secretary.getEmail())
                .department(secretary.getDepartment())
                .faculty(facultyList.getFaculty())
                .isFinalized(departmentList.getIsFinalized())
                .listId(departmentList.getDeptListId())
                .role("DEPARTMENT_SECRETARY")
                .build();
            
            responses.add(response);
        }

        log.debug("Found {} department secretaries under dean officer {}", responses.size(), deanOfficerEmpId);
        return responses;
    }

    private List<SubordinateStatusResponse> getDeanOfficerFinalizationStatusForStudentAffairs() {
        log.debug("Getting dean officer finalization status for student affairs");

        // Get all faculty lists (each represents a dean officer)
        List<FacultyList> facultyLists = facultyListRepository.findAll();

        List<SubordinateStatusResponse> responses = new ArrayList<>();
        for (FacultyList facultyList : facultyLists) {
            DeanOfficer deanOfficer = facultyList.getDeanOfficer();
            
            SubordinateStatusResponse response = SubordinateStatusResponse.builder()
                .empId(deanOfficer.getEmpId())
                .name(deanOfficer.getFirstName() + " " + deanOfficer.getLastName())
                .email(deanOfficer.getEmail())
                .faculty(deanOfficer.getFaculty())
                .isFinalized(facultyList.getIsFinalized())
                .listId(facultyList.getFacultyListId())
                .role("DEAN_OFFICER")
                .build();
            
            responses.add(response);
        }

        log.debug("Found {} dean officers for student affairs", responses.size());
        return responses;
    }

    @Override
    public TopStudentsResponse getTopStudentsFromFinalizedLists() {
        String userRole = getCurrentUserRole();
        String userEmpId = getCurrentUserEmpId();

        log.debug("Getting top students from finalized lists for user with role {} and empId {}", userRole, userEmpId);

        switch (userRole) {
            case "DEPARTMENT_SECRETARY":
                return getTopStudentsForDepartmentSecretary(userEmpId);
            case "DEAN_OFFICER":
                return getTopStudentsForDeanOfficer(userEmpId);
            case "STUDENT_AFFAIRS":
                return getTopStudentsForStudentAffairs();
            default:
                throw new IllegalArgumentException("Role " + userRole + " does not have access to top students data");
        }
    }

    private TopStudentsResponse getTopStudentsForDepartmentSecretary(String secretaryEmpId) {
        log.debug("Getting top 3 students for department secretary: {}", secretaryEmpId);

        // Find the department list for this secretary
        DepartmentList departmentList = departmentListRepository.findBySecretaryEmpId(secretaryEmpId)
            .orElseThrow(() -> new ResourceNotFoundException("Department list not found for secretary: " + secretaryEmpId));

        // Check if department list is finalized
        if (!departmentList.getIsFinalized()) {
            log.warn("Department list {} is not finalized yet", departmentList.getDeptListId());
            return TopStudentsResponse.builder()
                .topStudents(List.of())
                .topStudentsFromDepartments(List.of())
                .topStudentsFromFaculties(List.of())
                .build();
        }

        // Get all advisor lists under this department
        List<AdvisorList> advisorLists = advisorListRepository.findByDepartmentListDeptListId(
            departmentList.getDeptListId());

        // Get all finalized advisor lists
        List<AdvisorList> finalizedAdvisorLists = advisorLists.stream()
            .filter(AdvisorList::getIsFinalized)
            .collect(Collectors.toList());

        // Get top 3 students from this department
        List<TopStudentsResponse.TopStudentInfo> topStudents = getTopStudentsFromAdvisorLists(finalizedAdvisorLists, 3);

        return TopStudentsResponse.builder()
            .topStudents(topStudents)
            .topStudentsFromDepartments(List.of())
            .topStudentsFromFaculties(List.of())
            .build();
    }

    private TopStudentsResponse getTopStudentsForDeanOfficer(String deanOfficerEmpId) {
        log.debug("Getting top students and departments for dean officer: {}", deanOfficerEmpId);

        // Find the faculty list for this dean officer
        FacultyList facultyList = facultyListRepository.findByDeanOfficerEmpId(deanOfficerEmpId)
            .orElseThrow(() -> new ResourceNotFoundException("Faculty list not found for dean officer: " + deanOfficerEmpId));

        // Check if faculty list is finalized
        if (!facultyList.getIsFinalized()) {
            log.warn("Faculty list {} is not finalized yet", facultyList.getFacultyListId());
            return TopStudentsResponse.builder()
                .topStudents(List.of())
                .topStudentsFromDepartments(List.of())
                .topStudentsFromFaculties(List.of())
                .build();
        }

        // Get all department lists under this faculty
        List<DepartmentList> departmentLists = departmentListRepository.findByFacultyListFacultyListId(
            facultyList.getFacultyListId());

        // Get all finalized department lists
        List<DepartmentList> finalizedDepartmentLists = departmentLists.stream()
            .filter(DepartmentList::getIsFinalized)
            .collect(Collectors.toList());

        // Get all advisor lists from finalized departments
        List<AdvisorList> allAdvisorLists = new ArrayList<>();
        for (DepartmentList deptList : finalizedDepartmentLists) {
            List<AdvisorList> advisorLists = advisorListRepository.findByDepartmentListDeptListId(deptList.getDeptListId());
            List<AdvisorList> finalizedAdvisorLists = advisorLists.stream()
                .filter(AdvisorList::getIsFinalized)
                .collect(Collectors.toList());
            allAdvisorLists.addAll(finalizedAdvisorLists);
        }

        // Get top 3 students from all departments in this faculty (overall top 3)
        List<TopStudentsResponse.TopStudentInfo> topStudents = getTopStudentsFromAdvisorLists(allAdvisorLists, 3);

        // Get top 3 students from each department in this faculty
        List<TopStudentsResponse.TopDepartmentInfo> topStudentsFromDepartments = getTopStudentsFromEachDepartment(finalizedDepartmentLists, 3);

        return TopStudentsResponse.builder()
            .topStudents(topStudents)
            .topStudentsFromDepartments(topStudentsFromDepartments)
            .topStudentsFromFaculties(List.of())
            .build();
    }

    private TopStudentsResponse getTopStudentsForStudentAffairs() {
        log.debug("Getting top students, departments, and faculties for student affairs");

        // Get all faculty lists
        List<FacultyList> facultyLists = facultyListRepository.findAll();

        // Get all finalized faculty lists
        List<FacultyList> finalizedFacultyLists = facultyLists.stream()
            .filter(FacultyList::getIsFinalized)
            .collect(Collectors.toList());

        // Get all department lists from finalized faculties
        List<DepartmentList> allDepartmentLists = new ArrayList<>();
        for (FacultyList facultyList : finalizedFacultyLists) {
            List<DepartmentList> departmentLists = departmentListRepository.findByFacultyListFacultyListId(
                facultyList.getFacultyListId());
            List<DepartmentList> finalizedDepartmentLists = departmentLists.stream()
                .filter(DepartmentList::getIsFinalized)
                .collect(Collectors.toList());
            allDepartmentLists.addAll(finalizedDepartmentLists);
        }

        // Get all advisor lists from finalized departments
        List<AdvisorList> allAdvisorLists = new ArrayList<>();
        for (DepartmentList deptList : allDepartmentLists) {
            List<AdvisorList> advisorLists = advisorListRepository.findByDepartmentListDeptListId(deptList.getDeptListId());
            List<AdvisorList> finalizedAdvisorLists = advisorLists.stream()
                .filter(AdvisorList::getIsFinalized)
                .collect(Collectors.toList());
            allAdvisorLists.addAll(finalizedAdvisorLists);
        }

        // Get top 3 students from all faculties (overall top 3)
        List<TopStudentsResponse.TopStudentInfo> topStudents = getTopStudentsFromAdvisorLists(allAdvisorLists, 3);

        // Get top 3 students from each department across all faculties
        List<TopStudentsResponse.TopDepartmentInfo> topStudentsFromDepartments = getTopStudentsFromEachDepartment(allDepartmentLists, 3);

        // Get top 3 students from each faculty
        List<TopStudentsResponse.TopFacultyInfo> topStudentsFromFaculties = getTopStudentsFromEachFaculty(finalizedFacultyLists, 3);

        return TopStudentsResponse.builder()
            .topStudents(topStudents)
            .topStudentsFromDepartments(topStudentsFromDepartments)
            .topStudentsFromFaculties(topStudentsFromFaculties)
            .build();
    }

    private List<TopStudentsResponse.TopStudentInfo> getTopStudentsFromAdvisorLists(List<AdvisorList> advisorLists, int limit) {
        List<TopStudentsResponse.TopStudentInfo> studentInfos = new ArrayList<>();

        for (AdvisorList advisorList : advisorLists) {
            // Get all finally approved submissions from this advisor list
            List<Submission> approvedSubmissions = submissionRepository.findByAdvisorListIdAndStatus(
                advisorList.getAdvisorListId(), SubmissionStatus.FINAL_APPROVED);

            for (Submission submission : approvedSubmissions) {
                Student student = submission.getStudent();
                
                try {
                    // Get enhanced student data with GPA from ubys service
                    Student enhancedStudent = ubysService.getStudentWithTransientAttributes(student.getStudentNumber());
                    
                    // Only include students with valid GPA data
                    if (enhancedStudent.getGpa() > 0) {
                        Advisor advisor = advisorList.getAdvisor();
                        String advisorName = advisor.getFirstName() + " " + advisor.getLastName();
                        
                        TopStudentsResponse.TopStudentInfo studentInfo = TopStudentsResponse.TopStudentInfo.builder()
                            .studentNumber(student.getStudentNumber())
                            .firstName(student.getFirstName())
                            .lastName(student.getLastName())
                            .email(student.getEmail())
                            .department(student.getDepartment())
                            .faculty(getFacultyForStudent(student))
                            .gpa(enhancedStudent.getGpa())
                            .totalCredits(enhancedStudent.getTotalCredit())
                            .semester(enhancedStudent.getSemester())
                            .advisorName(advisorName)
                            .advisorEmpId(advisor.getEmpId())
                            .build();
                        
                        studentInfos.add(studentInfo);
                    }
                } catch (Exception e) {
                    log.warn("Could not get enhanced data for student {}: {}", student.getStudentNumber(), e.getMessage());
                }
            }
        }

        // Sort by semester ascending (earliest graduation first), then by GPA descending as tiebreaker
        // Priority: Lower semester number = earlier graduation = higher rank
        // Tiebreaker: Higher GPA = higher rank
        studentInfos.sort(
            Comparator.comparing((TopStudentsResponse.TopStudentInfo student) -> student.getSemester())
                .thenComparing(TopStudentsResponse.TopStudentInfo::getGpa, Comparator.reverseOrder())
        );
        
        // Assign ranks and limit to top N
        for (int i = 0; i < studentInfos.size() && i < limit; i++) {
            studentInfos.get(i).setRank(i + 1);
        }

        return studentInfos.stream().limit(limit).collect(Collectors.toList());
    }

    private List<TopStudentsResponse.TopDepartmentInfo> getTopStudentsFromEachDepartment(List<DepartmentList> departmentLists, int limit) {
        List<TopStudentsResponse.TopDepartmentInfo> departmentInfos = new ArrayList<>();
        
        for (DepartmentList departmentList : departmentLists) {
            String departmentName = departmentList.getSecretary().getDepartment();
            
            // Get all advisor lists under this department
            List<AdvisorList> advisorLists = advisorListRepository.findByDepartmentListDeptListId(
                departmentList.getDeptListId());
            
            List<AdvisorList> finalizedAdvisorLists = advisorLists.stream()
                .filter(AdvisorList::getIsFinalized)
                .collect(Collectors.toList());
            
            // Get top 3 students from this department
            List<TopStudentsResponse.TopStudentInfo> topStudents = getTopStudentsFromAdvisorLists(finalizedAdvisorLists, limit);
            
            if (!topStudents.isEmpty()) {
                double averageGpa = topStudents.stream()
                    .mapToDouble(TopStudentsResponse.TopStudentInfo::getGpa)
                    .average()
                    .orElse(0.0);
                
                String faculty = topStudents.get(0).getFaculty(); // All students in same department have same faculty
                
                TopStudentsResponse.TopDepartmentInfo departmentInfo = TopStudentsResponse.TopDepartmentInfo.builder()
                    .departmentName(departmentName)
                    .faculty(faculty)
                    .averageGpa(Math.round(averageGpa * 100.0) / 100.0)
                    .totalStudents(topStudents.size())
                    .rank(1) // Each department shows its own top students, no ranking between departments
                    .topStudents(topStudents)
                    .build();
                
                departmentInfos.add(departmentInfo);
            }
        }

        return departmentInfos;
    }

    private List<TopStudentsResponse.TopFacultyInfo> getTopStudentsFromEachFaculty(List<FacultyList> facultyLists, int limit) {
        List<TopStudentsResponse.TopFacultyInfo> facultyInfos = new ArrayList<>();
        
        for (FacultyList facultyList : facultyLists) {
            String facultyName = facultyList.getFaculty();
            
            // Get all department lists under this faculty
            List<DepartmentList> departmentLists = departmentListRepository.findByFacultyListFacultyListId(
                facultyList.getFacultyListId());
            
            List<DepartmentList> finalizedDepartmentLists = departmentLists.stream()
                .filter(DepartmentList::getIsFinalized)
                .collect(Collectors.toList());
            
            // Get all advisor lists from finalized departments in this faculty
            List<AdvisorList> allAdvisorLists = new ArrayList<>();
            for (DepartmentList deptList : finalizedDepartmentLists) {
                List<AdvisorList> advisorLists = advisorListRepository.findByDepartmentListDeptListId(deptList.getDeptListId());
                List<AdvisorList> finalizedAdvisorLists = advisorLists.stream()
                    .filter(AdvisorList::getIsFinalized)
                    .collect(Collectors.toList());
                allAdvisorLists.addAll(finalizedAdvisorLists);
            }
            
            // Get top 3 students from this faculty
            List<TopStudentsResponse.TopStudentInfo> topStudents = getTopStudentsFromAdvisorLists(allAdvisorLists, limit);
            
            if (!topStudents.isEmpty()) {
                double averageGpa = topStudents.stream()
                    .mapToDouble(TopStudentsResponse.TopStudentInfo::getGpa)
                    .average()
                    .orElse(0.0);
                
                // Get top 3 students from each department in this faculty
                List<TopStudentsResponse.TopDepartmentInfo> topDepartments = getTopStudentsFromEachDepartment(finalizedDepartmentLists, limit);
                
                TopStudentsResponse.TopFacultyInfo facultyInfo = TopStudentsResponse.TopFacultyInfo.builder()
                    .facultyName(facultyName)
                    .averageGpa(Math.round(averageGpa * 100.0) / 100.0)
                    .totalStudents(topStudents.size())
                    .totalDepartments(finalizedDepartmentLists.size())
                    .rank(1) // Each faculty shows its own top students, no ranking between faculties
                    .topStudentsFromDepartments(topDepartments)
                    .build();
                
                facultyInfos.add(facultyInfo);
            }
        }

        return facultyInfos;
    }

    private String getFacultyForStudent(Student student) {
        try {
            // Get the student's advisor
            Advisor advisor = student.getAdvisor();
            if (advisor == null) {
                return "Unknown";
            }
            
            // Get the advisor's advisor list
            AdvisorList advisorList = advisor.getAdvisorList();
            if (advisorList == null) {
                return "Unknown";
            }
            
            // Get the department list
            DepartmentList departmentList = advisorList.getDepartmentList();
            if (departmentList == null) {
                return "Unknown";
            }
            
            // Get the faculty list
            FacultyList facultyList = departmentList.getFacultyList();
            if (facultyList == null) {
                return "Unknown";
            }
            
            return facultyList.getFaculty();
        } catch (Exception e) {
            log.warn("Could not determine faculty for student {}: {}", student.getStudentNumber(), e.getMessage());
            return "Unknown";
        }
    }
}
