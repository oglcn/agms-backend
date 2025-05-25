package com.agms.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;
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
    private Timestamp creationDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isFinalized = false;

    @ManyToOne
    @JoinColumn(name = "graduationId")
    private Graduation graduation;

    @OneToMany(mappedBy = "graduationList")
    private List<FacultyList> facultyLists;
}