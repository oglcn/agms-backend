package com.agms.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.agms.backend.model.users.DepartmentSecretary;
import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "DepartmentList")
public class DepartmentList {
    @Id
    private String deptListId;

    @Column(nullable = false)
    private Timestamp creationDate;

    @Column(nullable = false)
    private String department;

    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @JoinColumn(name = "secretaryId", nullable = false)
    private DepartmentSecretary secretary;

    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @JoinColumn(name = "facultyListId", nullable = false)
    private FacultyList facultyList;

    @OneToMany(mappedBy = "departmentList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AdvisorList> advisorLists;
}