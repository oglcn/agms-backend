package com.agms.backend.model.users;

import java.util.List;

import com.agms.backend.model.Course;
import com.agms.backend.model.AdvisorList;
import com.agms.backend.model.Submission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "students")
@DiscriminatorValue("STUDENT")
public class Student extends User {

    @Column(name = "student_number", nullable = false, unique = true)
    private String studentNumber;

    @Column(name = "department")
    private String department;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Submission> submissions;

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "advisor_id")
    private Advisor advisor;

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    @Transient
    private double gpa;

    @Transient
    private int totalCredit;

    @Transient
    private List<Course> courses;

    @Transient
    private int semester;

    @Transient
    private boolean isCurriculumCompleted;

    public void setCourses(List<Course> courses) {
        this.courses = courses;
        calculateAcademicMetrics();
    }

    /**
     * Calculates GPA, total credits, and curriculum completion status from the
     * student's courses.
     * Call this method after setting courses to update GPA, totalCredit, and
     * isCurriculumCompleted.
     */
    public void calculateAcademicMetrics() {
        if (courses == null || courses.isEmpty()) {
            this.gpa = 0.0;
            this.totalCredit = 0;
            this.isCurriculumCompleted = false;
            return;
        }

        double totalGpaPoints = 0.0;
        int totalCredits = 0;

        for (Course course : courses) {
            if (course != null && course.getCredit() > 0) {
                totalCredits += course.getCredit();
                totalGpaPoints += (course.getGpaPoints() * course.getCredit());
            }
        }

        this.totalCredit = totalCredits;
        this.gpa = totalCredits > 0 ? Math.round((totalGpaPoints / totalCredits) * 100.0) / 100.0 : 0.0;

        // Calculate curriculum completion
        // A curriculum is considered completed if:
        // 1. Exactly 4 courses
        // 2. Must have the 3 required courses: ENG101, ENG102, MATH101
        // 3. All courses must have passing grades (DD or better)
        this.isCurriculumCompleted = checkCurriculumCompletion();
    }

    /**
     * Checks if the curriculum is completed based on required courses and passing
     * grades.
     * 
     * @return true if curriculum is completed, false otherwise
     */
    private boolean checkCurriculumCompletion() {
        // Must have exactly 4 courses
        if (courses == null || courses.size() != 4) {
            return false;
        }

        // Required courses that must be present
        String[] requiredCourses = { "ENG101", "ENG102", "MATH101" };
        int requiredCoursesFound = 0;

        // Check if all required courses are present
        for (String requiredCourse : requiredCourses) {
            boolean found = courses.stream()
                    .anyMatch(course -> course != null && requiredCourse.equals(course.getCode()));
            if (found) {
                requiredCoursesFound++;
            }
        }

        // Must have all 3 required courses
        if (requiredCoursesFound != 3) {
            return false;
        }

        // Check if all courses have passing grades (DD or better)
        for (Course course : courses) {
            if (course == null || !isPassingGrade(course.getGrade())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if a grade is passing (DD or better).
     * 
     * @param grade the grade to check
     * @return true if the grade is passing, false otherwise
     */
    private boolean isPassingGrade(String grade) {
        if (grade == null) {
            return false;
        }

        // Passing grades: AA, BA, BB, CB, CC, DD
        // Failing grades: FD, FF
        switch (grade.toUpperCase()) {
            case "AA":
            case "BA":
            case "BB":
            case "CB":
            case "CC":
            case "DD":
                return true;
            case "FD":
            case "FF":
            default:
                return false;
        }
    }
}