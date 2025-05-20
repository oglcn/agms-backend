package com.agms.backend.model;

import lombok.Builder;
import lombok.Value;

/**
 * Immutable value object representing test student data
 */
@Value
@Builder
public class TestStudent {
    String firstName;
    String lastName;
    String email;
    String password;
    String studentId;

    /**
     * Creates a Computer Science student with a specific pattern for student ID
     */
    public static TestStudent computerScienceStudent() {
        return TestStudent.builder()
            .firstName("John")
            .lastName("Doe")
            .email("cs.student@agms.com")
            .password("cs123")
            .studentId("CS230001")  // CS prefix for Computer Science
            .build();
    }

    /**
     * Creates an Engineering student with a specific pattern for student ID
     */
    public static TestStudent engineeringStudent() {
        return TestStudent.builder()
            .firstName("Jane")
            .lastName("Smith")
            .email("eng.student@agms.com")
            .password("eng123")
            .studentId("ENG23002")  // ENG prefix for Engineering
            .build();
    }

    /**
     * Creates a custom test student with provided details
     */
    public static TestStudent withDetails(String firstName, String lastName, String departmentPrefix) {
        String formattedId = String.format("%s%d", departmentPrefix, System.currentTimeMillis() % 10000);
        return TestStudent.builder()
            .firstName(firstName)
            .lastName(lastName)
            .email(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@agms.com")
            .password(departmentPrefix.toLowerCase() + "123")
            .studentId(formattedId)
            .build();
    }
} 