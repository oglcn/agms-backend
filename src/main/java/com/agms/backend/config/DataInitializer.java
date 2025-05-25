package com.agms.backend.config;

import com.agms.backend.model.*;
import com.agms.backend.model.users.*;
import com.agms.backend.repository.*;
import com.agms.backend.service.UbysService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Component
public class DataInitializer implements CommandLineRunner {

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
    private final UbysService ubysService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.data.initialization.enabled:true}")
    private boolean dataInitializationEnabled;

    @Override
    public void run(String... args) {
        if (!dataInitializationEnabled) {
            log.info("Data initialization is disabled. Skipping UBYS data initialization.");
            return;
        }

        log.info("Starting data initialization from UBYS...");

        try {
            // Check if data already exists
            if (userRepository.count() > 0) {
                log.info("Data already exists. Skipping initialization.");
                return;
            }

            // Initialize all entities from UBYS in the correct order
            initializeAllEntitiesFromUbys();

            // Initialize graduation hierarchy
            initializeGraduationHierarchy();

            log.info("Data initialization completed successfully!");

        } catch (Exception e) {
            log.error("Error during data initialization: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize data from UBYS", e);
        }
    }

    private void initializeAllEntitiesFromUbys() {
        log.info("Initializing all entities from UBYS data...");

        // Initialize in dependency order
        initializeStudentAffairsFromUbys();
        initializeDeanOfficersFromUbys();
        initializeDepartmentSecretariesFromUbys();
        initializeAdvisorsFromUbys();
        initializeStudentsFromUbys();

        log.info("All entities initialized from UBYS data successfully!");
    }

    private void initializeStudentAffairsFromUbys() {
        log.debug("Initializing student affairs from ubys.json...");

        try {
            List<StudentAffairs> studentAffairsList = ubysService.getAllStudentAffairsForDbInitialization();
            log.info("Found {} student affairs in ubys.json to initialize", studentAffairsList.size());

            for (StudentAffairs studentAffairs : studentAffairsList) {
                studentAffairs.setPassword(passwordEncoder.encode("Password123!"));
                studentAffairsRepository.save(studentAffairs);
                log.debug("Initialized StudentAffairs: {}", studentAffairs.getEmpId());
            }

        } catch (Exception e) {
            log.error("Error initializing student affairs from ubys.json: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize student affairs from ubys.json", e);
        }
    }

    private void initializeDeanOfficersFromUbys() {
        log.debug("Initializing dean officers from ubys.json...");

        try {
            List<DeanOfficer> deanOfficers = ubysService.getAllDeanOfficersForDbInitialization();
            log.info("Found {} dean officers in ubys.json to initialize", deanOfficers.size());

            // Get the StudentAffairs (assuming there's only one)
            StudentAffairs studentAffairs = studentAffairsRepository.findAll().get(0);

            for (DeanOfficer deanOfficer : deanOfficers) {
                deanOfficer.setPassword(passwordEncoder.encode("Password123!"));
                deanOfficer.setStudentAffairs(studentAffairs);

                // The faculty field should already be set from UBYS data
                String faculty = deanOfficer.getFaculty();
                if (faculty == null) {
                    log.warn("DeanOfficer {} has no faculty field from UBYS data", deanOfficer.getEmpId());
                }

                deanOfficerRepository.save(deanOfficer);
                log.debug("Initialized DeanOfficer: {} for faculty: {}", deanOfficer.getEmpId(), faculty);
            }

        } catch (Exception e) {
            log.error("Error initializing dean officers from ubys.json: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize dean officers from ubys.json", e);
        }
    }

    private void initializeDepartmentSecretariesFromUbys() {
        log.debug("Initializing department secretaries from ubys.json...");

        try {
            List<DepartmentSecretary> departmentSecretaries = ubysService
                    .getAllDepartmentSecretariesForDbInitialization();
            log.info("Found {} department secretaries in ubys.json to initialize", departmentSecretaries.size());

            for (DepartmentSecretary departmentSecretary : departmentSecretaries) {
                departmentSecretary.setPassword(passwordEncoder.encode("Password123!"));

                // The department field should already be set from UBYS data
                String department = departmentSecretary.getDepartment();
                if (department == null) {
                    log.warn("DepartmentSecretary {} has no department field from UBYS data",
                            departmentSecretary.getEmpId());
                    continue;
                }

                // Auto-match dean officer based on department -> faculty mapping
                String facultyName = getFacultyForDepartment(department);
                if (facultyName != null) {
                    DeanOfficer deanOfficer = deanOfficerRepository.findByFaculty(facultyName).orElse(null);
                    if (deanOfficer != null) {
                        departmentSecretary.setDeanOfficer(deanOfficer);
                        log.debug("Auto-matched DepartmentSecretary {} (dept: {}) with DeanOfficer {} (faculty: {})",
                                departmentSecretary.getEmpId(), department,
                                deanOfficer.getEmpId(), facultyName);
                    } else {
                        log.warn("No dean officer found for faculty: {}", facultyName);
                    }
                } else {
                    log.warn("Could not determine faculty for department: {}", department);
                }

                secretaryRepository.save(departmentSecretary);
                log.debug("Initialized DepartmentSecretary: {} for department: {}",
                        departmentSecretary.getEmpId(), department);
            }

        } catch (Exception e) {
            log.error("Error initializing department secretaries from ubys.json: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize department secretaries from ubys.json", e);
        }
    }

    private void initializeAdvisorsFromUbys() {
        log.debug("Initializing advisors from ubys.json...");

        try {
            List<Advisor> advisors = ubysService.getAllAdvisorsForDbInitialization();
            log.info("Found {} advisors in ubys.json to initialize", advisors.size());

            for (Advisor advisor : advisors) {
                advisor.setPassword(passwordEncoder.encode("Password123!"));

                // The department field should already be set from UBYS data
                String department = advisor.getDepartment();
                if (department == null) {
                    log.warn("Advisor {} has no department field from UBYS data", advisor.getEmpId());
                    continue;
                }

                // Auto-match department secretary based on advisor's department
                DepartmentSecretary departmentSecretary = secretaryRepository.findByDepartment(department).orElse(null);
                if (departmentSecretary != null) {
                    advisor.setDepartmentSecretary(departmentSecretary);
                    log.debug("Auto-matched Advisor {} with DepartmentSecretary {} (dept: {})",
                            advisor.getEmpId(), departmentSecretary.getEmpId(), department);
                } else {
                    log.warn("No department secretary found for department: {}", department);
                }

                advisorRepository.save(advisor);
                log.debug("Initialized Advisor: {}", advisor.getEmpId());
            }

        } catch (Exception e) {
            log.error("Error initializing advisors from ubys.json: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize advisors from ubys.json", e);
        }
    }

    private void initializeStudentsFromUbys() {
        log.debug("Initializing students from ubys.json...");

        try {
            // Get the raw UBYS data to access advisorId information
            Map<String, Object> ubysData = ubysService.getAllData();
            Map<String, Object> studentsSection = (Map<String, Object>) ubysData.get("students");

            List<Student> studentsFromUbys = ubysService.getAllStudentsForDbInitialization();
            log.info("Found {} students in ubys.json to initialize", studentsFromUbys.size());

            int successCount = 0;
            int failCount = 0;

            for (Student student : studentsFromUbys) {
                try {
                    student.setPassword(passwordEncoder.encode("Password123!"));

                    // Get the advisorId from the original UBYS data (object-oriented approach)
                    Map<String, Object> studentData = (Map<String, Object>) studentsSection
                            .get(student.getStudentNumber());
                    String advisorId = studentData != null ? (String) studentData.get("advisorId") : null;

                    if (advisorId != null) {
                        // Find the specific advisor by empId
                        Advisor assignedAdvisor = advisorRepository.findByEmpId(advisorId).orElse(null);
                        if (assignedAdvisor != null) {
                            // Verify that the advisor's department matches the student's department
                            if (assignedAdvisor.getDepartment().equals(student.getDepartment())) {
                                student.setAdvisor(assignedAdvisor);

                                if (successCount % 10 == 0) {
                                    log.debug("Assigned student {} (dept: {}) to advisor {} (dept: {})",
                                            student.getStudentNumber(), student.getDepartment(),
                                            assignedAdvisor.getEmpId(), assignedAdvisor.getDepartment());
                                }
                            } else {
                                log.warn("Department mismatch: Student {} (dept: {}) assigned to advisor {} (dept: {})",
                                        student.getStudentNumber(), student.getDepartment(),
                                        assignedAdvisor.getEmpId(), assignedAdvisor.getDepartment());
                            }
                        } else {
                            log.warn("Advisor with ID {} not found for student {}", advisorId,
                                    student.getStudentNumber());
                        }
                    } else {
                        log.warn("Student {} has no advisorId field from UBYS data", student.getStudentNumber());
                    }

                    studentRepository.save(student);
                    successCount++;

                    if (successCount % 50 == 0) {
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

    private Graduation createGraduation(String graduationId, Timestamp requestDate, String term,
            String status, StudentAffairs studentAffairs) {
        var graduation = Graduation.builder()
                .graduationId(graduationId)
                .requestDate(requestDate)
                .term(term)
                .status(status)
                .studentAffairs(studentAffairs)
                .build();
        return graduationRepository.save(graduation);
    }

    private GraduationList createGraduationList(String listId, Graduation graduation) {
        var graduationList = GraduationList.builder()
                .listId(listId)
                .creationDate(new Timestamp(System.currentTimeMillis()))
                .graduation(graduation)
                .build();
        return graduationListRepository.save(graduationList);
    }

    private FacultyList createFacultyList(String facultyListId, String faculty, DeanOfficer deanOfficer,
            GraduationList graduationList) {
        var facultyList = FacultyList.builder()
                .facultyListId(facultyListId)
                .faculty(faculty)
                .creationDate(new Timestamp(System.currentTimeMillis()))
                .deanOfficer(deanOfficer)
                .graduationList(graduationList)
                .build();
        return facultyListRepository.save(facultyList);
    }

    private DepartmentList createDepartmentList(String deptListId, String department,
            DepartmentSecretary departmentSecretary, FacultyList facultyList) {
        var departmentList = DepartmentList.builder()
                .deptListId(deptListId)
                .department(department)
                .creationDate(new Timestamp(System.currentTimeMillis()))
                .secretary(departmentSecretary)
                .facultyList(facultyList)
                .build();
        return departmentListRepository.save(departmentList);
    }

    private AdvisorList createAdvisorList(String advisorListId, Advisor advisor, DepartmentList departmentList) {
        var advisorList = AdvisorList.builder()
                .advisorListId(advisorListId)
                .creationDate(new Timestamp(System.currentTimeMillis()))
                .advisor(advisor)
                .departmentList(departmentList)
                .build();
        return advisorListRepository.save(advisorList);
    }

    private void initializeGraduationHierarchy() {
        log.debug("Initializing graduation hierarchy...");

        try {
            // Create a default graduation for current term
            StudentAffairs studentAffairs = studentAffairsRepository.findAll().get(0);
            String currentTerm = "2024-Spring";
            String graduationId = "GRAD_" + currentTerm.replace("-", "_");

            Graduation graduation = createGraduation(graduationId, new Timestamp(System.currentTimeMillis()),
                    currentTerm, "IN_PROGRESS", studentAffairs);

            // Create graduation list
            String graduationListId = "GL_" + graduationId;
            GraduationList graduationList = createGraduationList(graduationListId, graduation);

            // Create faculty lists for each dean officer
            List<DeanOfficer> deanOfficers = deanOfficerRepository.findAll();
            for (DeanOfficer deanOfficer : deanOfficers) {
                String facultyListId = "FL_" + deanOfficer.getEmpId();
                FacultyList facultyList = createFacultyList(facultyListId, deanOfficer.getFaculty(),
                        deanOfficer, graduationList);
                log.debug("Created FacultyList: {} for faculty: {}", facultyListId, deanOfficer.getFaculty());

                // Create department lists for each department secretary under this specific
                // dean officer
                List<DepartmentSecretary> departmentSecretaries = secretaryRepository
                        .findByDeanOfficerEmpId(deanOfficer.getEmpId());
                for (DepartmentSecretary secretary : departmentSecretaries) {
                    String deptListId = "DL_" + secretary.getEmpId();
                    DepartmentList departmentList = createDepartmentList(deptListId, secretary.getDepartment(),
                            secretary, facultyList);
                    log.debug("Created DepartmentList: {} for department: {}", deptListId, secretary.getDepartment());

                    // Create advisor lists for each advisor under this specific department
                    // secretary
                    List<Advisor> advisors = advisorRepository.findByDepartmentSecretaryEmpId(secretary.getEmpId());
                    for (Advisor advisor : advisors) {
                        String advisorListId = "AL_" + advisor.getEmpId();
                        AdvisorList advisorList = createAdvisorList(advisorListId, advisor, departmentList);
                        log.debug("Created AdvisorList: {} for advisor: {}", advisorListId, advisor.getEmpId());
                    }
                }
            }

            log.info("Graduation hierarchy initialized successfully for term: {}", currentTerm);

        } catch (Exception e) {
            log.error("Error initializing graduation hierarchy: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize graduation hierarchy", e);
        }
    }

    /**
     * Maps department names to their corresponding faculty names based on UBYS data
     * structure.
     * This method uses the actual faculty names from the dean officers in UBYS.
     */
    private String getFacultyForDepartment(String department) {
        switch (department) {
            // Engineering departments (matches UBYS data: "Engineering")
            case "Computer Engineering":
            case "Electronics and Communication Engineering":
            case "Civil Engineering":
            case "Mechanical Engineering":
            case "Bioengineering":
            case "Environmental Engineering":
            case "Energy Systems Engineering":
            case "Food Engineering":
            case "Chemical Engineering":
            case "Materials Science and Engineering":
                return "Faculty of Engineering";

            // Science departments
            case "Physics":
            case "Photonics":
            case "Chemistry":
            case "Mathematics":
            case "Molecular Biology and Genetics":
                return "Faculty of Science";

            // Architecture and Design departments
            case "Industrial Design":
            case "Architecture":
            case "City and Regional Planning":
                return "Faculty of Architecture and Design";

            default:
                log.warn("Unknown department: {}. Cannot determine faculty.", department);
                return null;
        }
    }
}
