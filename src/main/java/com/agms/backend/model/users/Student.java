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

    public void setCourses(List<Course> courses) {
        this.courses = courses;
        calculateAcademicMetrics();
    }

    /**
     * Calculates GPA and total credits from the student's courses and updates the
     * attributes.
     * Call this method after setting courses to update GPA and totalCredit.
     */
    public void calculateAcademicMetrics() {
        if (courses == null || courses.isEmpty()) {
            this.gpa = 0.0;
            this.totalCredit = 0;
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
    }
}