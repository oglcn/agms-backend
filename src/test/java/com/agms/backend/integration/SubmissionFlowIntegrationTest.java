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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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
                .requestDate(LocalDate.now())
                .term("Spring 2025")
                .studentAffairs(studentAffairs)
                .build();
        graduation = graduationRepository.save(graduation);

        // 5. Create GraduationList
        GraduationList graduationList = GraduationList.builder()
                .listId("GL_TEST_001")
                .creationDate(LocalDate.now())
                .graduation(graduation)
                .build();
        graduationList = graduationListRepository.save(graduationList);

        // 6. Create FacultyList
        FacultyList facultyList = FacultyList.builder()
                .facultyListId("FL_TEST_001")
                .creationDate(LocalDate.now())
                .faculty("Engineering")
                .deanOfficer(deanOfficer)
                .graduationList(graduationList)
                .build();
        facultyList = facultyListRepository.save(facultyList);

        // 7. Create DepartmentList
        DepartmentList departmentList = DepartmentList.builder()
                .deptListId("DL_TEST_001")
                .creationDate(LocalDate.now())
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
                .creationDate(LocalDate.now())
                .advisor(testAdvisor)
                .departmentList(departmentList)
                .build();
        testAdvisorList = advisorListRepository.save(testAdvisorList);

        // 10. Update advisor with the saved advisor list to complete the bidirectional relationship
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
        SubmissionResponse approvedSubmission = submissionService.updateSubmissionStatus(submissionId, SubmissionStatus.APPROVED);
        assertEquals(SubmissionStatus.APPROVED, approvedSubmission.getStatus());

        // Step 6: Verify the status was updated
        Optional<SubmissionResponse> updatedSubmission = submissionService.getSubmissionById(submissionId);
        assertTrue(updatedSubmission.isPresent());
        assertEquals(SubmissionStatus.APPROVED, updatedSubmission.get().getStatus());

        // Step 7: Verify student no longer has active pending submission (since it's approved)
        assertFalse(submissionService.hasActivePendingSubmission("S_TEST_001"));

        // Step 8: Verify latest submission is the approved one
        Optional<SubmissionResponse> latestSubmission = submissionService.getLatestSubmissionByStudent("S_TEST_001");
        assertTrue(latestSubmission.isPresent());
        assertEquals(SubmissionStatus.APPROVED, latestSubmission.get().getStatus());
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
        SubmissionResponse rejectedSubmission = submissionService.updateSubmissionStatus(submissionId, SubmissionStatus.REJECTED);
        assertEquals(SubmissionStatus.REJECTED, rejectedSubmission.getStatus());

        // Step 3: Student can now create a new submission (since previous is not pending)
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
} 