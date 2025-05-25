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
import com.agms.backend.model.users.StudentAffairs;
import com.agms.backend.repository.AdvisorRepository;
import com.agms.backend.repository.StudentAffairsRepository;
import com.agms.backend.service.SubmissionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class RegularGraduationWithExistingAdvisorsTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private StudentAffairsRepository studentAffairsRepository;

    @Autowired
    private AdvisorRepository advisorRepository;

    @Test
    @Transactional
    void testRegularGraduationWithExistingAdvisors() {
        log.info("=== Testing Regular Graduation with Existing Advisors ===");

        // Get Student Affairs (should exist from DataInitializer)
        StudentAffairs studentAffairs = studentAffairsRepository.findAll().get(0);
        assertNotNull(studentAffairs, "Student Affairs should exist from DataInitializer");

        // Set up Student Affairs authentication
        setUpAuthenticationForStudentAffairs(studentAffairs);

        // Start the regular graduation process
        String term = "Spring 2025";
        List<SubmissionResponse> createdSubmissions = submissionService.startRegularGraduation(term);

        // Verify submissions were created
        assertNotNull(createdSubmissions);
        assertTrue(createdSubmissions.size() > 0, "At least one submission should be created for eligible students");

        log.info("Created {} submissions for regular graduation", createdSubmissions.size());

        // Get a few existing advisors to test with
        List<Advisor> advisors = advisorRepository.findAll();
        assertTrue(advisors.size() >= 2, "Should have at least 2 advisors from DataInitializer");

        Advisor advisor1 = advisors.get(0);
        Advisor advisor2 = advisors.get(1);

        log.info("Testing with Advisor1: {} ({})", advisor1.getFirstName() + " " + advisor1.getLastName(), advisor1.getEmpId());
        log.info("Testing with Advisor2: {} ({})", advisor2.getFirstName() + " " + advisor2.getLastName(), advisor2.getEmpId());

        // Test Advisor 1's view
        setUpAuthenticationForAdvisor(advisor1);
        List<SubmissionResponse> advisor1Submissions = submissionService.getMyPendingSubmissions();

        log.info("Advisor1 ({}) sees {} pending submissions", advisor1.getEmpId(), advisor1Submissions.size());

        // Log details of advisor1's submissions
        for (SubmissionResponse submission : advisor1Submissions) {
            assertEquals(SubmissionStatus.PENDING, submission.getStatus());
            log.info("  - Student: {} (Name: {})", 
                    submission.getStudentNumber(), submission.getStudentName());
        }

        // Test Advisor 2's view
        setUpAuthenticationForAdvisor(advisor2);
        List<SubmissionResponse> advisor2Submissions = submissionService.getMyPendingSubmissions();

        log.info("Advisor2 ({}) sees {} pending submissions", advisor2.getEmpId(), advisor2Submissions.size());

        // Log details of advisor2's submissions
        for (SubmissionResponse submission : advisor2Submissions) {
            assertEquals(SubmissionStatus.PENDING, submission.getStatus());
            log.info("  - Student: {} (Name: {})", 
                    submission.getStudentNumber(), submission.getStudentName());
        }

        // Verify advisors see different students (no overlap in student numbers)
        if (!advisor1Submissions.isEmpty() && !advisor2Submissions.isEmpty()) {
            List<String> advisor1StudentNumbers = advisor1Submissions.stream()
                    .map(SubmissionResponse::getStudentNumber)
                    .toList();
            
            List<String> advisor2StudentNumbers = advisor2Submissions.stream()
                    .map(SubmissionResponse::getStudentNumber)
                    .toList();

            // Verify no overlap between advisor views
            for (String studentNumber : advisor1StudentNumbers) {
                assertFalse(advisor2StudentNumbers.contains(studentNumber),
                        "Student " + studentNumber + " should not appear in both advisor views");
            }

            log.info("✅ Verified: No overlap between advisor views - each advisor sees only their own students");
        }

        // Test approving a submission from advisor1 if available
        if (!advisor1Submissions.isEmpty()) {
            SubmissionResponse submissionToApprove = advisor1Submissions.get(0);
            log.info("Advisor1 approving submission for student: {}", submissionToApprove.getStudentNumber());
            
            SubmissionResponse approvedSubmission = submissionService.approveSubmission(submissionToApprove.getSubmissionId());
            assertEquals(SubmissionStatus.APPROVED_BY_ADVISOR, approvedSubmission.getStatus());
            
            // Verify it's no longer in advisor's pending list
            List<SubmissionResponse> advisor1SubmissionsAfter = submissionService.getMyPendingSubmissions();
            assertFalse(advisor1SubmissionsAfter.stream()
                    .anyMatch(s -> s.getSubmissionId().equals(approvedSubmission.getSubmissionId())),
                    "Approved submission should no longer be in advisor's pending list");
            
            log.info("✅ Verified: Approved submission is no longer in advisor's pending list");
        }

        // Summary
        int totalSubmissionsCreated = createdSubmissions.size();
        int advisor1Count = advisor1Submissions.size();
        int advisor2Count = advisor2Submissions.size();
        
        log.info("=== Test Summary ===");
        log.info("Total submissions created: {}", totalSubmissionsCreated);
        log.info("Advisor1 pending submissions: {}", advisor1Count);
        log.info("Advisor2 pending submissions: {}", advisor2Count);
        log.info("✅ Regular graduation with multiple advisors test completed successfully");
    }

    @Test
    @Transactional
    void testMultipleAdvisorWorkflowIntegration() {
        log.info("=== Testing Multiple Advisor Workflow Integration ===");

        // Get Student Affairs and start regular graduation
        StudentAffairs studentAffairs = studentAffairsRepository.findAll().get(0);
        setUpAuthenticationForStudentAffairs(studentAffairs);

        String term = "Fall 2025";
        List<SubmissionResponse> createdSubmissions = submissionService.startRegularGraduation(term);

        assertTrue(createdSubmissions.size() > 0, "Should have created submissions");
        log.info("Created {} submissions for term: {}", createdSubmissions.size(), term);

        // Get multiple advisors and test their independent workflows
        List<Advisor> advisors = advisorRepository.findAll();
        assertTrue(advisors.size() >= 3, "Should have at least 3 advisors for this test");

        // Test first 3 advisors to show independent workflows
        for (int i = 0; i < Math.min(3, advisors.size()); i++) {
            Advisor advisor = advisors.get(i);
            setUpAuthenticationForAdvisor(advisor);
            
            List<SubmissionResponse> pendingSubmissions = submissionService.getMyPendingSubmissions();
            log.info("Advisor {} ({}) has {} pending submissions", 
                    i + 1, advisor.getEmpId(), pendingSubmissions.size());

            // If this advisor has submissions, test approval workflow
            if (!pendingSubmissions.isEmpty()) {
                SubmissionResponse firstSubmission = pendingSubmissions.get(0);
                
                if (i == 0) {
                    // First advisor approves
                    SubmissionResponse approved = submissionService.approveSubmission(firstSubmission.getSubmissionId());
                    assertEquals(SubmissionStatus.APPROVED_BY_ADVISOR, approved.getStatus());
                    log.info("Advisor {} approved submission for student {}", 
                            i + 1, firstSubmission.getStudentNumber());
                } else if (i == 1) {
                    // Second advisor rejects
                    String rejectionReason = "Incomplete documentation";
                    SubmissionResponse rejected = submissionService.rejectSubmission(
                            firstSubmission.getSubmissionId(), rejectionReason);
                    assertEquals(SubmissionStatus.REJECTED_BY_ADVISOR, rejected.getStatus());
                    log.info("Advisor {} rejected submission for student {} with reason: {}", 
                            i + 1, firstSubmission.getStudentNumber(), rejectionReason);
                } else {
                    // Third advisor just views (no action)
                    log.info("Advisor {} viewed {} submissions without taking action", 
                            i + 1, pendingSubmissions.size());
                }
            }
        }

        log.info("✅ Multiple advisor workflow integration test completed successfully");
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
} 