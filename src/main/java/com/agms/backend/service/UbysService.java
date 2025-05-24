package com.agms.backend.service;

import com.agms.backend.model.users.Student;
import com.agms.backend.model.users.User;
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
        Student student = Student.builder()
                .studentNumber(studentNumber) // This is now the primary key
                .build();
        
        // Set user information from nested user object
        Map<String, Object> userData = (Map<String, Object>) studentData.get("user");
        if (userData != null) {
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
}