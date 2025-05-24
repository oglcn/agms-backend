package com.agms.backend.model.users;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.agms.backend.model.FacultyList;
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
    private String empId;

    @OneToMany(mappedBy = "deanOfficer")
    private List<FacultyList> facultyLists;

    @OneToMany(mappedBy = "deanOfficer")
    private List<DepartmentSecretary> departmentSecretaries;

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "student_affairs_id")
    private StudentAffairs studentAffairs;

    @Override
    public Role getRole() {
        return Role.DEAN_OFFICER;
    }
}