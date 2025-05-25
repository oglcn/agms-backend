package com.agms.backend.integration;

import com.agms.backend.dto.CreateSubmissionRequest;
import com.agms.backend.dto.SubmissionResponse;
import com.agms.backend.model.*;
import com.agms.backend.model.SubmissionStatus;
import com.agms.backend.model.users.*;
import com.agms.backend.repository.*;
import com.agms.backend.service.SubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class SubmissionFlowIntegrationTest {

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
        private DepartmentListRepository departmentListRepository;

        @Autowired
        private FacultyListRepository facultyListRepository;

        @Autowired
        private GraduationListRepository graduationListRepository;

        @Autowired
        private GraduationRepository graduationRepository;

        @Autowired
        private DeanOfficerRepository deanOfficerRepository;

        @Autowired
        private StudentAffairsRepository studentAffairsRepository;

        private Student testStudent;
        private Advisor testAdvisor;
        private AdvisorList testAdvisorList;

        @BeforeEach
        @Transactional
        void setUp() {
                // Create the complete hierarchy from top to bottom

                // 1. Create StudentAffairs
                StudentAffairs studentAffairs = StudentAffairs.builder()
                                .empId("SA_TEST_001")
                                .firstName("Test")
                                .lastName("StudentAffairs")
                                .email("test.sa@edu")
                                .password("password123")
                                .build();
                studentAffairs = studentAffairsRepository.save(studentAffairs);

                // 2. Create DeanOfficer
                DeanOfficer deanOfficer = DeanOfficer.builder()
                                .empId("DO_TEST_001")
                                .firstName("Test")
                                .lastName("DeanOfficer")
                                .email("test.do@edu")
                                .password("password123")
                                .studentAffairs(studentAffairs)
                                .build();
                deanOfficer = deanOfficerRepository.save(deanOfficer);

                // 3. Create DepartmentSecretary
                DepartmentSecretary departmentSecretary = DepartmentSecretary.builder()
                                .empId("DS_TEST_001")
                                .firstName("Test")
                                .lastName("DepartmentSecretary")
                                .email("test.ds@edu")
                                .password("password123")
                                .deanOfficer(deanOfficer)
                                .build();
                departmentSecretary = departmentSecretaryRepository.save(departmentSecretary);

                // 4. Create Graduation
                Graduation graduation = Graduation.builder()
                                .graduationId("GRAD_TEST_001")
                                .requestDate(new Timestamp(System.currentTimeMillis()))
                                .term("Spring 2025")
                                .studentAffairs(studentAffairs)
                                .build();
                graduation = graduationRepository.save(graduation);

                // 5. Create GraduationList
                GraduationList graduationList = GraduationList.builder()
                                .listId("GL_TEST_001")
                                .creationDate(new Timestamp(System.currentTimeMillis()))
                                .graduation(graduation)
                                .build();
                graduationList = graduationListRepository.save(graduationList);

                // 6. Create FacultyList
                FacultyList facultyList = FacultyList.builder()
                                .facultyListId("FL_TEST_001")
                                .creationDate(new Timestamp(System.currentTimeMillis()))
                                .faculty("Engineering")
                                .deanOfficer(deanOfficer)
                                .graduationList(graduationList)
                                .build();
                facultyList = facultyListRepository.save(facultyList);

                // 7. Create DepartmentList
                DepartmentList departmentList = DepartmentList.builder()
                                .deptListId("DL_TEST_001")
                                .creationDate(new Timestamp(System.currentTimeMillis()))
                                .department("Computer Engineering")
                                .secretary(departmentSecretary)
                                .facultyList(facultyList)
                                .build();
                departmentList = departmentListRepository.save(departmentList);

                // 8. Create Advisor
                testAdvisor = Advisor.builder()
                                .empId("ADV_TEST_001")
                                .firstName("Test")
                                .lastName("Advisor")
                                .email("test.advisor@edu")
                                .password("password123")
                                .departmentSecretary(departmentSecretary)
                                .build();
                testAdvisor = advisorRepository.save(testAdvisor);

                // 9. Create AdvisorList
                testAdvisorList = AdvisorList.builder()
                                .advisorListId("AL_TEST_001")
                                .creationDate(new Timestamp(System.currentTimeMillis()))
                                .advisor(testAdvisor)
                                .departmentList(departmentList)
                                .build();
                testAdvisorList = advisorListRepository.save(testAdvisorList);

                // 10. Update advisor with the saved advisor list to complete the bidirectional
                // relationship
                testAdvisor.setAdvisorList(testAdvisorList);
                testAdvisor = advisorRepository.save(testAdvisor);

                // 11. Create Student with the advisor
                testStudent = Student.builder()
                                .studentNumber("S_TEST_001")
                                .firstName("Test")
                                .lastName("Student")
                                .email("test.student@edu")
                                .password("password123")
                                .advisor(testAdvisor)
                                .build();
                testStudent = studentRepository.save(testStudent);
        }

        @Test
        @Transactional
        void completeGraduationSubmissionFlow() {
                // Step 1: Student creates graduation submission
                CreateSubmissionRequest request = CreateSubmissionRequest.builder()
                                .studentNumber("S_TEST_001")
                                .content("I would like to request graduation for Spring 2025")
                                .build();

                SubmissionResponse submissionResponse = submissionService.createGraduationSubmission(request);

                // Verify submission was created successfully
                assertNotNull(submissionResponse);
                assertNotNull(submissionResponse.getSubmissionId());
                assertEquals("S_TEST_001", submissionResponse.getStudentNumber());
                assertEquals("Test Student", submissionResponse.getStudentName());
                assertEquals(SubmissionStatus.PENDING, submissionResponse.getStatus());
                assertEquals("AL_TEST_001", submissionResponse.getAdvisorListId());
                assertEquals("I would like to request graduation for Spring 2025", submissionResponse.getContent());

                String submissionId = submissionResponse.getSubmissionId();

                // Step 2: Verify submission appears in student's submissions
                List<SubmissionResponse> studentSubmissions = submissionService.getSubmissionsByStudent("S_TEST_001");
                assertEquals(1, studentSubmissions.size());
                assertEquals(submissionId, studentSubmissions.get(0).getSubmissionId());

                // Step 3: Verify submission appears in advisor's submissions
                List<SubmissionResponse> advisorSubmissions = submissionService.getSubmissionsByAdvisor("ADV_TEST_001");
                assertEquals(1, advisorSubmissions.size());
                assertEquals(submissionId, advisorSubmissions.get(0).getSubmissionId());

                // Step 4: Verify student has active pending submission
                assertTrue(submissionService.hasActivePendingSubmission("S_TEST_001"));

                // Step 5: Advisor approves the submission
                SubmissionResponse approvedSubmission = submissionService.updateSubmissionStatusByAdvisor(submissionId,
                                SubmissionStatus.APPROVED_BY_ADVISOR, null);
                assertEquals(SubmissionStatus.APPROVED_BY_ADVISOR, approvedSubmission.getStatus());

                // Step 6: Verify the status was updated
                Optional<SubmissionResponse> updatedSubmission = submissionService.getSubmissionById(submissionId);
                assertTrue(updatedSubmission.isPresent());
                assertEquals(SubmissionStatus.APPROVED_BY_ADVISOR, updatedSubmission.get().getStatus());

                // Step 7: Verify student no longer has active pending submission (since it's
                // approved)
                assertFalse(submissionService.hasActivePendingSubmission("S_TEST_001"));

                // Step 8: Verify latest submission is the approved one
                Optional<SubmissionResponse> latestSubmission = submissionService
                                .getLatestSubmissionByStudent("S_TEST_001");
                assertTrue(latestSubmission.isPresent());
                assertEquals(SubmissionStatus.APPROVED_BY_ADVISOR, latestSubmission.get().getStatus());
        }

        @Test
        @Transactional
        void studentCannotCreateMultiplePendingSubmissions() {
                // Step 1: Create first submission
                CreateSubmissionRequest request1 = CreateSubmissionRequest.builder()
                                .studentNumber("S_TEST_001")
                                .content("First graduation request")
                                .build();

                SubmissionResponse firstSubmission = submissionService.createGraduationSubmission(request1);
                assertNotNull(firstSubmission);
                assertEquals(SubmissionStatus.PENDING, firstSubmission.getStatus());

                // Step 2: Try to create second submission (should fail)
                CreateSubmissionRequest request2 = CreateSubmissionRequest.builder()
                                .studentNumber("S_TEST_001")
                                .content("Second graduation request")
                                .build();

                assertThrows(IllegalStateException.class, () -> {
                        submissionService.createGraduationSubmission(request2);
                });

                // Verify only one submission exists
                List<SubmissionResponse> submissions = submissionService.getSubmissionsByStudent("S_TEST_001");
                assertEquals(1, submissions.size());
        }

        @Test
        @Transactional
        void submissionCanBeRejectedAndNewOneCreated() {
                // Step 1: Create initial submission
                CreateSubmissionRequest request1 = CreateSubmissionRequest.builder()
                                .studentNumber("S_TEST_001")
                                .content("Initial graduation request")
                                .build();

                SubmissionResponse initialSubmission = submissionService.createGraduationSubmission(request1);
                String submissionId = initialSubmission.getSubmissionId();

                // Step 2: Advisor rejects the submission
                SubmissionResponse rejectedSubmission = submissionService.updateSubmissionStatusByAdvisor(submissionId,
                                SubmissionStatus.REJECTED_BY_ADVISOR, null);
                assertEquals(SubmissionStatus.REJECTED_BY_ADVISOR, rejectedSubmission.getStatus());

                // Step 3: Student can now create a new submission (since previous is not
                // pending)
                CreateSubmissionRequest request2 = CreateSubmissionRequest.builder()
                                .studentNumber("S_TEST_001")
                                .content("Revised graduation request")
                                .build();

                SubmissionResponse newSubmission = submissionService.createGraduationSubmission(request2);
                assertNotNull(newSubmission);
                assertEquals(SubmissionStatus.PENDING, newSubmission.getStatus());
                assertNotEquals(submissionId, newSubmission.getSubmissionId());

                // Verify student now has 2 submissions total
                List<SubmissionResponse> allSubmissions = submissionService.getSubmissionsByStudent("S_TEST_001");
                assertEquals(2, allSubmissions.size());

                // Verify student has active pending submission again
                assertTrue(submissionService.hasActivePendingSubmission("S_TEST_001"));
        }

        @Test
        @Transactional
        void completeWorkflowFromStudentToAdvisorToDepartmentSecretary() {
                log.info("=== Starting Complete Workflow Test ===");

                // Create additional test students and advisors for more comprehensive testing
                createAdditionalTestData();

                // === STEP 1: Multiple students submit graduation requests ===
                log.info("Step 1: Students submitting graduation requests");

                List<String> submissionIds = new ArrayList<>();

                // Student 1 submits
                CreateSubmissionRequest request1 = CreateSubmissionRequest.builder()
                                .studentNumber("S_TEST_001")
                                .content("Student 1 graduation request - Computer Engineering")
                                .build();
                SubmissionResponse submission1 = submissionService.createGraduationSubmission(request1);
                submissionIds.add(submission1.getSubmissionId());
                log.info("Created submission 1: {} for advisor: {}", submission1.getSubmissionId(),
                                submission1.getAdvisorListId());

                // Student 2 submits
                CreateSubmissionRequest request2 = CreateSubmissionRequest.builder()
                                .studentNumber("S_TEST_002")
                                .content("Student 2 graduation request - Computer Engineering")
                                .build();
                SubmissionResponse submission2 = submissionService.createGraduationSubmission(request2);
                submissionIds.add(submission2.getSubmissionId());
                log.info("Created submission 2: {} for advisor: {}", submission2.getSubmissionId(),
                                submission2.getAdvisorListId());

                // Student 3 submits (under different advisor)
                CreateSubmissionRequest request3 = CreateSubmissionRequest.builder()
                                .studentNumber("S_TEST_003")
                                .content("Student 3 graduation request - Computer Engineering")
                                .build();
                SubmissionResponse submission3 = submissionService.createGraduationSubmission(request3);
                submissionIds.add(submission3.getSubmissionId());
                log.info("Created submission 3: {} for advisor: {}", submission3.getSubmissionId(),
                                submission3.getAdvisorListId());

                // === STEP 2: Verify submissions appear in advisor's pending list ===
                log.info("Step 2: Verifying submissions appear in advisor's pending lists");

                // Check Advisor 1's pending submissions (should have 2)
                List<SubmissionResponse> advisor1Pending = submissionService
                                .getSubmissionsPendingForRole("ADV_TEST_001", "ADVISOR");
                assertEquals(2, advisor1Pending.size(), "Advisor 1 should have 2 pending submissions");
                log.info("Advisor 1 has {} pending submissions", advisor1Pending.size());

                // Check Advisor 2's pending submissions (should have 1)
                List<SubmissionResponse> advisor2Pending = submissionService
                                .getSubmissionsPendingForRole("ADV_TEST_002", "ADVISOR");
                assertEquals(1, advisor2Pending.size(), "Advisor 2 should have 1 pending submission");
                log.info("Advisor 2 has {} pending submissions", advisor2Pending.size());

                // === STEP 3: Advisor 1 reviews submissions - approves some, rejects some ===
                log.info("Step 3: Advisor 1 reviewing submissions");

                // Advisor 1 approves first submission
                SubmissionResponse approved1 = submissionService.updateSubmissionStatusByAdvisor(
                                submission1.getSubmissionId(), SubmissionStatus.APPROVED_BY_ADVISOR, null);
                assertEquals(SubmissionStatus.APPROVED_BY_ADVISOR, approved1.getStatus());
                log.info("Advisor 1 approved submission: {}", submission1.getSubmissionId());

                // Advisor 1 rejects second submission
                SubmissionResponse rejected2 = submissionService.updateSubmissionStatusByAdvisor(
                                submission2.getSubmissionId(), SubmissionStatus.REJECTED_BY_ADVISOR, null);
                assertEquals(SubmissionStatus.REJECTED_BY_ADVISOR, rejected2.getStatus());
                log.info("Advisor 1 rejected submission: {}", submission2.getSubmissionId());

                // === STEP 4: Advisor 2 approves their submission ===
                log.info("Step 4: Advisor 2 reviewing submission");

                SubmissionResponse approved3 = submissionService.updateSubmissionStatusByAdvisor(
                                submission3.getSubmissionId(), SubmissionStatus.APPROVED_BY_ADVISOR, null);
                assertEquals(SubmissionStatus.APPROVED_BY_ADVISOR, approved3.getStatus());
                log.info("Advisor 2 approved submission: {}", submission3.getSubmissionId());

                // === STEP 5: Verify no more pending submissions for advisors ===
                log.info("Step 5: Verifying no pending submissions remain for advisors");

                List<SubmissionResponse> advisor1PendingAfter = submissionService
                                .getSubmissionsPendingForRole("ADV_TEST_001", "ADVISOR");
                assertEquals(0, advisor1PendingAfter.size(),
                                "Advisor 1 should have no pending submissions after review");

                List<SubmissionResponse> advisor2PendingAfter = submissionService
                                .getSubmissionsPendingForRole("ADV_TEST_002", "ADVISOR");
                assertEquals(0, advisor2PendingAfter.size(),
                                "Advisor 2 should have no pending submissions after review");

                // === STEP 6: Verify Department Secretary receives approved submissions ===
                log.info("Step 6: Verifying Department Secretary receives approved submissions");

                // Department Secretary should receive approved submissions from both advisors
                // under them
                List<SubmissionResponse> deptSecretaryPending = submissionService
                                .getSubmissionsPendingForRole("DS_TEST_001", "DEPARTMENT_SECRETARY");
                assertEquals(2, deptSecretaryPending.size(), "Department Secretary should have 2 approved submissions");
                log.info("Department Secretary has {} approved submissions to review", deptSecretaryPending.size());

                // Verify the specific submissions are the approved ones
                List<String> approvedSubmissionIds = deptSecretaryPending.stream()
                                .map(SubmissionResponse::getSubmissionId)
                                .collect(Collectors.toList());
                assertTrue(approvedSubmissionIds.contains(submission1.getSubmissionId()),
                                "Should contain approved submission 1");
                assertTrue(approvedSubmissionIds.contains(submission3.getSubmissionId()),
                                "Should contain approved submission 3");
                assertFalse(approvedSubmissionIds.contains(submission2.getSubmissionId()),
                                "Should NOT contain rejected submission 2");

                // Verify all have correct status
                for (SubmissionResponse submission : deptSecretaryPending) {
                        assertEquals(SubmissionStatus.APPROVED_BY_ADVISOR, submission.getStatus(),
                                        "All submissions for department secretary should have APPROVED_BY_ADVISOR status");
                }

                // === STEP 7: Verify rejected submission workflow ===
                log.info("Step 7: Verifying rejected submission handling");

                // Verify rejected submission is not in department secretary's list
                List<SubmissionResponse> allDeptSecretarySubmissions = submissionService
                                .getSubmissionsByDepartmentSecretary("DS_TEST_001");
                List<String> allDeptSubmissionIds = allDeptSecretarySubmissions.stream()
                                .map(SubmissionResponse::getSubmissionId)
                                .collect(Collectors.toList());
                assertFalse(allDeptSubmissionIds.contains(submission2.getSubmissionId()),
                                "Rejected submission should not appear in department secretary's list");

                // === STEP 8: Verify student can create new submission after rejection ===
                log.info("Step 8: Verifying rejected student can resubmit");

                // Student 2 whose submission was rejected should be able to submit again
                assertFalse(submissionService.hasActivePendingSubmission("S_TEST_002"),
                                "Student 2 should not have active pending submission after rejection");

                CreateSubmissionRequest newRequest = CreateSubmissionRequest.builder()
                                .studentNumber("S_TEST_002")
                                .content("Student 2 revised graduation request")
                                .build();

                SubmissionResponse newSubmission = submissionService.createGraduationSubmission(newRequest);
                assertEquals(SubmissionStatus.PENDING, newSubmission.getStatus());
                log.info("Student 2 successfully created new submission: {}", newSubmission.getSubmissionId());

                log.info("=== Complete Workflow Test Completed Successfully ===");
        }

        @Test
        @Transactional
        void basicWorkflowFromStudentToAdvisor() {
                log.info("=== Starting Basic Workflow Test ===");

                // === STEP 1: Student submits graduation request ===
                log.info("Step 1: Student submitting graduation request");

                CreateSubmissionRequest request = CreateSubmissionRequest.builder()
                                .studentNumber("S_TEST_001")
                                .content("Student graduation request - Computer Engineering")
                                .build();
                SubmissionResponse submission = submissionService.createGraduationSubmission(request);
                String submissionId = submission.getSubmissionId();
                log.info("Created submission: {}", submissionId);

                // Verify initial state
                assertEquals(SubmissionStatus.PENDING, submission.getStatus());
                assertEquals("S_TEST_001", submission.getStudentNumber());
                assertNotNull(submission.getAdvisorListId());

                // === STEP 2: Verify submission appears in advisor's pending list ===
                log.info("Step 2: Verifying submission appears in advisor's pending list");

                List<SubmissionResponse> advisorPending = submissionService.getSubmissionsPendingForRole("ADV_TEST_001",
                                "ADVISOR");
                assertEquals(1, advisorPending.size(), "Advisor should have 1 pending submission");
                assertEquals(submissionId, advisorPending.get(0).getSubmissionId());
                log.info("Advisor has {} pending submissions", advisorPending.size());

                // === STEP 3: Advisor approves the submission ===
                log.info("Step 3: Advisor approving submission");

                SubmissionResponse approved = submissionService.updateSubmissionStatusByAdvisor(
                                submissionId, SubmissionStatus.APPROVED_BY_ADVISOR, null);
                assertEquals(SubmissionStatus.APPROVED_BY_ADVISOR, approved.getStatus());
                log.info("Advisor approved submission: {}", submissionId);

                // === STEP 4: Verify no more pending submissions for advisor ===
                log.info("Step 4: Verifying no pending submissions remain for advisor");

                List<SubmissionResponse> advisorPendingAfter = submissionService
                                .getSubmissionsPendingForRole("ADV_TEST_001", "ADVISOR");
                assertEquals(0, advisorPendingAfter.size(), "Advisor should have no pending submissions after review");

                // === STEP 5: Verify submission can be retrieved by various methods ===
                log.info("Step 5: Verifying submission retrieval methods");

                // Get by student
                List<SubmissionResponse> studentSubmissions = submissionService.getSubmissionsByStudent("S_TEST_001");
                assertEquals(1, studentSubmissions.size());
                assertEquals(SubmissionStatus.APPROVED_BY_ADVISOR, studentSubmissions.get(0).getStatus());

                // Get by ID
                Optional<SubmissionResponse> byId = submissionService.getSubmissionById(submissionId);
                assertTrue(byId.isPresent());
                assertEquals(SubmissionStatus.APPROVED_BY_ADVISOR, byId.get().getStatus());

                // Get by status
                List<SubmissionResponse> byStatus = submissionService
                                .getSubmissionsByStatus(SubmissionStatus.APPROVED_BY_ADVISOR);
                assertTrue(byStatus.size() >= 1);
                assertTrue(byStatus.stream().anyMatch(s -> s.getSubmissionId().equals(submissionId)));

                log.info("=== Basic Workflow Test Completed Successfully ===");
        }

        @Test
        @Transactional
        void testRejectionWithReason() {
                log.info("=== Testing Rejection with Reason ===");

                // === STEP 1: Student submits graduation request ===
                CreateSubmissionRequest request = CreateSubmissionRequest.builder()
                                .studentNumber("S_TEST_001")
                                .content("Student graduation request - needs review")
                                .build();
                SubmissionResponse submission = submissionService.createGraduationSubmission(request);
                String submissionId = submission.getSubmissionId();

                // Verify initial content
                assertEquals("Student graduation request - needs review", submission.getContent());
                log.info("Initial submission content: {}", submission.getContent());

                // === STEP 2: Advisor rejects with reason ===
                String rejectionReason = "Your thesis proposal is incomplete. Please include: 1) Research methodology, 2) Literature review, 3) Timeline. Please resubmit after addressing these issues.";

                SubmissionResponse rejectedSubmission = submissionService.updateSubmissionStatusByAdvisor(
                                submissionId,
                                SubmissionStatus.REJECTED_BY_ADVISOR,
                                rejectionReason);

                // Verify the content field now contains the rejection reason
                assertEquals(SubmissionStatus.REJECTED_BY_ADVISOR, rejectedSubmission.getStatus());
                assertEquals(rejectionReason, rejectedSubmission.getContent());
                log.info("Rejection reason stored in content: {}", rejectedSubmission.getContent());

                // === STEP 3: Student can see the rejection reason ===
                Optional<SubmissionResponse> retrievedSubmission = submissionService.getSubmissionById(submissionId);
                assertTrue(retrievedSubmission.isPresent());
                assertEquals(rejectionReason, retrievedSubmission.get().getContent());
                log.info("Student can view rejection reason: {}", retrievedSubmission.get().getContent());

                log.info("=== Rejection with Reason Test Completed Successfully ===");
        }

        private void createAdditionalTestData() {
                // Create second advisor under same department secretary
                Advisor testAdvisor2 = Advisor.builder()
                                .empId("ADV_TEST_002")
                                .firstName("Test2")
                                .lastName("Advisor2")
                                .email("test.advisor2@edu")
                                .password("password123")
                                .departmentSecretary(departmentSecretaryRepository.findByEmpId("DS_TEST_001").get())
                                .build();
                testAdvisor2 = advisorRepository.save(testAdvisor2);

                // Get the existing DepartmentList for the second advisor (they should be under
                // the same department)
                DepartmentList existingDepartmentList = departmentListRepository.findAll().get(0);

                // Create AdvisorList for second advisor under the same DepartmentList
                AdvisorList testAdvisorList2 = AdvisorList.builder()
                                .advisorListId("AL_TEST_002")
                                .creationDate(new Timestamp(System.currentTimeMillis()))
                                .advisor(testAdvisor2)
                                .departmentList(existingDepartmentList) // Link to the same department list
                                .build();
                testAdvisorList2 = advisorListRepository.save(testAdvisorList2);

                // Update advisor with the saved advisor list
                testAdvisor2.setAdvisorList(testAdvisorList2);
                advisorRepository.save(testAdvisor2);

                // Create additional students
                Student testStudent2 = Student.builder()
                                .studentNumber("S_TEST_002")
                                .firstName("Test2")
                                .lastName("Student2")
                                .email("test.student2@edu")
                                .password("password123")
                                .advisor(testAdvisor) // Under first advisor
                                .build();
                studentRepository.save(testStudent2);

                Student testStudent3 = Student.builder()
                                .studentNumber("S_TEST_003")
                                .firstName("Test3")
                                .lastName("Student3")
                                .email("test.student3@edu")
                                .password("password123")
                                .advisor(testAdvisor2) // Under second advisor
                                .build();
                studentRepository.save(testStudent3);
        }
}