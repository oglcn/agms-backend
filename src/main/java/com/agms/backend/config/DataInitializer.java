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
                    var studentAffairsOfficer = createUser("Student", "Affairs", "studentaffairs@iyte.edu.tr",
                            "password", Role.STUDENT_AFFAIRS, "U101");
                    createStudentAffairs(studentAffairsOfficer, "SA101");

                    // Create Dean Officers
                    log.debug("Creating Dean Officers...");
                    var deanOfficer1 = createUser("Dean", "Officer1", "dean1@iyte.edu.tr", "password",
                            Role.DEAN_OFFICER,
                            "U102");
                    createDeanOfficer(deanOfficer1, "DO102");

                    var deanOfficer2 = createUser("Dean", "Officer2", "dean2@iyte.edu.tr", "password",
                            Role.DEAN_OFFICER,
                            "U106");
                    createDeanOfficer(deanOfficer2, "DO106");

                    // Create Department Secretaries
                    log.debug("Creating Department Secretaries...");
                    var secretary1 = createUser("Department", "Secretary1", "secretary1@iyte.edu.tr", "password",
                            Role.DEPARTMENT_SECRETARY, "U103");
                    createDepartmentSecretary(secretary1, "DS103");

                    var secretary2 = createUser("Department", "Secretary2", "secretary2@iyte.edu.tr", "password",
                            Role.DEPARTMENT_SECRETARY, "U107");
                    createDepartmentSecretary(secretary2, "DS107");

                    // Create Advisors
                    log.debug("Creating Advisors...");
                    var advisor1 = createUser("Professor", "Advisor1", "advisor1@iyte.edu.tr", "password", Role.ADVISOR,
                            "U104");
                    createAdvisor(advisor1, "ADV104");

                    var advisor2 = createUser("Professor", "Advisor2", "advisor2@iyte.edu.tr", "password", Role.ADVISOR,
                            "U108");
                    createAdvisor(advisor2, "ADV108");

                    // Create Students
                    log.debug("Creating Students...");
                    var student1 = createUser("Student1", "Test", "student1@std.iyte.edu.tr", "password", Role.STUDENT,
                            "U105");
                    createStudent(student1, "CS105");

                    var student2 = createUser("Student2", "Test", "student2@std.iyte.edu.tr", "password", Role.STUDENT,
                            "U109");
                    createStudent(student2, "CS109");

                    var student3 = createUser("Student3", "Test", "student3@std.iyte.edu.tr", "password", Role.STUDENT,
                            "U110");
                    createStudent(student3, "CS110");

                    var student4 = createUser("Student4", "Test", "student4@std.iyte.edu.tr", "password", Role.STUDENT,
                            "U111");
                    createStudent(student4, "CS111");

                    var student5 = createUser("Student5", "Test", "student5@std.iyte.edu.tr", "password", Role.STUDENT,
                            "U112");
                    createStudent(student5, "CS112");

                    var student6 = createUser("Student6", "Test", "student6@std.iyte.edu.tr", "password", Role.STUDENT,
                            "U113");
                    createStudent(student6, "CS113");

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


    private User createUser(String firstName, String lastName, String email, String password, Role role,
            String userId) {
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


    private Graduation createGraduation(String graduationId, LocalDate requestDate, String term,
            String studentAffairsId) {
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


    private FacultyList createFacultyList(String facultyListId, String faculty, String deanOfficerId,
            GraduationList graduationList) {
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


    private DepartmentList createDepartmentList(String deptListId, String department, String secretaryId,
            FacultyList facultyList) {
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
