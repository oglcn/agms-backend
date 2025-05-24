package com.agms.backend.service;

import com.agms.backend.model.users.*;
import com.agms.backend.model.Course;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.agms.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class UbysService {
    private final ObjectMapper objectMapper;

    public UbysService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Retrieves all data from ubys.json
     */
    private Map<String, Object> getAllData() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/ubys.json");
        return objectMapper.readValue(
            resource.getInputStream(),
            new TypeReference<Map<String, Object>>() {}
        );
    }

    /**
     * Retrieves student data from the JSON file based on the student number
     * @param studentNumber The student number to look up
     * @return Map containing the student's data
     * @throws ResourceNotFoundException if the student is not found
     * @throws RuntimeException          if there's an error reading the data file
     */
    public Map<String, Object> getStudentData(String studentNumber) {
        if (studentNumber == null || studentNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Student number cannot be null or empty");
        }

        try {
            ClassPathResource resource = new ClassPathResource("data/ubys.json");
            Map<String, Map<String, Object>> allData = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<Map<String, Map<String, Object>>>() {}
            );

            Map<String, Object> studentsSection = allData.get("students");
            if (studentsSection == null) {
                throw new RuntimeException("Error reading student data: 'students' section not found in ubys.json");
            }

            Map<String, Object> studentData = (Map<String, Object>) studentsSection.get(studentNumber);

            if (studentData == null) {
                throw new ResourceNotFoundException("Student with number " + studentNumber + " not found");
            }

            return studentData;
        } catch (IOException e) {
            throw new RuntimeException("Error reading student data file", e);
        }
    }

    /**
     * Validates if a student exists in the system
     * 
     * @param studentNumber The student number to validate
     * @return true if the student exists, false otherwise
     */
    public boolean studentExists(String studentNumber) {
        try {
            return getStudentWithTransientAttributes(studentNumber) != null;
        } catch (ResourceNotFoundException e) {
            return false;
        } catch (RuntimeException e) {
            System.err.println("Error checking if student exists: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all student data from ubys.json and maps them to Student objects for database initialization.
     * This method creates Student objects WITHOUT populating transient attributes (gpa, totalCredit, courses, semester).
     * These are meant to be persisted to the database.
     * 
     * @return List of Student objects ready for database persistence
     * @throws RuntimeException if there's an error reading or parsing the data file
     */
    public List<Student> getAllStudentsForDbInitialization() {
        try {
            ClassPathResource resource = new ClassPathResource("data/ubys.json");
            Map<String, Object> allData = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> studentsSection = (Map<String, Object>) allData.get("students");
            if (studentsSection == null) {
                throw new RuntimeException("Error reading student data: 'students' section not found or empty in ubys.json");
            }

            List<Student> students = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : studentsSection.entrySet()) {
                String studentNumber = entry.getKey();
                Map<String, Object> studentData = (Map<String, Object>) entry.getValue();
                
                Student student = createStudentForDatabase(studentNumber, studentData);
                students.add(student);
            }
            
            return students;
        } catch (IOException e) {
            throw new RuntimeException("Error reading or parsing student data from ubys.json for DB initialization", e);
        }
    }

    /**
     * Retrieves a specific student's data from ubys.json and maps it to a Student object,
     * INCLUDING transient attributes (gpa, totalCredit, courses, semester) calculated from the JSON data.
     * This method is for retrieving complete student information including academic performance.
     * 
     * @param studentNumber The student number to look up
     * @return Student object with all transient attributes populated
     * @throws ResourceNotFoundException if the student is not found
     * @throws RuntimeException          if there's an error reading or parsing the data file
     */
    public Student getStudentWithTransientAttributes(String studentNumber) {
        if (studentNumber == null || studentNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Student number cannot be null or empty");
        }
        
        try {
            ClassPathResource resource = new ClassPathResource("data/ubys.json");
            Map<String, Object> allData = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> studentsSection = (Map<String, Object>) allData.get("students");
            if (studentsSection == null) {
                throw new ResourceNotFoundException("Student data not found in ubys.json");
            }

            Map<String, Object> studentData = (Map<String, Object>) studentsSection.get(studentNumber);
            if (studentData == null) {
                throw new ResourceNotFoundException("Student with number " + studentNumber + " not found");
            }
            
            return createStudentWithTransientAttributes(studentNumber, studentData);
        } catch (IOException e) {
            throw new RuntimeException("Error reading or parsing student data from ubys.json for student " + studentNumber, e);
        }
    }

    /**
     * Creates a Student object for database persistence (without transient attributes)
     */
    private Student createStudentForDatabase(String studentNumber, Map<String, Object> studentData) {
        // Set user information from nested user object
        Map<String, Object> userData = (Map<String, Object>) studentData.get("user");
        
        Student student = Student.builder()
                .studentNumber(studentNumber)
                .build();
        
        if (userData != null) {
            // ID will be auto-generated, so we don't need to set it manually
            student.setFirstName((String) userData.get("firstName"));
            student.setLastName((String) userData.get("lastName"));
            student.setEmail((String) userData.get("email"));
        }
        
        // Note: advisor relationship would be set separately during database initialization
        // student.setAdvisorId((String) studentData.get("advisorId"));
        
        return student;
    }

    /**
     * Creates a Student object with all transient attributes calculated
     */
    private Student createStudentWithTransientAttributes(String studentNumber, Map<String, Object> studentData) {
        // Start with the base student for database
        Student student = createStudentForDatabase(studentNumber, studentData);
        
        // Set semester
        Object semesterObj = studentData.get("semester");
        if (semesterObj != null) {
            student.setSemester(((Number) semesterObj).intValue());
        }
        
        // Process courses and calculate GPA and total credits
        List<Object> coursesData = (List<Object>) studentData.get("courses");
        if (coursesData != null) {
            List<Course> courses = new ArrayList<>();
            
            for (Object courseObj : coursesData) {
                Map<String, Object> courseData = (Map<String, Object>) courseObj;
                
                Course course = new Course(
                    (String) courseData.get("code"),
                    (String) courseData.get("name"),
                    (String) courseData.get("grade"),
                    ((Number) courseData.get("credit")).intValue()
                );
                
                courses.add(course);
            }
            
            student.setCourses(courses);
        }
        
        return student;
    }

    // ========== STUDENT AFFAIRS METHODS ==========
    
    /**
     * Retrieves all StudentAffairs entities from ubys.json for database initialization
     */
    public List<StudentAffairs> getAllStudentAffairsForDbInitialization() {
        try {
            Map<String, Object> allData = getAllData();
            Map<String, Object> studentAffairsSection = (Map<String, Object>) allData.get("studentAffairs");
            
            if (studentAffairsSection == null) {
                throw new RuntimeException("Error reading student affairs data: 'studentAffairs' section not found in ubys.json");
            }

            List<StudentAffairs> studentAffairsList = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : studentAffairsSection.entrySet()) {
                String empId = entry.getKey();
                Map<String, Object> studentAffairsData = (Map<String, Object>) entry.getValue();
                
                StudentAffairs studentAffairs = createStudentAffairsForDatabase(empId, studentAffairsData);
                studentAffairsList.add(studentAffairs);
            }
            
            return studentAffairsList;
        } catch (IOException e) {
            throw new RuntimeException("Error reading or parsing student affairs data from ubys.json for DB initialization", e);
        }
    }

    private StudentAffairs createStudentAffairsForDatabase(String empId, Map<String, Object> studentAffairsData) {
        Map<String, Object> userData = (Map<String, Object>) studentAffairsData.get("user");
        
        StudentAffairs studentAffairs = StudentAffairs.builder()
                .empId(empId)
                .build();
        
        if (userData != null) {
            studentAffairs.setFirstName((String) userData.get("firstName"));
            studentAffairs.setLastName((String) userData.get("lastName"));
            studentAffairs.setEmail((String) userData.get("email"));
        }
        
        return studentAffairs;
    }

    // ========== DEAN OFFICER METHODS ==========
    
    /**
     * Retrieves all DeanOfficer entities from ubys.json for database initialization
     */
    public List<DeanOfficer> getAllDeanOfficersForDbInitialization() {
        try {
            Map<String, Object> allData = getAllData();
            Map<String, Object> deanOfficersSection = (Map<String, Object>) allData.get("deanOfficers");
            
            if (deanOfficersSection == null) {
                throw new RuntimeException("Error reading dean officers data: 'deanOfficers' section not found in ubys.json");
            }

            List<DeanOfficer> deanOfficers = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : deanOfficersSection.entrySet()) {
                String empId = entry.getKey();
                Map<String, Object> deanOfficerData = (Map<String, Object>) entry.getValue();
                
                DeanOfficer deanOfficer = createDeanOfficerForDatabase(empId, deanOfficerData);
                deanOfficers.add(deanOfficer);
            }
            
            return deanOfficers;
        } catch (IOException e) {
            throw new RuntimeException("Error reading or parsing dean officers data from ubys.json for DB initialization", e);
        }
    }

    private DeanOfficer createDeanOfficerForDatabase(String empId, Map<String, Object> deanOfficerData) {
        Map<String, Object> userData = (Map<String, Object>) deanOfficerData.get("user");
        
        DeanOfficer deanOfficer = DeanOfficer.builder()
                .empId(empId)
                .build();
        
        if (userData != null) {
            deanOfficer.setFirstName((String) userData.get("firstName"));
            deanOfficer.setLastName((String) userData.get("lastName"));
            deanOfficer.setEmail((String) userData.get("email"));
        }
        
        return deanOfficer;
    }

    // ========== DEPARTMENT SECRETARY METHODS ==========
    
    /**
     * Retrieves all DepartmentSecretary entities from ubys.json for database initialization
     */
    public List<DepartmentSecretary> getAllDepartmentSecretariesForDbInitialization() {
        try {
            Map<String, Object> allData = getAllData();
            Map<String, Object> departmentSecretariesSection = (Map<String, Object>) allData.get("departmentSecretaries");
            
            if (departmentSecretariesSection == null) {
                throw new RuntimeException("Error reading department secretaries data: 'departmentSecretaries' section not found in ubys.json");
            }

            List<DepartmentSecretary> departmentSecretaries = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : departmentSecretariesSection.entrySet()) {
                String empId = entry.getKey();
                Map<String, Object> departmentSecretaryData = (Map<String, Object>) entry.getValue();
                
                DepartmentSecretary departmentSecretary = createDepartmentSecretaryForDatabase(empId, departmentSecretaryData);
                departmentSecretaries.add(departmentSecretary);
            }
            
            return departmentSecretaries;
        } catch (IOException e) {
            throw new RuntimeException("Error reading or parsing department secretaries data from ubys.json for DB initialization", e);
        }
    }

    private DepartmentSecretary createDepartmentSecretaryForDatabase(String empId, Map<String, Object> departmentSecretaryData) {
        Map<String, Object> userData = (Map<String, Object>) departmentSecretaryData.get("user");
        
        DepartmentSecretary departmentSecretary = DepartmentSecretary.builder()
                .empId(empId)
                .build();
        
        if (userData != null) {
            departmentSecretary.setFirstName((String) userData.get("firstName"));
            departmentSecretary.setLastName((String) userData.get("lastName"));
            departmentSecretary.setEmail((String) userData.get("email"));
        }
        
        return departmentSecretary;
    }

    // ========== ADVISOR METHODS ==========
    
    /**
     * Retrieves all Advisor entities from ubys.json for database initialization
     */
    public List<Advisor> getAllAdvisorsForDbInitialization() {
        try {
            Map<String, Object> allData = getAllData();
            Map<String, Object> advisorsSection = (Map<String, Object>) allData.get("advisors");
            
            if (advisorsSection == null) {
                throw new RuntimeException("Error reading advisors data: 'advisors' section not found in ubys.json");
            }

            List<Advisor> advisors = new ArrayList<>();
            
            for (Map.Entry<String, Object> entry : advisorsSection.entrySet()) {
                String empId = entry.getKey();
                Map<String, Object> advisorData = (Map<String, Object>) entry.getValue();
                
                Advisor advisor = createAdvisorForDatabase(empId, advisorData);
                advisors.add(advisor);
            }
            
            return advisors;
        } catch (IOException e) {
            throw new RuntimeException("Error reading or parsing advisors data from ubys.json for DB initialization", e);
        }
    }

    private Advisor createAdvisorForDatabase(String empId, Map<String, Object> advisorData) {
        Map<String, Object> userData = (Map<String, Object>) advisorData.get("user");
        
        Advisor advisor = Advisor.builder()
                .empId(empId)
                .build();
        
        if (userData != null) {
            advisor.setFirstName((String) userData.get("firstName"));
            advisor.setLastName((String) userData.get("lastName"));
            advisor.setEmail((String) userData.get("email"));
        }
        
        return advisor;
    }
}