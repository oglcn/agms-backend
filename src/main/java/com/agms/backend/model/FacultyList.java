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
@Table(name = "FacultyList")
public class FacultyList {
    @Id
    private String faculty_list_id;

    @Column
    private LocalDate finish_date;

    @Column
    private String status;

    @OneToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @JoinColumn(name = "dean_officer_id", nullable = false)
    private DeanOfficer dean_officer;

    @OneToMany(mappedBy = "facultyList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DepartmentList> department_lists;
}