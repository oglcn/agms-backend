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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
public class RoleAgnosticSubmissionTest {

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

    private String testSubmissionId;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create the complete hierarchy from top to bottom (copied from SubmissionFlowIntegrationTest)
        
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
        Advisor testAdvisor = Advisor.builder()
                .empId("ADV_TEST_001")
                .firstName("Test")
                .lastName("Advisor")
                .email("test.advisor@edu")
                .password("password123")
                .departmentSecretary(departmentSecretary)
                .build();
        testAdvisor = advisorRepository.save(testAdvisor);

        // 9. Create AdvisorList
        AdvisorList testAdvisorList = AdvisorList.builder()
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
        Student testStudent = Student.builder()
                .studentNumber("S_TEST_001")
                .firstName("Test")
                .lastName("Student")
                .email("test.student@edu")
                .password("password123")
                .advisor(testAdvisor)
                .build();
        testStudent = studentRepository.save(testStudent);

        // 12. Create a test submission to work with
        CreateSubmissionRequest request = CreateSubmissionRequest.builder()
                .studentNumber("S_TEST_001")
                .content("Test submission for role-agnostic testing")
                .build();
        
        SubmissionResponse response = submissionService.createGraduationSubmission(request);
        testSubmissionId = response.getSubmissionId();
    }

    @Test
    @WithMockUser(username = "test.advisor@edu", roles = {"ADVISOR"})
    void testAdvisorApproveSubmission() {
        // Test that advisor can approve submission using role-agnostic method
        SubmissionResponse response = submissionService.approveSubmission(testSubmissionId);
        
        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.APPROVED_BY_ADVISOR);
        assertThat(response.getSubmissionId()).isEqualTo(testSubmissionId);
    }

    @Test
    @WithMockUser(username = "test.advisor@edu", roles = {"ADVISOR"})
    void testAdvisorRejectSubmissionWithReason() {
        String rejectionReason = "Missing required documents";
        
        // Test that advisor can reject submission with reason using role-agnostic method
        SubmissionResponse response = submissionService.rejectSubmission(testSubmissionId, rejectionReason);
        
        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.REJECTED_BY_ADVISOR);
        assertThat(response.getContent()).isEqualTo(rejectionReason);
        assertThat(response.getSubmissionId()).isEqualTo(testSubmissionId);
    }

    @Test
    @WithMockUser(username = "test.ds@edu", roles = {"DEPARTMENT_SECRETARY"})
    void testDepartmentSecretaryApproveSubmission() {
        // First approve by advisor to get to APPROVED_BY_ADVISOR status
        submissionService.updateSubmissionStatusByAdvisor(testSubmissionId, SubmissionStatus.APPROVED_BY_ADVISOR, null);
        
        // Test that department secretary can approve submission using role-agnostic method
        SubmissionResponse response = submissionService.approveSubmission(testSubmissionId);
        
        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.APPROVED_BY_DEPT);
        assertThat(response.getSubmissionId()).isEqualTo(testSubmissionId);
    }

    @Test
    @WithMockUser(username = "test.do@edu", roles = {"DEAN_OFFICER"})
    void testDeanOfficerRejectSubmission() {
        // Progress submission to APPROVED_BY_DEPT status
        submissionService.updateSubmissionStatusByAdvisor(testSubmissionId, SubmissionStatus.APPROVED_BY_ADVISOR, null);
        submissionService.updateSubmissionStatusByDepartmentSecretary(testSubmissionId, SubmissionStatus.APPROVED_BY_DEPT, null);
        
        String rejectionReason = "Does not meet faculty standards";
        
        // Test that dean officer can reject submission using role-agnostic method
        SubmissionResponse response = submissionService.rejectSubmission(testSubmissionId, rejectionReason);
        
        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.REJECTED_BY_DEAN);
        assertThat(response.getContent()).isEqualTo(rejectionReason);
        assertThat(response.getSubmissionId()).isEqualTo(testSubmissionId);
    }

    @Test
    @WithMockUser(username = "test.sa@edu", roles = {"STUDENT_AFFAIRS"})
    void testStudentAffairsFinalApproval() {
        // Progress submission to APPROVED_BY_DEAN status
        submissionService.updateSubmissionStatusByAdvisor(testSubmissionId, SubmissionStatus.APPROVED_BY_ADVISOR, null);
        submissionService.updateSubmissionStatusByDepartmentSecretary(testSubmissionId, SubmissionStatus.APPROVED_BY_DEPT, null);
        submissionService.updateSubmissionStatusByDeanOfficer(testSubmissionId, SubmissionStatus.APPROVED_BY_DEAN, null);
        
        // Test that student affairs can give final approval using role-agnostic method
        SubmissionResponse response = submissionService.approveSubmission(testSubmissionId);
        
        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.FINAL_APPROVED);
        assertThat(response.getSubmissionId()).isEqualTo(testSubmissionId);
    }

    @Test
    @WithMockUser(username = "test.student@edu", roles = {"STUDENT"})
    void testStudentRoleCannotApproveSubmission() {
        // Test that student role cannot approve submissions
        assertThatThrownBy(() -> {
            submissionService.approveSubmission(testSubmissionId);
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unsupported role for empId lookup: STUDENT");
    }

    @Test
    @WithMockUser(username = "test.student@edu", roles = {"STUDENT"})
    void testStudentRoleCannotRejectSubmission() {
        // Test that student role cannot reject submissions
        assertThatThrownBy(() -> {
            submissionService.rejectSubmission(testSubmissionId, "Student trying to reject");
        }).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unsupported role for empId lookup: STUDENT");
    }

    @Test
    void testRoleAgnosticMethodDetectsUserRole() {
        // This test verifies that the role detection logic works correctly
        // by testing different roles in sequence
        
        // Test as advisor
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .setAuthentication(new org.springframework.security.authentication.TestingAuthenticationToken(
                "test.advisor@edu", "password", "ROLE_ADVISOR"));
        
        SubmissionResponse advisorResponse = submissionService.approveSubmission(testSubmissionId);
        assertThat(advisorResponse.getStatus()).isEqualTo(SubmissionStatus.APPROVED_BY_ADVISOR);
        
        // Reset submission for next test
        testSubmissionId = createNewTestSubmission();
        
        // Progress to department secretary level
        submissionService.updateSubmissionStatusByAdvisor(testSubmissionId, SubmissionStatus.APPROVED_BY_ADVISOR, null);
        
        // Test as department secretary
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .setAuthentication(new org.springframework.security.authentication.TestingAuthenticationToken(
                "test.ds@edu", "password", "ROLE_DEPARTMENT_SECRETARY"));
        
        SubmissionResponse deptResponse = submissionService.approveSubmission(testSubmissionId);
        assertThat(deptResponse.getStatus()).isEqualTo(SubmissionStatus.APPROVED_BY_DEPT);
    }

    private String createNewTestSubmission() {
        CreateSubmissionRequest request = CreateSubmissionRequest.builder()
                .studentNumber("S_TEST_001")
                .content("New test submission")
                .build();
        
        SubmissionResponse response = submissionService.createGraduationSubmission(request);
        return response.getSubmissionId();
    }

    @Test
    @WithMockUser(username = "test.advisor@edu", roles = {"ADVISOR"})
    void testMySubmissionsForAdvisor() {
        // Test that advisor can get their submissions using role-agnostic endpoint
        List<SubmissionResponse> submissions = submissionService.getMySubmissions();
        
        assertThat(submissions).isNotEmpty();
        assertThat(submissions).anyMatch(sub -> sub.getSubmissionId().equals(testSubmissionId));
    }

    @Test
    @WithMockUser(username = "test.advisor@edu", roles = {"ADVISOR"})
    void testMyPendingSubmissionsForAdvisor() {
        // Test that advisor can get their pending submissions using role-agnostic endpoint
        List<SubmissionResponse> pendingSubmissions = submissionService.getMyPendingSubmissions();
        
        assertThat(pendingSubmissions).isNotEmpty();
        assertThat(pendingSubmissions).anyMatch(sub -> 
            sub.getSubmissionId().equals(testSubmissionId) && 
            sub.getStatus() == SubmissionStatus.PENDING);
    }

    @Test
    @WithMockUser(username = "test.ds@edu", roles = {"DEPARTMENT_SECRETARY"})
    void testMyPendingSubmissionsForDepartmentSecretary() {
        // First approve by advisor to get submission to department secretary level
        submissionService.updateSubmissionStatusByAdvisor(testSubmissionId, SubmissionStatus.APPROVED_BY_ADVISOR, null);
        
        // Test that department secretary can get their pending submissions
        List<SubmissionResponse> pendingSubmissions = submissionService.getMyPendingSubmissions();
        
        assertThat(pendingSubmissions).isNotEmpty();
        assertThat(pendingSubmissions).anyMatch(sub -> 
            sub.getSubmissionId().equals(testSubmissionId) && 
            sub.getStatus() == SubmissionStatus.APPROVED_BY_ADVISOR);
    }

    @Test
    @WithMockUser(username = "test.student@edu", roles = {"STUDENT"})
    void testMySubmissionsForStudent() {
        // Test that student can get their own submissions using role-agnostic endpoint
        List<SubmissionResponse> submissions = submissionService.getMySubmissions();
        
        assertThat(submissions).isNotEmpty();
        assertThat(submissions).anyMatch(sub -> sub.getSubmissionId().equals(testSubmissionId));
        // Student should only see their own submissions
        assertThat(submissions).allMatch(sub -> "S_TEST_001".equals(sub.getStudentNumber()));
    }
} 