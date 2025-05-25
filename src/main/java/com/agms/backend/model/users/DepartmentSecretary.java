package com.agms.backend.model.users;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.agms.backend.model.DepartmentList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "department_secretaries")
@DiscriminatorValue("DEPARTMENT_SECRETARY")
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"departmentLists", "advisors", "deanOfficer"})
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentSecretary extends User {

    @Column(name = "emp_id", nullable = false, unique = true)
    private String empId;

    @Column(name = "department")
    private String department;

    @OneToMany(mappedBy = "secretary")
    @JsonManagedReference
    private List<DepartmentList> departmentLists;

    @OneToMany(mappedBy = "departmentSecretary")
    @JsonManagedReference
    private List<Advisor> advisors;

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "dean_officer_id")
    private DeanOfficer deanOfficer;

    @Override
    public Role getRole() {
        return Role.DEPARTMENT_SECRETARY;
    }
}