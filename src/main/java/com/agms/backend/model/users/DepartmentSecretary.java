package com.agms.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.List;

@Entity
@Table(name = "department_secretaries")
@DiscriminatorValue("DEPARTMENT_SECRETARY")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentSecretary extends User {

    @Column(name = "emp_id", nullable = false, unique = true)
    private String empId;

    @OneToMany(mappedBy = "secretary")
    private List<DepartmentList> departmentLists;

    @OneToMany(mappedBy = "departmentSecretary")
    private List<Advisor> advisors;

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "dean_officer_id")
    private DeanOfficer deanOfficer;

    @Override
    public Role getRole() {
        return Role.DEPARTMENT_SECRETARY;
    }
}