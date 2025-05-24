package com.agms.backend.config;

import com.agms.backend.model.*;
import com.agms.backend.model.users.*;
import com.agms.backend.repository.*;
import com.agms.backend.service.UbysService;
import com.agms.backend.dto.RegisterRequest;
import com.agms.backend.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final AdvisorRepository advisorRepository;
    private final DepartmentSecretaryRepository secretaryRepository;
    private final DeanOfficerRepository deanOfficerRepository;
    private final StudentAffairsRepository studentAffairsRepository;
    private final GraduationRepository graduationRepository;
    private final GraduationListRepository graduationListRepository;
    private final FacultyListRepository facultyListRepository;
    private final DepartmentListRepository departmentListRepository;
    private final AdvisorListRepository advisorListRepository;
    private final AuthenticationService authenticationService;
    private final UbysService ubysService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Transactional
    public CommandLineRunner initializeData() {
        return args -> {
            try {
                if (userRepository.count() == 0) {
                    log.info("Starting data initialization...");

                    // Create administrative users first
                    createAdministrativeUsers();
                    
                    // Initialize all students from ubys.json
                    initializeStudentsFromUbys();

                    log.info("Data initialization completed successfully!");
                } else {
                    log.info("Database is not empty. Skipping data initialization.");
                }
            } catch (DataIntegrityViolationException e) {
                log.error("Database integrity violation during initialization: {}", e.getMessage());
                throw new RuntimeException("Failed to initialize data due to integrity violation", e);
            } catch (Exception e) {
                log.error("Error during data initialization: {}", e.getMessage());
                throw new RuntimeException("Failed to initialize data", e);
            }
        };
    }

    private void createAdministrativeUsers() {
        log.debug("Creating administrative users...");

        // Create Student Affairs Officer
        createUser("Student", "Affairs", "studentaffairs@iyte.edu.tr", "password", Role.STUDENT_AFFAIRS, null);

        // Create Dean Officers  
        createUser("Dean", "Officer1", "dean1@iyte.edu.tr", "password", Role.DEAN_OFFICER, null);
        createUser("Dean", "Officer2", "dean2@iyte.edu.tr", "password", Role.DEAN_OFFICER, null);

        // Create Department Secretaries
        createUser("Department", "Secretary1", "secretary1@iyte.edu.tr", "password", Role.DEPARTMENT_SECRETARY, null);
        createUser("Department", "Secretary2", "secretary2@iyte.edu.tr", "password", Role.DEPARTMENT_SECRETARY, null);

        // Create Advisors
        createUser("Professor", "Advisor1", "advisor1@iyte.edu.tr", "password", Role.ADVISOR, null);
        createUser("Professor", "Advisor2", "advisor2@iyte.edu.tr", "password", Role.ADVISOR, null);

        log.debug("Administrative users created successfully.");
    }

    private void initializeStudentsFromUbys() {
        log.debug("Initializing students from ubys.json...");
        
        try {
            // Get all students from ubys.json for database initialization
            List<Student> studentsFromUbys = ubysService.getAllStudentsForDbInitialization();
            
            log.info("Found {} students in ubys.json to initialize", studentsFromUbys.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (Student student : studentsFromUbys) {
                try {
                    // Set a default password for all students from ubys.json
                    student.setPassword(passwordEncoder.encode("password123"));
                    
                    // Save the student to database
                    studentRepository.save(student);
                    successCount++;
                    
                    if (successCount % 10 == 0) {
                        log.debug("Initialized {} students so far...", successCount);
                    }
                    
                } catch (Exception e) {
                    failCount++;
                    log.warn("Failed to initialize student {}: {}", student.getStudentNumber(), e.getMessage());
                }
            }
            
            log.info("Student initialization completed. Success: {}, Failed: {}", successCount, failCount);
            
        } catch (Exception e) {
            log.error("Error initializing students from ubys.json: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize students from ubys.json", e);
        }
    }

    private void createUser(String firstName, String lastName, String email, String password, Role role, String studentNumber) {
        try {
            var request = RegisterRequest.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .password(password)
                    .role(role)
                    .build();

            // For students, provide the student number
            if (role == Role.STUDENT && studentNumber != null) {
                request.setStudentNumber(studentNumber);
            }

            // Use AuthenticationService to create the user with proper inheritance
            authenticationService.register(request);
        } catch (Exception e) {
            log.error("Failed to create user {}: {}", email, e.getMessage());
            throw e;
        }
    }

    // Additional helper methods for creating organizational structure can be added here
    // For example: createGraduation, createGraduationList, etc. as needed

    private Graduation createGraduation(String graduationId, LocalDate requestDate, String term, String studentAffairsId) {
        var studentAffairs = studentAffairsRepository.findByEmpId(studentAffairsId).orElseThrow();
        var graduation = Graduation.builder()
                .graduationId(graduationId)
                .requestDate(requestDate)
                .term(term)
                .studentAffairs(studentAffairs)
                .build();
        return graduationRepository.save(graduation);
    }

    private GraduationList createGraduationList(String listId, Graduation graduation) {
        var graduationList = GraduationList.builder()
                .listId(listId)
                .creationDate(LocalDate.now())
                .graduation(graduation)
                .build();
        return graduationListRepository.save(graduationList);
    }

    private FacultyList createFacultyList(String facultyListId, String faculty, String deanOfficerId, GraduationList graduationList) {
        var deanOfficer = deanOfficerRepository.findByEmpId(deanOfficerId).orElseThrow();
        var facultyList = FacultyList.builder()
                .facultyListId(facultyListId)
                .creationDate(LocalDate.now())
                .faculty(faculty)
                .deanOfficer(deanOfficer)
                .graduationList(graduationList)
                .build();
        return facultyListRepository.save(facultyList);
    }

    private DepartmentList createDepartmentList(String deptListId, String department, String secretaryId, FacultyList facultyList) {
        var secretary = secretaryRepository.findByEmpId(secretaryId).orElseThrow();
        var departmentList = DepartmentList.builder()
                .deptListId(deptListId)
                .creationDate(LocalDate.now())
                .department(department)
                .secretary(secretary)
                .facultyList(facultyList)
                .build();
        return departmentListRepository.save(departmentList);
    }

    private AdvisorList createAdvisorList(String advisorListId, String advisorId, DepartmentList departmentList) {
        var advisor = advisorRepository.findByEmpId(advisorId).orElseThrow();
        var advisorList = AdvisorList.builder()
                .advisorListId(advisorListId)
                .creationDate(LocalDate.now())
                .advisor(advisor)
                .departmentList(departmentList)
                .build();
        return advisorListRepository.save(advisorList);
    }

    private void assignAdvisorToStudent(String studentNumber, AdvisorList advisorList) {
        var student = studentRepository.findByStudentNumber(studentNumber).orElseThrow();
        student.setAdvisorList(advisorList);
        studentRepository.save(student);
    }
}
