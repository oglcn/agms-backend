package com.agms.backend.entity;

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
@Table(name = "AdvisorList")
public class AdvisorList {
    @Id
    private String advisorListId;

    @Column(nullable = false)
    private LocalDate creationDate;

    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @JoinColumn(name = "advisorId", nullable = false)
    private Advisor advisor;

    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @JoinColumn(name = "deptListId", nullable = false)
    private DepartmentList departmentList;

    @OneToMany(mappedBy = "advisorList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Student> students;
} 