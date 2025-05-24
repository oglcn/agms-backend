package com.agms.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.agms.backend.model.users.DepartmentSecretary;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "DepartmentList")
public class DepartmentList {
    @Id
    private String dept_list_id;

    @Column
    private LocalDate finish_date;

    @Column
    private String status;

    @OneToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @JoinColumn(name = "secretary_id", nullable = false)
    private DepartmentSecretary secretary;

    @OneToMany(mappedBy = "departmentList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AdvisorList> advisor_lists;
}