package com.agms.backend.service.impl;

import com.agms.backend.dto.CreateStudentRequest;
import com.agms.backend.model.TestStudent;
import com.agms.backend.repository.UserRepository;
import com.agms.backend.service.StudentService;
import com.agms.backend.service.TestDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestDataServiceImpl implements TestDataService {
    private final StudentService studentService;
    private final UserRepository userRepository;

    private final List<TestStudent> DEFAULT_TEST_STUDENTS = Arrays.asList(
        TestStudent.computerScienceStudent(),
        TestStudent.engineeringStudent(),
        TestStudent.withDetails("Alice", "Johnson", "BIO")
    );

    @Override
    @Transactional
    public void initializeTestData() {
        if (userRepository.count() == 0) {
            DEFAULT_TEST_STUDENTS.forEach(this::createTestStudentFromModel);
            System.out.println("Test data initialized successfully!");
        }
    }

    @Override
    @Transactional
    public String createTestStudent(String firstName, String lastName, String email, String password, String studentId) {
        return createTestStudentFromModel(TestStudent.builder()
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .password(password)
            .studentId(studentId)
            .build());
    }

    private String createTestStudentFromModel(TestStudent student) {
        CreateStudentRequest request = CreateStudentRequest.builder()
            .firstName(student.getFirstName())
            .lastName(student.getLastName())
            .email(student.getEmail())
            .password(student.getPassword())
            .studentId(student.getStudentId())
            .build();

        return studentService.createStudent(request).getStudentId();
    }
} 