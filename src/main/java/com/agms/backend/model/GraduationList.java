package com.agms.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "GraduationList")
public class GraduationList {
    @Id
    private String listId;

    @Column(nullable = false)
    private LocalDate creationDate;

    @ManyToOne
    @JoinColumn(name = "graduationId")
    private Graduation graduation;

    @OneToMany(mappedBy = "graduationList")
    private List<FacultyList> facultyLists;
} 