package com.agms.backend.entity;

import java.beans.Transient;
import java.util.List;

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
    private String student_number;

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "advisor_list_id")
    private AdvisorList advisor_list;

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "advisor_id")
    private Advisor advisor;

    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    @Transient
    private int gpa;

    @Transient
    private int totalCredit;

    @Transient
    private List<Course> courses;

    @Transient
    private int semester;
}