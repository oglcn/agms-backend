package com.agms.backend.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.agms.backend.dto.SubmissionResponse;
import com.agms.backend.model.AdvisorList;
import com.agms.backend.model.DepartmentList;
import com.agms.backend.model.FacultyList;
import com.agms.backend.model.Graduation;
import com.agms.backend.model.GraduationList;
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
import com.agms.backend.service.SubmissionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class RegularGraduationMultiAdvisorTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AdvisorRepository advisorRepository;

    @Autowired
    private AdvisorListRepository advisorListRepository;

    @Autowired
    private DepartmentSecretaryRepository departmentSecretaryRepository;

    @Autowired
    private DeanOfficerRepository deanOfficerRepository;

    @Autowired
    private StudentAffairsRepository studentAffairsRepository;

    @Autowired
    private GraduationRepository graduationRepository;

    @Autowired
    private GraduationListRepository graduationListRepository;

    @Autowired
    private FacultyListRepository facultyListRepository;

    @Autowired
    private DepartmentListRepository departmentListRepository;

    // Test entities
    private StudentAffairs studentAffairs;
    private DeanOfficer deanOfficer;
    private DepartmentSecretary departmentSecretary;
    
    // Multiple advisors
    private Advisor advisor1;
    private Advisor advisor2;
    private AdvisorList advisorList1;
    private AdvisorList advisorList2;
    
    // Multiple students
    private Student student1; // Under advisor1, eligible (S001)
    private Student student2; // Under advisor2, eligible (S013)
    private Student student3; // Under advisor1, eligible (S027)
    private Student student4; // Under advisor2, might not be eligible

    @BeforeEach
    @Transactional
    void setUp() {
        log.info("=== Setting up Multi-Advisor Regular Graduation Test ===");

        // Create the complete hierarchy from top to bottom
        
        // 1. Create StudentAffairs
        studentAffairs = StudentAffairs.builder()
                .empId("SA_MULTI_TEST_001")
                .firstName("Test")
                .lastName("StudentAffairs")
                .email("test.sa.multi@edu")
                .password("password123")
                .build();
        studentAffairs = studentAffairsRepository.save(studentAffairs);

        // 2. Create DeanOfficer
        deanOfficer = DeanOfficer.builder()
                .empId("DO_MULTI_TEST_001")
                .firstName("Test")
                .lastName("DeanOfficer")
                .email("test.do.multi@edu")
                .password("password123")
                .studentAffairs(studentAffairs)
                .build();
        deanOfficer = deanOfficerRepository.save(deanOfficer);

        // 3. Create DepartmentSecretary
        departmentSecretary = DepartmentSecretary.builder()
                .empId("DS_MULTI_TEST_001")
                .firstName("Test")
                .lastName("DepartmentSecretary")
                .email("test.ds.multi@edu")
                .password("password123")
                .deanOfficer(deanOfficer)
                .build();
        departmentSecretary = departmentSecretaryRepository.save(departmentSecretary);

        // 4. Create Graduation
        Graduation graduation = Graduation.builder()
                .graduationId("GRAD_MULTI_TEST_001")
                .requestDate(new Timestamp(System.currentTimeMillis()))
                .term("Spring 2025")
                .status("IN_PROGRESS")
                .studentAffairs(studentAffairs)
                .build();
        graduation = graduationRepository.save(graduation);

        // 5. Create GraduationList
        GraduationList graduationList = GraduationList.builder()
                .listId("GL_MULTI_TEST_001")
                .creationDate(new Timestamp(System.currentTimeMillis()))
                .graduation(graduation)
                .build();
        graduationList = graduationListRepository.save(graduationList);

        // 6. Create FacultyList
        FacultyList facultyList = FacultyList.builder()
                .facultyListId("FL_MULTI_TEST_001")
                .creationDate(new Timestamp(System.currentTimeMillis()))
                .faculty("Engineering")
                .deanOfficer(deanOfficer)
                .graduationList(graduationList)
                .build();
        facultyList = facultyListRepository.save(facultyList);

        // 7. Create DepartmentList
        DepartmentList departmentList = DepartmentList.builder()
                .deptListId("DL_MULTI_TEST_001")
                .creationDate(new Timestamp(System.currentTimeMillis()))
                .department("Computer Engineering")
                .secretary(departmentSecretary)
                .facultyList(facultyList)
                .build();
        departmentList = departmentListRepository.save(departmentList);

        // 8. Create Advisor 1
        advisor1 = Advisor.builder()
                .empId("ADV_MULTI_TEST_001")
                .firstName("Test")
                .lastName("Advisor1")
                .email("test.advisor1.multi@edu")
                .password("password123")
                .departmentSecretary(departmentSecretary)
                .build();
        advisor1 = advisorRepository.save(advisor1);

        // 9. Create Advisor 2
        advisor2 = Advisor.builder()
                .empId("ADV_MULTI_TEST_002")
                .firstName("Test")
                .lastName("Advisor2")
                .email("test.advisor2.multi@edu")
                .password("password123")
                .departmentSecretary(departmentSecretary)
                .build();
        advisor2 = advisorRepository.save(advisor2);

        // 10. Create AdvisorList 1
        advisorList1 = AdvisorList.builder()
                .advisorListId("AL_MULTI_TEST_001")
                .creationDate(new Timestamp(System.currentTimeMillis()))
                .advisor(advisor1)
                .departmentList(departmentList)
                .build();
        advisorList1 = advisorListRepository.save(advisorList1);

        // 11. Create AdvisorList 2
        advisorList2 = AdvisorList.builder()
                .advisorListId("AL_MULTI_TEST_002")
                .creationDate(new Timestamp(System.currentTimeMillis()))
                .advisor(advisor2)
                .departmentList(departmentList)
                .build();
        advisorList2 = advisorListRepository.save(advisorList2);

        // Update advisors with advisor lists
        advisor1.setAdvisorList(advisorList1);
        advisor1 = advisorRepository.save(advisor1);

        advisor2.setAdvisorList(advisorList2);
        advisor2 = advisorRepository.save(advisor2);

        // 12. Create test students
        // Student 1 - Under advisor1, eligible (S001 from ubys.json)
        student1 = Student.builder()
                .studentNumber("S001")
                .firstName("Test")
                .lastName("Student1")
                .email("s001@std.iyte.edu.tr")
                .password("password123")
                .department("Computer Engineering")
                .advisor(advisor1)
                .build();
        student1 = studentRepository.save(student1);

        // Student 2 - Under advisor2, eligible (S013 from ubys.json)
        student2 = Student.builder()
                .studentNumber("S013")
                .firstName("Test")
                .lastName("Student2")
                .email("s013@std.iyte.edu.tr")
                .password("password123")
                .department("Computer Engineering")
                .advisor(advisor2)
                .build();
        student2 = studentRepository.save(student2);

        // Student 3 - Under advisor1, eligible (S027 from ubys.json)
        student3 = Student.builder()
                .studentNumber("S027")
                .firstName("Test")
                .lastName("Student3")
                .email("s027@std.iyte.edu.tr")
                .password("password123")
                .department("Computer Engineering")
                .advisor(advisor1)
                .build();
        student3 = studentRepository.save(student3);

        // Student 4 - Under advisor2, potentially not eligible (S002 from ubys.json)
        student4 = Student.builder()
                .studentNumber("S002")
                .firstName("Test")
                .lastName("Student4")
                .email("s002@std.iyte.edu.tr")
                .password("password123")
                .department("Computer Engineering")
                .advisor(advisor2)
                .build();
        student4 = studentRepository.save(student4);

        log.info("Test setup completed successfully");
        log.info("Advisor1 ({}) has students: S001, S027", advisor1.getEmpId());
        log.info("Advisor2 ({}) has students: S013, S002", advisor2.getEmpId());
    }

    @Test
    @Transactional
    void testRegularGraduationWithMultipleAdvisors() {
        log.info("=== Testing Regular Graduation with Multiple Advisors ===");

        // Set up Student Affairs authentication
        setUpAuthenticationForStudentAffairs();

        // Start the regular graduation process
        String term = "Spring 2025";
        List<SubmissionResponse> createdSubmissions = submissionService.startRegularGraduation(term);

        // Verify submissions were created
        assertNotNull(createdSubmissions);
        assertTrue(createdSubmissions.size() > 0, "At least one submission should be created for eligible students");

        log.info("Created {} submissions for regular graduation", createdSubmissions.size());

        // Count submissions per advisor
        long advisor1Submissions = createdSubmissions.stream()
                .filter(s -> advisorList1.getAdvisorListId().equals(s.getAdvisorListId()))
                .count();

        long advisor2Submissions = createdSubmissions.stream()
                .filter(s -> advisorList2.getAdvisorListId().equals(s.getAdvisorListId()))
                .count();

        log.info("Advisor1 has {} submissions, Advisor2 has {} submissions", 
                advisor1Submissions, advisor2Submissions);

        // Test Advisor 1's view
        setUpAuthenticationForAdvisor1();
        List<SubmissionResponse> advisor1View = submissionService.getMyPendingSubmissions();
        
        log.info("Advisor1 sees {} pending submissions", advisor1View.size());
        
        // Verify advisor1 only sees their students' submissions
        for (SubmissionResponse submission : advisor1View) {
            assertEquals(advisorList1.getAdvisorListId(), submission.getAdvisorListId());
            assertTrue(submission.getStudentNumber().equals("S001") || submission.getStudentNumber().equals("S027"),
                    "Advisor1 should only see submissions from students S001 and S027");
            assertEquals(SubmissionStatus.PENDING, submission.getStatus());
            
            log.info("Advisor1 sees submission for student: {} with ID: {}", 
                    submission.getStudentNumber(), submission.getSubmissionId());
        }

        // Test Advisor 2's view
        setUpAuthenticationForAdvisor2();
        List<SubmissionResponse> advisor2View = submissionService.getMyPendingSubmissions();
        
        log.info("Advisor2 sees {} pending submissions", advisor2View.size());
        
        // Verify advisor2 only sees their students' submissions
        for (SubmissionResponse submission : advisor2View) {
            assertEquals(advisorList2.getAdvisorListId(), submission.getAdvisorListId());
            assertTrue(submission.getStudentNumber().equals("S013") || submission.getStudentNumber().equals("S002"),
                    "Advisor2 should only see submissions from students S013 and S002");
            assertEquals(SubmissionStatus.PENDING, submission.getStatus());
            
            log.info("Advisor2 sees submission for student: {} with ID: {}", 
                    submission.getStudentNumber(), submission.getSubmissionId());
        }

        // Verify advisors see different submissions (no overlap)
        List<String> advisor1StudentNumbers = advisor1View.stream()
                .map(SubmissionResponse::getStudentNumber)
                .toList();
        
        List<String> advisor2StudentNumbers = advisor2View.stream()
                .map(SubmissionResponse::getStudentNumber)
                .toList();

        // Ensure no overlap between advisor views
        for (String studentNumber : advisor1StudentNumbers) {
            assertFalse(advisor2StudentNumbers.contains(studentNumber),
                    "Student " + studentNumber + " should not appear in both advisor views");
        }

        log.info("=== Multi-Advisor Regular Graduation Test Completed Successfully ===");
    }

    @Test
    @Transactional
    void testAdvisorWorkflowAfterRegularGraduation() {
        log.info("=== Testing Advisor Workflow After Regular Graduation ===");

        // Set up Student Affairs authentication and start regular graduation
        setUpAuthenticationForStudentAffairs();
        String term = "Spring 2025";
        List<SubmissionResponse> createdSubmissions = submissionService.startRegularGraduation(term);

        assertTrue(createdSubmissions.size() > 0, "Should have created submissions");

        // Test Advisor 1 approving a submission
        setUpAuthenticationForAdvisor1();
        List<SubmissionResponse> advisor1Pending = submissionService.getMyPendingSubmissions();
        
        if (!advisor1Pending.isEmpty()) {
            SubmissionResponse submissionToApprove = advisor1Pending.get(0);
            log.info("Advisor1 approving submission: {} for student: {}", 
                    submissionToApprove.getSubmissionId(), submissionToApprove.getStudentNumber());
            
            // Approve the submission
            SubmissionResponse approvedSubmission = submissionService.approveSubmission(submissionToApprove.getSubmissionId());
            
            assertEquals(SubmissionStatus.APPROVED_BY_ADVISOR, approvedSubmission.getStatus());
            log.info("Submission {} successfully approved by Advisor1", approvedSubmission.getSubmissionId());

            // Verify it's no longer in advisor's pending list
            List<SubmissionResponse> advisor1PendingAfter = submissionService.getMyPendingSubmissions();
            assertFalse(advisor1PendingAfter.stream()
                    .anyMatch(s -> s.getSubmissionId().equals(approvedSubmission.getSubmissionId())),
                    "Approved submission should no longer be in advisor's pending list");
        }

        // Test Advisor 2 rejecting a submission
        setUpAuthenticationForAdvisor2();
        List<SubmissionResponse> advisor2Pending = submissionService.getMyPendingSubmissions();
        
        if (!advisor2Pending.isEmpty()) {
            SubmissionResponse submissionToReject = advisor2Pending.get(0);
            log.info("Advisor2 rejecting submission: {} for student: {}", 
                    submissionToReject.getSubmissionId(), submissionToReject.getStudentNumber());
            
            // Reject the submission
            String rejectionReason = "Missing required documentation";
            SubmissionResponse rejectedSubmission = submissionService.rejectSubmission(
                    submissionToReject.getSubmissionId(), rejectionReason);
            
            assertEquals(SubmissionStatus.REJECTED_BY_ADVISOR, rejectedSubmission.getStatus());
            log.info("Submission {} successfully rejected by Advisor2", rejectedSubmission.getSubmissionId());

            // Verify it's no longer in advisor's pending list
            List<SubmissionResponse> advisor2PendingAfter = submissionService.getMyPendingSubmissions();
            assertFalse(advisor2PendingAfter.stream()
                    .anyMatch(s -> s.getSubmissionId().equals(rejectedSubmission.getSubmissionId())),
                    "Rejected submission should no longer be in advisor's pending list");
        }

        log.info("=== Advisor Workflow Test Completed Successfully ===");
    }

    private void setUpAuthenticationForStudentAffairs() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                studentAffairs.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT_AFFAIRS"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Set authentication for Student Affairs: {}", studentAffairs.getEmail());
    }

    private void setUpAuthenticationForAdvisor1() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                advisor1.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADVISOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Set authentication for Advisor1: {}", advisor1.getEmail());
    }

    private void setUpAuthenticationForAdvisor2() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                advisor2.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADVISOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Set authentication for Advisor2: {}", advisor2.getEmail());
    }
} 