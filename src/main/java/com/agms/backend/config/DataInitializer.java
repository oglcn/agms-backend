package com.agms.backend.config;

import com.agms.backend.model.*;
import com.agms.backend.model.users.*;
import com.agms.backend.repository.*;
import com.agms.backend.service.UbysService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.sql.Timestamp;

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

    @Override
    public void run(String... args) {
        try {
            if (userRepository.count() == 0) {
                log.info("Starting data initialization...");

                // Initialize all entities from ubys.json in the correct order
                initializeAllEntitiesFromUbys();

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
    }

    private void initializeAllEntitiesFromUbys() {
        log.debug("Initializing all entities from ubys.json...");

        try {
            // Step 1: Create StudentAffairs (top of hierarchy)
            initializeStudentAffairsFromUbys();

            // Step 2: Create DeanOfficers (depends on StudentAffairs)
            initializeDeanOfficersFromUbys();

            // Step 3: Create DepartmentSecretaries (depends on DeanOfficers)
            initializeDepartmentSecretariesFromUbys();

            // Step 4: Create Advisors (depends on DepartmentSecretaries)
            initializeAdvisorsFromUbys();

            // Step 5: Create Students (depends on Advisors)
            initializeStudentsFromUbys();

            // Step 6: Create complete organizational hierarchy for graduation system
            initializeGraduationHierarchy();

            log.info("All entities initialized successfully from ubys.json");

        } catch (Exception e) {
            log.error("Error initializing entities from ubys.json: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize entities from ubys.json", e);
        }
    }

    private void initializeStudentAffairsFromUbys() {
        log.debug("Initializing student affairs from ubys.json...");

        try {
            List<StudentAffairs> studentAffairsList = ubysService.getAllStudentAffairsForDbInitialization();
            log.info("Found {} student affairs officers in ubys.json to initialize", studentAffairsList.size());

            for (StudentAffairs studentAffairs : studentAffairsList) {
                studentAffairs.setPassword(passwordEncoder.encode("password123"));
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
                deanOfficer.setPassword(passwordEncoder.encode("password123"));
                deanOfficer.setStudentAffairs(studentAffairs);
                deanOfficerRepository.save(deanOfficer);
                log.debug("Initialized DeanOfficer: {}", deanOfficer.getEmpId());
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

            // Get the relationship data from JSON to establish dean officer relationships
            Map<String, String> deanOfficerRelationships = getDeanOfficerRelationships();

            for (DepartmentSecretary departmentSecretary : departmentSecretaries) {
                departmentSecretary.setPassword(passwordEncoder.encode("password123"));

                // Set dean officer relationship based on JSON data
                String deanOfficerId = deanOfficerRelationships.get(departmentSecretary.getEmpId());
                if (deanOfficerId != null) {
                    DeanOfficer deanOfficer = deanOfficerRepository.findByEmpId(deanOfficerId).orElse(null);
                    if (deanOfficer != null) {
                        departmentSecretary.setDeanOfficer(deanOfficer);
                    }
                }

                secretaryRepository.save(departmentSecretary);
                log.debug("Initialized DepartmentSecretary: {}", departmentSecretary.getEmpId());
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

            // Get the relationship data from JSON to establish department secretary
            // relationships
            Map<String, String> departmentSecretaryRelationships = getDepartmentSecretaryRelationships();

            for (Advisor advisor : advisors) {
                advisor.setPassword(passwordEncoder.encode("password123"));

                // Set department secretary relationship based on JSON data
                String departmentSecretaryId = departmentSecretaryRelationships.get(advisor.getEmpId());
                if (departmentSecretaryId != null) {
                    DepartmentSecretary departmentSecretary = secretaryRepository.findByEmpId(departmentSecretaryId)
                            .orElse(null);
                    if (departmentSecretary != null) {
                        advisor.setDepartmentSecretary(departmentSecretary);
                    }
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
            List<Student> studentsFromUbys = ubysService.getAllStudentsForDbInitialization();
            log.info("Found {} students in ubys.json to initialize", studentsFromUbys.size());

            // Get the relationship data from JSON to establish advisor relationships
            Map<String, String> advisorRelationships = getAdvisorRelationships();

            int successCount = 0;
            int failCount = 0;

            for (Student student : studentsFromUbys) {
                try {
                    student.setPassword(passwordEncoder.encode("password123"));

                    // Set advisor relationship based on JSON data
                    String advisorId = advisorRelationships.get(student.getStudentNumber());
                    if (advisorId != null) {
                        Advisor advisor = advisorRepository.findByEmpId(advisorId).orElse(null);
                        if (advisor != null) {
                            student.setAdvisor(advisor);
                        }
                    }

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

    // Helper methods to get relationship data from JSON
    private Map<String, String> getDeanOfficerRelationships() {
        // Maps department secretary empId to dean officer empId
        Map<String, String> relationships = new HashMap<>();
        relationships.put("DS101", "DO101");
        relationships.put("DS102", "DO101");
        relationships.put("DS103", "DO101");
        relationships.put("DS104", "DO101");
        relationships.put("DS105", "DO101");
        relationships.put("DS106", "DO101");
        relationships.put("DS107", "DO101");
        relationships.put("DS108", "DO101");
        relationships.put("DS109", "DO101");
        relationships.put("DS110", "DO101");
        relationships.put("DS111", "DO102");
        relationships.put("DS112", "DO102");
        relationships.put("DS113", "DO102");
        relationships.put("DS114", "DO102");
        relationships.put("DS115", "DO102");
        relationships.put("DS116", "DO103");
        relationships.put("DS117", "DO103");
        relationships.put("DS118", "DO103");
        return relationships;
    }

    private Map<String, String> getDepartmentSecretaryRelationships() {
        // Maps advisor empId to department secretary empId
        Map<String, String> relationships = new HashMap<>();
        relationships.put("ADV101", "DS101");
        relationships.put("ADV102", "DS101");
        relationships.put("ADV103", "DS101");
        relationships.put("ADV104", "DS102");
        relationships.put("ADV105", "DS102");
        relationships.put("ADV106", "DS103");
        relationships.put("ADV107", "DS103");
        relationships.put("ADV108", "DS104");
        relationships.put("ADV109", "DS104");
        relationships.put("ADV110", "DS105");
        relationships.put("ADV111", "DS105");
        relationships.put("ADV112", "DS106");
        relationships.put("ADV113", "DS106");
        relationships.put("ADV114", "DS107");
        relationships.put("ADV115", "DS107");
        relationships.put("ADV116", "DS108");
        relationships.put("ADV117", "DS108");
        relationships.put("ADV118", "DS109");
        relationships.put("ADV119", "DS109");
        relationships.put("ADV120", "DS110");
        relationships.put("ADV121", "DS110");
        relationships.put("ADV122", "DS111");
        relationships.put("ADV123", "DS111");
        relationships.put("ADV124", "DS112");
        relationships.put("ADV125", "DS112");
        relationships.put("ADV126", "DS113");
        relationships.put("ADV127", "DS113");
        relationships.put("ADV128", "DS114");
        relationships.put("ADV129", "DS114");
        relationships.put("ADV130", "DS115");
        relationships.put("ADV131", "DS115");
        relationships.put("ADV132", "DS116");
        relationships.put("ADV133", "DS116");
        relationships.put("ADV134", "DS117");
        relationships.put("ADV135", "DS117");
        relationships.put("ADV136", "DS118");
        relationships.put("ADV137", "DS118");
        return relationships;
    }

    private Map<String, String> getAdvisorRelationships() {
        // Maps student number to advisor empId
        Map<String, String> relationships = new HashMap<>();
        relationships.put("S101", "ADV101");
        relationships.put("S102", "ADV101");
        relationships.put("S103", "ADV101");
        relationships.put("S104", "ADV101");
        relationships.put("S105", "ADV102");
        relationships.put("S106", "ADV102");
        relationships.put("S107", "ADV103");
        relationships.put("S108", "ADV103");
        relationships.put("S109", "ADV104");
        relationships.put("S110", "ADV105");
        relationships.put("S111", "ADV106");
        relationships.put("S112", "ADV106");
        relationships.put("S113", "ADV107");
        relationships.put("S114", "ADV107");
        relationships.put("S115", "ADV108");
        relationships.put("S116", "ADV108");
        relationships.put("S117", "ADV109");
        relationships.put("S118", "ADV109");
        relationships.put("S119", "ADV110");
        relationships.put("S120", "ADV110");
        relationships.put("S121", "ADV111");
        relationships.put("S122", "ADV111");
        relationships.put("S123", "ADV112");
        relationships.put("S124", "ADV112");
        relationships.put("S125", "ADV113");
        relationships.put("S126", "ADV113");
        relationships.put("S127", "ADV114");
        relationships.put("S128", "ADV114");
        relationships.put("S129", "ADV115");
        relationships.put("S130", "ADV115");
        relationships.put("S131", "ADV116");
        relationships.put("S132", "ADV116");
        relationships.put("S133", "ADV117");
        relationships.put("S134", "ADV117");
        relationships.put("S135", "ADV118");
        relationships.put("S136", "ADV118");
        relationships.put("S137", "ADV119");
        relationships.put("S138", "ADV119");
        relationships.put("S139", "ADV120");
        relationships.put("S140", "ADV120");
        relationships.put("S141", "ADV121");
        relationships.put("S142", "ADV121");
        relationships.put("S143", "ADV122");
        relationships.put("S144", "ADV122");
        relationships.put("S145", "ADV123");
        relationships.put("S146", "ADV123");
        relationships.put("S147", "ADV124");
        relationships.put("S148", "ADV124");
        relationships.put("S149", "ADV125");
        relationships.put("S150", "ADV125");
        relationships.put("S151", "ADV126");
        relationships.put("S152", "ADV126");
        relationships.put("S153", "ADV127");
        relationships.put("S154", "ADV127");
        relationships.put("S155", "ADV128");
        relationships.put("S156", "ADV128");
        relationships.put("S157", "ADV129");
        relationships.put("S158", "ADV129");
        relationships.put("S159", "ADV130");
        relationships.put("S160", "ADV130");
        relationships.put("S161", "ADV131");
        relationships.put("S162", "ADV131");
        relationships.put("S163", "ADV132");
        relationships.put("S164", "ADV132");
        relationships.put("S165", "ADV133");
        relationships.put("S166", "ADV133");
        relationships.put("S167", "ADV134");
        relationships.put("S168", "ADV134");
        relationships.put("S169", "ADV135");
        relationships.put("S170", "ADV135");
        relationships.put("S171", "ADV136");
        relationships.put("S172", "ADV136");
        relationships.put("S173", "ADV137");
        relationships.put("S174", "ADV137");
        return relationships;
    }

    // Additional helper methods for creating organizational structure can be added
    // here
    // For example: createGraduation, createGraduationList, etc. as needed

    private Graduation createGraduation(String graduationId, Timestamp requestDate, String term,
            String type, StudentAffairs studentAffairs) {
        var graduation = Graduation.builder()
                .graduationId(graduationId)
                .requestDate(requestDate)
                .term(term)
                .type(type)
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
            Graduation graduation = createGraduation("GRAD_2025_SPRING", new Timestamp(System.currentTimeMillis()),
                    "Spring 2025", "Graduate", studentAffairs);

            // Create graduation list
            GraduationList graduationList = createGraduationList("GL_MAIN", graduation);

            // Create faculty lists for each dean officer
            Map<String, FacultyList> facultyListMap = new HashMap<>();
            List<DeanOfficer> deanOfficers = deanOfficerRepository.findAll();
            for (DeanOfficer deanOfficer : deanOfficers) {
                String facultyName = getFacultyName(deanOfficer.getEmpId());
                String facultyListId = "FL_" + deanOfficer.getEmpId();
                FacultyList facultyList = createFacultyList(facultyListId, facultyName, deanOfficer, graduationList);
                facultyListMap.put(deanOfficer.getEmpId(), facultyList);
                log.debug("Created FacultyList: {} for dean officer: {}", facultyListId, deanOfficer.getEmpId());
            }

            // Create department lists for each department secretary
            Map<String, DepartmentList> departmentListMap = new HashMap<>();
            List<DepartmentSecretary> departmentSecretaries = secretaryRepository.findAll();
            for (DepartmentSecretary secretary : departmentSecretaries) {
                // Find the corresponding FacultyList using the dean officer of the secretary
                DeanOfficer deanOfficer = secretary.getDeanOfficer();
                if (deanOfficer != null) {
                    FacultyList facultyList = facultyListMap.get(deanOfficer.getEmpId());
                    if (facultyList != null) {
                        String departmentName = getDepartmentName(secretary.getEmpId());
                        String deptListId = "DL_" + secretary.getEmpId();
                        DepartmentList departmentList = createDepartmentList(deptListId, departmentName, secretary,
                                facultyList);
                        departmentListMap.put(secretary.getEmpId(), departmentList);
                        log.debug("Created DepartmentList: {} for secretary: {}", deptListId, secretary.getEmpId());
                    }
                }
            }

            // Create advisor lists for each advisor
            List<Advisor> advisors = advisorRepository.findAll();
            for (Advisor advisor : advisors) {
                // Find the corresponding DepartmentList using the department secretary of the
                // advisor
                DepartmentSecretary secretary = advisor.getDepartmentSecretary();
                if (secretary != null) {
                    DepartmentList departmentList = departmentListMap.get(secretary.getEmpId());
                    if (departmentList != null) {
                        String advisorListId = "AL_" + advisor.getEmpId();
                        AdvisorList advisorList = createAdvisorList(advisorListId, advisor, departmentList);
                        log.debug("Created AdvisorList: {} for advisor: {}", advisorListId, advisor.getEmpId());
                    }
                }
            }

            // Example: Create two graduations for different terms for testing
            // Assuming studentAffairs1 and studentAffairs2 are fetched or created elsewhere
            // For demonstration, let's use the first two available student affairs officers
            // if they exist
            List<StudentAffairs> studentAffairsList = studentAffairsRepository.findAll();
            StudentAffairs studentAffairs1 = null;
            StudentAffairs studentAffairs2 = null;
            if (studentAffairsList.size() > 0)
                studentAffairs1 = studentAffairsList.get(0);
            if (studentAffairsList.size() > 1)
                studentAffairs2 = studentAffairsList.get(1);

            // Ensure studentAffairs1 and studentAffairs2 are not null before using them
            if (studentAffairs1 != null && studentAffairs2 != null) {
                Graduation graduation1 = createGraduation("GRAD_TEST_SPRING", new Timestamp(System.currentTimeMillis()),
                        "Spring 2024", "TestType", studentAffairs1);
                Graduation graduation2 = createGraduation("GRAD_TEST_FALL", new Timestamp(System.currentTimeMillis()),
                        "Fall 2024", "TestType", studentAffairs2);
                graduationRepository.saveAll(List.of(graduation1, graduation2));
            } else if (studentAffairs1 != null) {
                // Fallback if only one student affairs officer exists
                Graduation graduation1 = createGraduation("GRAD_TEST_SPRING", new Timestamp(System.currentTimeMillis()),
                        "Spring 2024", "TestType", studentAffairs1);
                graduationRepository.save(graduation1);
                log.warn("Only one StudentAffairs officer found, created one test graduation.");
            } else {
                log.warn("No StudentAffairs officers found to create example test graduations.");
            }

            log.info("Graduation hierarchy initialized successfully");

        } catch (Exception e) {
            log.error("Error initializing graduation hierarchy: {}", e.getMessage());
            throw new RuntimeException("Failed to initialize graduation hierarchy", e);
        }
    }

    private String getFacultyName(String deanOfficerId) {
        switch (deanOfficerId) {
            case "DO101":
                return "Faculty of Engineering";
            case "DO102":
                return "Faculty of Science";
            case "DO103":
                return "Faculty of Arts";
            default:
                return "Faculty of " + deanOfficerId;
        }
    }

    private String getDepartmentName(String secretaryId) {
        // Map secretary IDs to department names
        switch (secretaryId) {
            case "DS101":
                return "Computer Engineering";
            case "DS102":
                return "Software Engineering";
            case "DS103":
                return "Electrical Engineering";
            case "DS104":
                return "Mechanical Engineering";
            case "DS105":
                return "Civil Engineering";
            case "DS106":
                return "Industrial Engineering";
            case "DS107":
                return "Environmental Engineering";
            case "DS108":
                return "Biomedical Engineering";
            case "DS109":
                return "Aerospace Engineering";
            case "DS110":
                return "Chemical Engineering";
            case "DS111":
                return "Mathematics";
            case "DS112":
                return "Physics";
            case "DS113":
                return "Chemistry";
            case "DS114":
                return "Biology";
            case "DS115":
                return "Statistics";
            case "DS116":
                return "Philosophy";
            case "DS117":
                return "History";
            case "DS118":
                return "Literature";
            default:
                return "Department of " + secretaryId;
        }
    }

    private String getDeanOfficerForSecretary(String secretaryId) {
        // Based on our existing relationship mapping
        Map<String, String> relationships = getDeanOfficerRelationships();
        return relationships.get(secretaryId);
    }

    private String getDepartmentSecretaryForAdvisor(String advisorId) {
        // Based on our existing relationship mapping
        Map<String, String> relationships = getDepartmentSecretaryRelationships();
        return relationships.get(advisorId);
    }
}
