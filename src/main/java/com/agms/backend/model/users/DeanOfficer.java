package com.agms.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dean_officers")
@DiscriminatorValue("DEAN_OFFICER")
public class DeanOfficer extends User {

    @Column(name = "emp_id", nullable = false, unique = true)
    private String emp_id;

    @OneToMany(mappedBy = "deanOfficer")
    private List<FacultyList> faculty_lists;

    @OneToMany(mappedBy = "deanOfficer")
    private List<DepartmentSecretary> department_secretaries;

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "student_affairs_id")
    private StudentAffairs student_affairs;

    @Override
    public Role getRole() {
        return Role.DEAN_OFFICER;
    }
}