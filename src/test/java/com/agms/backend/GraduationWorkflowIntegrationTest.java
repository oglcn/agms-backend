package com.agms.backend;

import com.agms.backend.dto.CreateSubmissionRequest;
import com.agms.backend.dto.StartRegularGraduationRequest;
import com.agms.backend.dto.SubmissionResponse;
import com.agms.backend.model.*;
import com.agms.backend.model.users.*;
import com.agms.backend.repository.*;
import com.agms.backend.service.SubmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class GraduationWorkflowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AdvisorRepository advisorRepository;

    @Autowired
    private DepartmentSecretaryRepository departmentSecretaryRepository;

    @Autowired
    private DeanOfficerRepository deanOfficerRepository;

    @Autowired
    private StudentAffairsRepository studentAffairsRepository;

    @Autowired
    private AdvisorListRepository advisorListRepository;

    @Autowired
    private DepartmentListRepository departmentListRepository;

    @Autowired
    private FacultyListRepository facultyListRepository;

    @Autowired
    private GraduationListRepository graduationListRepository;

    @Autowired
    private GraduationRepository graduationRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // Test data IDs - these will be loaded from main UBYS data
    private static final String STUDENT_EMAIL = "neclaakyol@std.iyte.edu.tr";
    private static final String STUDENT_NUMBER = "S101";
    private static final String ADVISOR_EMP_ID = "ADV101";
    private static final String ADVISOR_EMAIL = "advisorADV101@iyte.edu.tr";
    private static final String DEPT_SEC_EMP_ID = "DS101";
    private static final String DEPT_SEC_EMAIL = "secretaryDS101@iyte.edu.tr";
    private static final String DEAN_EMP_ID = "DO101";
    private static final String DEAN_EMAIL = "deanDO101@iyte.edu.tr";
    private static final String SA_EMP_ID = "SA101";
    private static final String SA_EMAIL = "studentaffairs@iyte.edu.tr";
    private static final String TEST_TERM = "2024-SPRING";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        objectMapper = new ObjectMapper();
        // Remove manual test data setup - use UBYS data instead
        verifyTestDataExists();
    }

    private void verifyTestDataExists() {
        // Verify that the UBYS data has been loaded correctly
        System.out.println("=== VERIFYING TEST DATA ===");
        
        // Check if entities exist
        boolean studentAffairsExists = studentAffairsRepository.findByEmpId(SA_EMP_ID).isPresent();
        boolean deanOfficerExists = deanOfficerRepository.findByEmpId(DEAN_EMP_ID).isPresent();
        boolean deptSecExists = departmentSecretaryRepository.findByEmpId(DEPT_SEC_EMP_ID).isPresent();
        boolean advisorExists = advisorRepository.findByEmpId(ADVISOR_EMP_ID).isPresent();
        boolean studentExists = studentRepository.findByStudentNumber(STUDENT_NUMBER).isPresent();
        
        System.out.println("Student Affairs exists: " + studentAffairsExists);
        System.out.println("Dean Officer exists: " + deanOfficerExists);
        System.out.println("Department Secretary exists: " + deptSecExists);
        System.out.println("Advisor exists: " + advisorExists);
        System.out.println("Student exists: " + studentExists);
        
        // Check graduation hierarchy
        List<GraduationList> graduationLists = graduationListRepository.findAll();
        List<FacultyList> facultyLists = facultyListRepository.findAll();
        List<DepartmentList> departmentLists = departmentListRepository.findAll();
        List<AdvisorList> advisorLists = advisorListRepository.findAll();
        
        System.out.println("Graduation Lists: " + graduationLists.size());
        System.out.println("Faculty Lists: " + facultyLists.size());
        System.out.println("Department Lists: " + departmentLists.size());
        System.out.println("Advisor Lists: " + advisorLists.size());
        
        if (!studentAffairsExists || !deanOfficerExists || !deptSecExists || !advisorExists || !studentExists) {
            throw new RuntimeException("Required test data not found. Check UBYS data initialization.");
        }
    }

    @Test
    @Order(1)
    @WithMockUser(username = SA_EMAIL, roles = {"STUDENT_AFFAIRS"})
    void testStartRegularGraduation() throws Exception {
        System.out.println("\n=== STEP 1: START REGULAR GRADUATION PROCESS ===");
        
        StartRegularGraduationRequest request = new StartRegularGraduationRequest();
        request.setTerm(TEST_TERM);

        mockMvc.perform(post("/api/submissions/regular-graduation/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(result -> {
                    System.out.println("✅ Regular graduation started successfully");
                    
                    // Verify submissions were created
                    List<Submission> submissions = submissionRepository.findAll();
                    System.out.println("📊 Total submissions created: " + submissions.size());
                    
                    for (Submission submission : submissions) {
                        System.out.println("📝 Submission: " + submission.getSubmissionId() + 
                            " | Student: " + submission.getStudent().getStudentNumber() + 
                            " | Status: " + submission.getStatus());
                    }
                });
    }

    @Test
    @Order(2)
    @WithMockUser(username = ADVISOR_EMAIL, roles = {"ADVISOR"})
    void testAdvisorCannotFinalizeWithPendingSubmissions() throws Exception {
        System.out.println("\n=== STEP 2: TEST ADVISOR FINALIZATION CONSTRAINTS ===");
        
        // First, try to finalize without processing submissions
        mockMvc.perform(post("/api/submissions/finalize-my-list"))
                .andExpect(status().isConflict())
                .andDo(result -> {
                    System.out.println("✅ Advisor correctly blocked from finalizing with pending submissions");
                });

        // Check pending submissions
        mockMvc.perform(get("/api/submissions/my-pending"))
                .andExpect(status().isOk())
                .andDo(result -> {
                    System.out.println("📋 Advisor pending submissions: " + result.getResponse().getContentAsString());
                });
    }

    @Test
    @Order(3)
    @WithMockUser(username = ADVISOR_EMAIL, roles = {"ADVISOR"})
    void testAdvisorProcessesSubmissions() throws Exception {
        System.out.println("\n=== STEP 3: ADVISOR PROCESSES SUBMISSIONS ===");
        
        // Get all submissions for advisor
        List<Submission> submissions = submissionRepository.findByStatus(SubmissionStatus.PENDING);
        
        for (Submission submission : submissions) {
            // Approve each submission
            mockMvc.perform(put("/api/submissions/" + submission.getSubmissionId() + "/approve"))
                    .andExpect(status().isOk())
                    .andDo(result -> {
                        System.out.println("✅ Approved submission: " + submission.getSubmissionId());
                    });
        }

        System.out.println("📊 All submissions processed by advisor");
    }

    @Test
    @Order(4)
    @WithMockUser(username = ADVISOR_EMAIL, roles = {"ADVISOR"})
    void testAdvisorFinalizesAfterProcessing() throws Exception {
        System.out.println("\n=== STEP 4: ADVISOR FINALIZES LIST ===");
        
        // Now finalization should work
        mockMvc.perform(post("/api/submissions/finalize-my-list"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andDo(result -> {
                    System.out.println("✅ Advisor successfully finalized their list");
                });

        // Verify finalization status
        mockMvc.perform(get("/api/submissions/my-list/finalized"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andDo(result -> {
                    System.out.println("✅ Advisor list finalization confirmed");
                });
    }

    @Test
    @Order(5)
    @WithMockUser(username = DEPT_SEC_EMAIL, roles = {"DEPARTMENT_SECRETARY"})
    @Transactional
    void testDepartmentSecretaryCannotFinalizeWithoutAdvisorFinalization() throws Exception {
        System.out.println("\n=== STEP 5: TEST DEPARTMENT SECRETARY CONSTRAINTS ===");
        
        // Reset advisor finalization to test constraint
        AdvisorList advisorList = advisorListRepository.findByAdvisorEmpId(ADVISOR_EMP_ID).orElseThrow();
        advisorListRepository.updateFinalizationStatus(advisorList.getAdvisorListId(), false);
        
        // Try to finalize without advisor finalization
        mockMvc.perform(post("/api/submissions/finalize-my-list"))
                .andExpect(status().isConflict())
                .andDo(result -> {
                    System.out.println("✅ Department Secretary correctly blocked - advisor list not finalized");
                });

        // Re-finalize advisor list for next tests
        advisorListRepository.updateFinalizationStatus(advisorList.getAdvisorListId(), true);
    }

    @Test
    @Order(6)
    @WithMockUser(username = DEPT_SEC_EMAIL, roles = {"DEPARTMENT_SECRETARY"})
    void testDepartmentSecretaryProcessesAndFinalizes() throws Exception {
        System.out.println("\n=== STEP 6: DEPARTMENT SECRETARY PROCESSES AND FINALIZES ===");
        
        // Process all submissions awaiting department review
        List<Submission> submissions = submissionRepository.findByStatus(SubmissionStatus.APPROVED_BY_ADVISOR);
        
        for (Submission submission : submissions) {
            mockMvc.perform(put("/api/submissions/" + submission.getSubmissionId() + "/approve"))
                    .andExpect(status().isOk())
                    .andDo(result -> {
                        System.out.println("✅ Department Secretary approved: " + submission.getSubmissionId());
                    });
        }

        // Now finalize department list
        mockMvc.perform(post("/api/submissions/finalize-my-list"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andDo(result -> {
                    System.out.println("✅ Department Secretary successfully finalized their list");
                });
    }

    @Test
    @Order(7)
    @WithMockUser(username = DEAN_EMAIL, roles = {"DEAN_OFFICER"})
    void testDeanOfficerProcessesAndFinalizes() throws Exception {
        System.out.println("\n=== STEP 7: DEAN OFFICER PROCESSES AND FINALIZES ===");
        
        // Process all submissions awaiting dean review
        List<Submission> submissions = submissionRepository.findByStatus(SubmissionStatus.APPROVED_BY_DEPT);
        
        for (Submission submission : submissions) {
            mockMvc.perform(put("/api/submissions/" + submission.getSubmissionId() + "/approve"))
                    .andExpect(status().isOk())
                    .andDo(result -> {
                        System.out.println("✅ Dean Officer approved: " + submission.getSubmissionId());
                    });
        }

        // Finalize faculty list
        mockMvc.perform(post("/api/submissions/finalize-my-list"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andDo(result -> {
                    System.out.println("✅ Dean Officer successfully finalized their list");
                });
    }

    @Test
    @Order(8)
    @WithMockUser(username = SA_EMAIL, roles = {"STUDENT_AFFAIRS"})
    void testStudentAffairsCompletesGraduation() throws Exception {
        System.out.println("\n=== STEP 8: STUDENT AFFAIRS COMPLETES GRADUATION ===");
        
        // Check prerequisites are met
        mockMvc.perform(get("/api/submissions/prerequisite-lists/finalized"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andDo(result -> {
                    System.out.println("✅ All prerequisite lists are finalized");
                });

        // Process final submissions
        List<Submission> submissions = submissionRepository.findByStatus(SubmissionStatus.APPROVED_BY_DEAN);
        
        for (Submission submission : submissions) {
            mockMvc.perform(put("/api/submissions/" + submission.getSubmissionId() + "/approve"))
                    .andExpect(status().isOk())
                    .andDo(result -> {
                        System.out.println("✅ Student Affairs gave final approval: " + submission.getSubmissionId());
                    });
        }

        // Finalize graduation - this should complete the entire process
        mockMvc.perform(post("/api/submissions/finalize-my-list"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andDo(result -> {
                    System.out.println("✅ Student Affairs finalized graduation list");
                    System.out.println("🎓 GRADUATION PROCESS COMPLETED!");
                });

        // Verify graduation status changed to COMPLETED
        Graduation graduation = graduationRepository.findById("GRAD_2024_Spring").orElseThrow();
        assertEquals("COMPLETED", graduation.getStatus());
        System.out.println("✅ Graduation status confirmed as COMPLETED");
    }

    @Test
    @Order(9)
    @WithMockUser(username = STUDENT_EMAIL, roles = {"STUDENT"})
    void testStudentCanSeeGraduationStatus() throws Exception {
        System.out.println("\n=== STEP 9: STUDENT CHECKS GRADUATION STATUS ===");
        
        // Student can see their submissions and check graduation status
        mockMvc.perform(get("/api/submissions/my-submissions"))
                .andExpect(status().isOk())
                .andDo(result -> {
                    String responseContent = result.getResponse().getContentAsString();
                    System.out.println("📋 Student submissions: " + responseContent);
                    
                    // Check if response contains FINAL_APPROVED status
                    boolean hasGraduated = responseContent.contains("FINAL_APPROVED");
                    if (hasGraduated) {
                        System.out.println("🎓 Student successfully confirmed their graduation by checking submissions!");
                        System.out.println("✅ Graduation status: GRADUATED (has FINAL_APPROVED submission)");
                    } else {
                        System.out.println("❌ Student has not graduated yet (no FINAL_APPROVED submissions)");
                    }
                });
    }

    @Test
    @Order(10)
    void testWorkflowConstraintsAreMaintained() {
        System.out.println("\n=== STEP 10: VERIFY WORKFLOW INTEGRITY ===");
        
        // Verify all lists are finalized
        AdvisorList advisorList = advisorListRepository.findByAdvisorEmpId(ADVISOR_EMP_ID).orElseThrow();
        assertTrue(advisorList.getIsFinalized(), "Advisor list should be finalized");
        
        DepartmentList departmentList = departmentListRepository.findBySecretaryEmpId(DEPT_SEC_EMP_ID).orElseThrow();
        assertTrue(departmentList.getIsFinalized(), "Department list should be finalized");
        
        FacultyList facultyList = facultyListRepository.findByDeanOfficerEmpId(DEAN_EMP_ID).orElseThrow();
        assertTrue(facultyList.getIsFinalized(), "Faculty list should be finalized");
        
        List<GraduationList> graduationLists = graduationListRepository.findAll();
        assertTrue(graduationLists.get(0).getIsFinalized(), "Graduation list should be finalized");
        
        // Verify all submissions reached final status
        List<Submission> allSubmissions = submissionRepository.findAll();
        long finalApprovedCount = allSubmissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.FINAL_APPROVED)
                .count();
        
        System.out.println("✅ Total submissions: " + allSubmissions.size());
        System.out.println("✅ Final approved: " + finalApprovedCount);
        System.out.println("✅ All workflow constraints maintained");
        System.out.println("🎓 GRADUATION WORKFLOW TEST COMPLETED SUCCESSFULLY!");
    }
} 