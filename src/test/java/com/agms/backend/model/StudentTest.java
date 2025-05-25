package com.agms.backend.model;

import com.agms.backend.model.users.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StudentTest {

    private Student student;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .studentNumber("S001")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .department("Computer Engineering")
                .build();
    }

    @Test
    void testIsEligibleForGraduation_AllCriteriaMet() {
        // Create courses that meet all graduation requirements
        List<Course> courses = Arrays.asList(
                new Course("ENG101", "English 1", "AA", 6),
                new Course("ENG102", "English 2", "BA", 6),
                new Course("MATH101", "Mathematics 1", "BB", 6),
                new Course("CS101", "Computer Science 1", "AA", 5)
        );

        student.setCourses(courses);

        // Verify all conditions
        assertTrue(student.getGpa() >= 2.0, "GPA should be >= 2.0");
        assertTrue(student.getTotalCredit() >= 23, "Total credit should be >= 23");
        assertTrue(student.isCurriculumCompleted(), "Curriculum should be completed");
        assertTrue(student.isEligibleForGraduation(), "Student should be eligible for graduation");
    }

    @Test
    void testIsEligibleForGraduation_LowGPA() {
        // Create courses with low GPA (below 2.0)
        List<Course> courses = Arrays.asList(
                new Course("ENG101", "English 1", "DD", 6),
                new Course("ENG102", "English 2", "DD", 6),
                new Course("MATH101", "Mathematics 1", "DD", 6),
                new Course("CS101", "Computer Science 1", "DD", 5)
        );

        student.setCourses(courses);

        assertTrue(student.getTotalCredit() >= 23, "Total credit should be >= 23");
        assertTrue(student.isCurriculumCompleted(), "Curriculum should be completed");
        assertFalse(student.getGpa() >= 2.0, "GPA should be < 2.0");
        assertFalse(student.isEligibleForGraduation(), "Student should NOT be eligible due to low GPA");
    }

    @Test
    void testIsEligibleForGraduation_InsufficientCredits() {
        // Create courses with insufficient credits (less than 23)
        List<Course> courses = Arrays.asList(
                new Course("ENG101", "English 1", "AA", 5),
                new Course("ENG102", "English 2", "BA", 5),
                new Course("MATH101", "Mathematics 1", "BB", 5),
                new Course("CS101", "Computer Science 1", "AA", 5)
        );

        student.setCourses(courses);

        assertTrue(student.getGpa() >= 2.0, "GPA should be >= 2.0");
        assertTrue(student.isCurriculumCompleted(), "Curriculum should be completed");
        assertFalse(student.getTotalCredit() >= 23, "Total credit should be < 23");
        assertFalse(student.isEligibleForGraduation(), "Student should NOT be eligible due to insufficient credits");
    }

    @Test
    void testIsEligibleForGraduation_CurriculumNotCompleted() {
        // Create courses without required courses (missing ENG101)
        List<Course> courses = Arrays.asList(
                new Course("CS101", "Computer Science 1", "AA", 6),
                new Course("ENG102", "English 2", "BA", 6),
                new Course("MATH101", "Mathematics 1", "BB", 6),
                new Course("CS102", "Computer Science 2", "AA", 5)
        );

        student.setCourses(courses);

        assertTrue(student.getGpa() >= 2.0, "GPA should be >= 2.0");
        assertTrue(student.getTotalCredit() >= 23, "Total credit should be >= 23");
        assertFalse(student.isCurriculumCompleted(), "Curriculum should NOT be completed");
        assertFalse(student.isEligibleForGraduation(), "Student should NOT be eligible due to incomplete curriculum");
    }

    @Test
    void testIsEligibleForGraduation_NoCourses() {
        // Test with no courses
        student.setCourses(null);

        assertFalse(student.getGpa() >= 2.0, "GPA should be 0");
        assertFalse(student.getTotalCredit() >= 23, "Total credit should be 0");
        assertFalse(student.isCurriculumCompleted(), "Curriculum should NOT be completed");
        assertFalse(student.isEligibleForGraduation(), "Student should NOT be eligible with no courses");
    }

    @Test
    void testIsEligibleForGraduation_ExactBoundaryValues() {
        // Test exact boundary values: GPA = 2.0, Credits = 23
        List<Course> courses = Arrays.asList(
                new Course("ENG101", "English 1", "CC", 6),
                new Course("ENG102", "English 2", "CC", 6),
                new Course("MATH101", "Mathematics 1", "CC", 6),
                new Course("CS101", "Computer Science 1", "CC", 5)
        );

        student.setCourses(courses);

        assertEquals(2.0, student.getGpa(), 0.01, "GPA should be exactly 2.0");
        assertEquals(23, student.getTotalCredit(), "Total credit should be exactly 23");
        assertTrue(student.isCurriculumCompleted(), "Curriculum should be completed");
        assertTrue(student.isEligibleForGraduation(), "Student should be eligible with boundary values");
    }
} 