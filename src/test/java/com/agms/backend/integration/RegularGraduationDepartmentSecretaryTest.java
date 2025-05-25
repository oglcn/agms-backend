package com.agms.backend.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.agms.backend.dto.SubmissionResponse;
import com.agms.backend.model.SubmissionStatus;
import com.agms.backend.model.users.Advisor;
import com.agms.backend.model.users.DepartmentSecretary;
import com.agms.backend.model.users.StudentAffairs;
import com.agms.backend.repository.AdvisorRepository;
import com.agms.backend.repository.DepartmentSecretaryRepository;
import com.agms.backend.repository.StudentAffairsRepository;
import com.agms.backend.service.SubmissionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class RegularGraduationDepartmentSecretaryTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private StudentAffairsRepository studentAffairsRepository;

    @Autowired
    private AdvisorRepository advisorRepository;

    @Autowired
    private DepartmentSecretaryRepository departmentSecretaryRepository;

    @Test
    @Transactional
    void testDepartmentSecretarySeesOnlyTheirAdvisorsSubmissions() {
        log.info("=== Testing Department Secretary Multi-Department Isolation ===");

        // Get Student Affairs and start regular graduation
        StudentAffairs studentAffairs = studentAffairsRepository.findAll().get(0);
        setUpAuthenticationForStudentAffairs(studentAffairs);

        String term = "Spring 2025";
        List<SubmissionResponse> createdSubmissions = submissionService.startRegularGraduation(term);

        assertTrue(createdSubmissions.size() > 0, "Should have created submissions");
        log.info("Created {} submissions for regular graduation", createdSubmissions.size());

        // Get all department secretaries
        List<DepartmentSecretary> departmentSecretaries = departmentSecretaryRepository.findAll();
        assertTrue(departmentSecretaries.size() >= 2, "Should have at least 2 department secretaries");

        log.info("Found {} department secretaries to test", departmentSecretaries.size());

        // First, approve some submissions as advisors to make them visible to department secretaries
        approveSubmissionsFromDifferentAdvisors();

        // Test each department secretary's view
        for (int i = 0; i < Math.min(3, departmentSecretaries.size()); i++) {
            DepartmentSecretary depSec = departmentSecretaries.get(i);
            
            log.info("\n--- Testing Department Secretary {} ---", i + 1);
            log.info("Secretary: {} ({}) - Department: {}", 
                    depSec.getFirstName() + " " + depSec.getLastName(), 
                    depSec.getEmpId(),
                    getDepartmentFromEmpId(depSec.getEmpId()));

            // Authenticate as this department secretary
            setUpAuthenticationForDepartmentSecretary(depSec);

            // Get pending submissions for this department secretary
            List<SubmissionResponse> departmentSubmissions = submissionService.getMyPendingSubmissions();
            
            log.info("Department Secretary {} sees {} pending submissions", 
                    depSec.getEmpId(), departmentSubmissions.size());

            // Log details of submissions
            for (SubmissionResponse submission : departmentSubmissions) {
                assertEquals(SubmissionStatus.APPROVED_BY_ADVISOR, submission.getStatus(), 
                        "Department Secretary should only see submissions approved by advisors");
                log.info("  - Student: {} (Name: {}, Advisor List: {})", 
                        submission.getStudentNumber(), 
                        submission.getStudentName(),
                        submission.getAdvisorListId());
            }

            // Find advisors under this department secretary
            List<Advisor> advisorsUnderThisDepartment = advisorRepository.findByDepartmentSecretaryEmpId(depSec.getEmpId());
            log.info("This department has {} advisors under it", advisorsUnderThisDepartment.size());

            // Verify that all submissions belong to advisors under this department
            for (SubmissionResponse submission : departmentSubmissions) {
                boolean belongsToThisDepartment = advisorsUnderThisDepartment.stream()
                        .anyMatch(advisor -> advisor.getAdvisorList() != null && 
                                           advisor.getAdvisorList().getAdvisorListId().equals(submission.getAdvisorListId()));
                
                assertTrue(belongsToThisDepartment, 
                        "Submission " + submission.getSubmissionId() + 
                        " with advisor list " + submission.getAdvisorListId() + 
                        " should belong to an advisor under department secretary " + depSec.getEmpId());
            }

            log.info("✅ Verified: All {} submissions belong to advisors under this department", 
                    departmentSubmissions.size());
        }

        // Cross-verification: Ensure no overlap between different department secretaries
        testDepartmentSecretaryIsolation(departmentSecretaries);

        log.info("✅ Department Secretary multi-department test completed successfully");
    }

    @Test
    @Transactional
    void testDepartmentSecretaryWorkflowWithApprovalRejection() {
        log.info("=== Testing Department Secretary Approval/Rejection Workflow ===");

        // Setup and start regular graduation
        StudentAffairs studentAffairs = studentAffairsRepository.findAll().get(0);
        setUpAuthenticationForStudentAffairs(studentAffairs);

        String term = "Fall 2025";
        List<SubmissionResponse> createdSubmissions = submissionService.startRegularGraduation(term);
        assertTrue(createdSubmissions.size() > 0, "Should have created submissions");

        // Approve some submissions as advisors first
        approveSubmissionsFromDifferentAdvisors();

        // Get different department secretaries
        List<DepartmentSecretary> departmentSecretaries = departmentSecretaryRepository.findAll();
        assertTrue(departmentSecretaries.size() >= 2, "Should have at least 2 department secretaries");

        // Test workflow with first department secretary (approval)
        DepartmentSecretary depSec1 = departmentSecretaries.get(0);
        setUpAuthenticationForDepartmentSecretary(depSec1);
        
        List<SubmissionResponse> depSec1Submissions = submissionService.getMyPendingSubmissions();
        log.info("Department Secretary 1 ({}) has {} pending submissions", 
                depSec1.getEmpId(), depSec1Submissions.size());

        if (!depSec1Submissions.isEmpty()) {
            SubmissionResponse toApprove = depSec1Submissions.get(0);
            log.info("Department Secretary 1 approving submission for student: {}", 
                    toApprove.getStudentNumber());
            
            SubmissionResponse approved = submissionService.approveSubmission(toApprove.getSubmissionId());
            assertEquals(SubmissionStatus.APPROVED_BY_DEPT, approved.getStatus());
            
            // Verify it's no longer in department secretary's pending list
            List<SubmissionResponse> depSec1After = submissionService.getMyPendingSubmissions();
            assertFalse(depSec1After.stream()
                    .anyMatch(s -> s.getSubmissionId().equals(approved.getSubmissionId())),
                    "Approved submission should no longer be in department secretary's pending list");
            
            log.info("✅ Department Secretary 1 successfully approved submission");
        }

        // Test workflow with second department secretary (rejection)
        if (departmentSecretaries.size() > 1) {
            DepartmentSecretary depSec2 = departmentSecretaries.get(1);
            setUpAuthenticationForDepartmentSecretary(depSec2);
            
            List<SubmissionResponse> depSec2Submissions = submissionService.getMyPendingSubmissions();
            log.info("Department Secretary 2 ({}) has {} pending submissions", 
                    depSec2.getEmpId(), depSec2Submissions.size());

            if (!depSec2Submissions.isEmpty()) {
                SubmissionResponse toReject = depSec2Submissions.get(0);
                log.info("Department Secretary 2 rejecting submission for student: {}", 
                        toReject.getStudentNumber());
                
                String rejectionReason = "Insufficient course credits";
                SubmissionResponse rejected = submissionService.rejectSubmission(
                        toReject.getSubmissionId(), rejectionReason);
                assertEquals(SubmissionStatus.REJECTED_BY_DEPT, rejected.getStatus());
                
                log.info("✅ Department Secretary 2 successfully rejected submission with reason: {}", 
                        rejectionReason);
            }
        }

        log.info("✅ Department Secretary workflow test completed successfully");
    }

    private void approveSubmissionsFromDifferentAdvisors() {
        log.info("--- Approving submissions from different advisors ---");
        
        // Get several advisors from different departments
        List<Advisor> advisors = advisorRepository.findAll();
        
        // Approve submissions from first few advisors to make them visible to department secretaries
        for (int i = 0; i < Math.min(5, advisors.size()); i++) {
            Advisor advisor = advisors.get(i);
            setUpAuthenticationForAdvisor(advisor);
            
            List<SubmissionResponse> advisorSubmissions = submissionService.getMyPendingSubmissions();
            if (!advisorSubmissions.isEmpty()) {
                SubmissionResponse toApprove = advisorSubmissions.get(0);
                try {
                    SubmissionResponse approved = submissionService.approveSubmission(toApprove.getSubmissionId());
                    log.info("Advisor {} approved submission for student {}", 
                            advisor.getEmpId(), approved.getStudentNumber());
                } catch (Exception e) {
                    log.debug("Advisor {} could not approve submission: {}", advisor.getEmpId(), e.getMessage());
                }
            }
        }
    }

    private void testDepartmentSecretaryIsolation(List<DepartmentSecretary> departmentSecretaries) {
        log.info("\n--- Testing Department Secretary Isolation ---");
        
        if (departmentSecretaries.size() < 2) {
            log.warn("Not enough department secretaries to test isolation");
            return;
        }

        // Get submissions from first two department secretaries
        DepartmentSecretary depSec1 = departmentSecretaries.get(0);
        DepartmentSecretary depSec2 = departmentSecretaries.get(1);

        setUpAuthenticationForDepartmentSecretary(depSec1);
        List<SubmissionResponse> depSec1Submissions = submissionService.getMyPendingSubmissions();

        setUpAuthenticationForDepartmentSecretary(depSec2);
        List<SubmissionResponse> depSec2Submissions = submissionService.getMyPendingSubmissions();

        // Extract submission IDs
        List<String> depSec1SubmissionIds = depSec1Submissions.stream()
                .map(SubmissionResponse::getSubmissionId)
                .toList();

        List<String> depSec2SubmissionIds = depSec2Submissions.stream()
                .map(SubmissionResponse::getSubmissionId)
                .toList();

        // Verify no overlap
        for (String submissionId : depSec1SubmissionIds) {
            assertFalse(depSec2SubmissionIds.contains(submissionId),
                    "Submission " + submissionId + " should not appear in both department secretary views");
        }

        log.info("✅ Verified: No overlap between department secretary {} ({} submissions) and {} ({} submissions)", 
                depSec1.getEmpId(), depSec1Submissions.size(),
                depSec2.getEmpId(), depSec2Submissions.size());
    }

    private String getDepartmentFromEmpId(String empId) {
        // Extract department info from employee ID pattern (e.g., DS101 -> Computer Engineering)
        // This is based on the UBYS data structure
        switch (empId) {
            case "DS101":
            case "DS102":
            case "DS103":
                return "Computer Engineering";
            case "DS104":
            case "DS105":
            case "DS106":
                return "Electrical Engineering";
            case "DS107":
            case "DS108":
            case "DS109":
                return "Mechanical Engineering";
            case "DS110":
            case "DS111":
            case "DS112":
                return "Civil Engineering";
            case "DS113":
            case "DS114":
            case "DS115":
                return "Chemical Engineering";
            case "DS116":
            case "DS117":
            case "DS118":
                return "Industrial Engineering";
            default:
                return "Unknown Department";
        }
    }

    private void setUpAuthenticationForStudentAffairs(StudentAffairs studentAffairs) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                studentAffairs.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT_AFFAIRS"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Set authentication for Student Affairs: {}", studentAffairs.getEmail());
    }

    private void setUpAuthenticationForAdvisor(Advisor advisor) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                advisor.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADVISOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Set authentication for Advisor: {} ({})", advisor.getEmail(), advisor.getEmpId());
    }

    private void setUpAuthenticationForDepartmentSecretary(DepartmentSecretary departmentSecretary) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                departmentSecretary.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DEPARTMENT_SECRETARY"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Set authentication for Department Secretary: {} ({})", 
                departmentSecretary.getEmail(), departmentSecretary.getEmpId());
    }
} 