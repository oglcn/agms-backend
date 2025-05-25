package com.agms.backend.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class RegularGraduationIntegrationTest {

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
    private Advisor advisor;
    private Student student1;
    private Student student2;
    private AdvisorList advisorList;

    @BeforeEach
    @Transactional
    void setUp() {
        log.info("=== Setting up Regular Graduation Integration Test ===");

        // Create the complete hierarchy from top to bottom
        
        // 1. Create StudentAffairs
        studentAffairs = StudentAffairs.builder()
                .empId("SA_REG_TEST_001")
                .firstName("Test")
                .lastName("StudentAffairs")
                .email("test.sa.reg@edu")
                .password("password123")
                .build();
        studentAffairs = studentAffairsRepository.save(studentAffairs);

        // 2. Create DeanOfficer
        deanOfficer = DeanOfficer.builder()
                .empId("DO_REG_TEST_001")
                .firstName("Test")
                .lastName("DeanOfficer")
                .email("test.do.reg@edu")
                .password("password123")
                .studentAffairs(studentAffairs)
                .build();
        deanOfficer = deanOfficerRepository.save(deanOfficer);

        // 3. Create DepartmentSecretary
        departmentSecretary = DepartmentSecretary.builder()
                .empId("DS_REG_TEST_001")
                .firstName("Test")
                .lastName("DepartmentSecretary")
                .email("test.ds.reg@edu")
                .password("password123")
                .deanOfficer(deanOfficer)
                .build();
        departmentSecretary = departmentSecretaryRepository.save(departmentSecretary);

        // 4. Create Graduation
        Graduation graduation = Graduation.builder()
                .graduationId("GRAD_REG_TEST_001")
                .requestDate(new Timestamp(System.currentTimeMillis()))
                .term("Spring 2025")
                .status("IN_PROGRESS")
                .studentAffairs(studentAffairs)
                .build();
        graduation = graduationRepository.save(graduation);

        // 5. Create GraduationList
        GraduationList graduationList = GraduationList.builder()
                .listId("GL_REG_TEST_001")
                .creationDate(new Timestamp(System.currentTimeMillis()))
                .graduation(graduation)
                .build();
        graduationList = graduationListRepository.save(graduationList);

        // 6. Create FacultyList
        FacultyList facultyList = FacultyList.builder()
                .facultyListId("FL_REG_TEST_001")
                .creationDate(new Timestamp(System.currentTimeMillis()))
                .faculty("Engineering")
                .deanOfficer(deanOfficer)
                .graduationList(graduationList)
                .build();
        facultyList = facultyListRepository.save(facultyList);

        // 7. Create DepartmentList
        DepartmentList departmentList = DepartmentList.builder()
                .deptListId("DL_REG_TEST_001")
                .creationDate(new Timestamp(System.currentTimeMillis()))
                .department("Computer Engineering")
                .secretary(departmentSecretary)
                .facultyList(facultyList)
                .build();
        departmentList = departmentListRepository.save(departmentList);

        // 8. Create Advisor
        advisor = Advisor.builder()
                .empId("ADV_REG_TEST_001")
                .firstName("Test")
                .lastName("Advisor")
                .email("test.advisor.reg@edu")
                .password("password123")
                .departmentSecretary(departmentSecretary)
                .build();
        advisor = advisorRepository.save(advisor);

        // 9. Create AdvisorList
        advisorList = AdvisorList.builder()
                .advisorListId("AL_REG_TEST_001")
                .creationDate(new Timestamp(System.currentTimeMillis()))
                .advisor(advisor)
                .departmentList(departmentList)
                .build();
        advisorList = advisorListRepository.save(advisorList);

        // Update advisor with advisor list
        advisor.setAdvisorList(advisorList);
        advisor = advisorRepository.save(advisor);

        // 10. Create test students
        // Student 1 - Eligible for graduation (has required data in ubys.json: S001)
        student1 = Student.builder()
                .studentNumber("S001")
                .firstName("Test")
                .lastName("Student1")
                .email("s001@std.iyte.edu.tr")
                .password("password123")
                .department("Computer Engineering")
                .advisor(advisor)
                .build();
        student1 = studentRepository.save(student1);

        // Student 2 - Not eligible for graduation (has required data in ubys.json: S002)
        student2 = Student.builder()
                .studentNumber("S002")
                .firstName("Test")
                .lastName("Student2")
                .email("s002@std.iyte.edu.tr")
                .password("password123")
                .department("Computer Engineering")
                .advisor(advisor)
                .build();
        student2 = studentRepository.save(student2);

        log.info("Test setup completed successfully");
    }

    @Test
    @Transactional
    void testStartRegularGraduation() {
        log.info("=== Testing Regular Graduation Process ===");

        // Start the regular graduation process
        String term = "Spring 2025";
        List<SubmissionResponse> createdSubmissions = submissionService.startRegularGraduation(term);

        // Verify submissions were created
        assertNotNull(createdSubmissions);
        assertTrue(createdSubmissions.size() > 0, "At least one submission should be created for eligible students");

        log.info("Created {} submissions for regular graduation", createdSubmissions.size());

        // Verify that all created submissions have the correct properties
        for (SubmissionResponse submission : createdSubmissions) {
            assertNotNull(submission.getSubmissionId());
            assertEquals(SubmissionStatus.PENDING, submission.getStatus());
            assertNotNull(submission.getStudentNumber());
            assertNotNull(submission.getStudentName());
            assertNotNull(submission.getAdvisorListId());
            assertTrue(submission.getContent().contains("Regular graduation application"));
            assertTrue(submission.getContent().contains(term));

            log.info("Verified submission for student: {} with ID: {}", 
                submission.getStudentNumber(), submission.getSubmissionId());
        }

        // Verify that only eligible students got submissions
        // According to ubys.json, S001 should be eligible (good grades, curriculum completed)
        // S002 might not be eligible depending on the data
        boolean foundS001 = createdSubmissions.stream()
                .anyMatch(s -> "S001".equals(s.getStudentNumber()));

        assertTrue(foundS001, "Student S001 should have a submission created as they are eligible");

        log.info("=== Regular Graduation Process Test Completed Successfully ===");
    }

    @Test
    @Transactional
    void testStartRegularGraduationPreventsDoubleSubmission() {
        log.info("=== Testing Regular Graduation Double Submission Prevention ===");

        String term = "Spring 2025";

        // Start the regular graduation process first time
        List<SubmissionResponse> firstRun = submissionService.startRegularGraduation(term);
        int firstRunCount = firstRun.size();

        assertTrue(firstRunCount > 0, "First run should create submissions for eligible students");
        log.info("First run created {} submissions", firstRunCount);

        // Start the regular graduation process second time
        List<SubmissionResponse> secondRun = submissionService.startRegularGraduation(term);
        int secondRunCount = secondRun.size();

        // Second run should create 0 submissions since students already have pending submissions
        assertEquals(0, secondRunCount, 
            "Second run should not create any submissions as students already have active pending submissions");

        log.info("Second run correctly created {} submissions (prevented duplicates)", secondRunCount);

        log.info("=== Double Submission Prevention Test Completed Successfully ===");
    }
} 