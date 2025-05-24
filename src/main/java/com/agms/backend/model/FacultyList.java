package com.agms.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.agms.backend.model.users.DeanOfficer;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "FacultyList")
public class FacultyList {
    @Id
    private String facultyListId;

    @Column(nullable = false)
    private LocalDate creationDate;

    @Column(nullable = false)
    private String faculty;

    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @JoinColumn(name = "deanOfficerId", nullable = false)
    private DeanOfficer deanOfficer;

    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @JoinColumn(name = "tGraduationListId", nullable = false)
    private GraduationList graduationList;

    @OneToMany(mappedBy = "facultyList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DepartmentList> departmentLists;
} 