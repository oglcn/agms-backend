package com.agms.backend.config;

import com.agms.backend.entity.*;
import com.agms.backend.repository.*;
import com.agms.backend.service.AuthenticationService;
import com.agms.backend.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

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
    private final PasswordEncoder passwordEncoder;

    @Bean
    @Transactional
    public CommandLineRunner initializeData() {
        return args -> {
            try {
                if (userRepository.count() == 0) {
                    log.info("Starting data initialization...");
                    
                    // Create Student Affairs Officer
                    log.debug("Creating Student Affairs Officer...");
                    var studentAffairsOfficer = createUser("Student", "Affairs", "studentaffairs@iyte.edu.tr", "password", Role.STUDENT_AFFAIRS, "U101");
                    createStudentAffairs(studentAffairsOfficer, "SA101");

                    // Create Dean Officer
                    log.debug("Creating Dean Officer...");
                    var deanOfficer = createUser("Dean", "Officer", "dean@iyte.edu.tr", "password", Role.DEAN_OFFICER, "U102");
                    createDeanOfficer(deanOfficer, "DO102");

                    // Create Department Secretary
                    log.debug("Creating Department Secretary...");
                    var secretary = createUser("Department", "Secretary", "secretary@iyte.edu.tr", "password", Role.DEPARTMENT_SECRETARY, "U103");
                    createDepartmentSecretary(secretary, "DS103");

                    // Create Advisor
                    log.debug("Creating Advisor...");
                    var advisor = createUser("Professor", "Advisor", "advisor@iyte.edu.tr", "password", Role.ADVISOR, "U104");
                    createAdvisor(advisor, "ADV104");

                    // Create Student
                    log.debug("Creating Student...");
                    var student = createUser("Student", "Test", "student@std.iyte.edu.tr", "password", Role.STUDENT, "U105");
                    createStudent(student, "CS105");

                    // Create Graduation Records
                    log.debug("Creating Graduation Records...");
                    var graduation = createGraduation("GR101", LocalDate.now(), "Spring 2024", "SA101");
                    var graduationList = createGraduationList("GL101", graduation);

                    // Create Faculty Lists
                    log.debug("Creating Faculty Lists...");
                    var facultyList = createFacultyList("FL101", "Engineering", "DO102", graduationList);

                    // Create Department Lists
                    log.debug("Creating Department Lists...");
                    var departmentList = createDepartmentList("DL101", "Computer Engineering", "DS103", facultyList);

                    // Create Advisor Lists
                    log.debug("Creating Advisor Lists...");
                    var advisorList = createAdvisorList("AL101", "ADV104", departmentList);

                    // Assign advisor to student
                    log.debug("Assigning advisor to student...");
                    assignAdvisorToStudent("CS105", advisorList);

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

    private User createUser(String firstName, String lastName, String email, String password, Role role, String userId) {
        try {
            var request = RegisterRequest.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .password(password)
                    .role(role)
                    .build();

            User user = User.builder()
                    .id(userId)
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(role)
                    .build();
            return userRepository.save(user);
        } catch (Exception e) {
            log.error("Failed to create user with ID {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    private void createStudentAffairs(User user, String empId) {
        var studentAffairs = StudentAffairs.builder()
                .empId(empId)
                .user(user)
                .build();
        studentAffairsRepository.save(studentAffairs);
    }

    private void createDeanOfficer(User user, String empId) {
        var deanOfficer = DeanOfficer.builder()
                .empId(empId)
                .user(user)
                .build();
        deanOfficerRepository.save(deanOfficer);
    }

    private void createDepartmentSecretary(User user, String empId) {
        var secretary = DepartmentSecretary.builder()
                .empId(empId)
                .user(user)
                .build();
        secretaryRepository.save(secretary);
    }

    private void createAdvisor(User user, String empId) {
        var advisor = Advisor.builder()
                .empId(empId)
                .user(user)
                .build();
        advisorRepository.save(advisor);
    }

    private void createStudent(User user, String studentId) {
        var student = Student.builder()
                .studentId(studentId)
                .user(user)
                .build();
        studentRepository.save(student);
    }

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

    private void assignAdvisorToStudent(String studentId, AdvisorList advisorList) {
        var student = studentRepository.findByStudentId(studentId).orElseThrow();
        student.setAdvisorList(advisorList);
        studentRepository.save(student);
    }
} 