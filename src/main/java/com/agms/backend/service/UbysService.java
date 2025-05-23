package com.agms.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.agms.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UbysService {
    private final ObjectMapper objectMapper;

    /**
     * Retrieves student data from the JSON file based on the student number
     * 
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
            // Read the JSON file from resources
            ClassPathResource resource = new ClassPathResource("data/students.json");
            Map<String, Object> students = objectMapper.readValue(resource.getInputStream(), Map.class);

            // Get student data by student number
            Map<String, Object> studentData = (Map<String, Object>) students.get(studentNumber);

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
            getStudentData(studentNumber);
            return true;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }
}