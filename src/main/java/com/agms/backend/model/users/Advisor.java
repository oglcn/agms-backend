package com.agms.backend.model.users;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.agms.backend.model.AdvisorList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Data
@EqualsAndHashCode(callSuper = true, exclude = {"advisorList", "students", "departmentSecretary"})
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "advisors")
@DiscriminatorValue("ADVISOR")
public class Advisor extends User {

    @Column(name = "emp_id", nullable = false, unique = true)
    private String empId;

    @Column(name = "department")
    private String department;

    @OneToOne(mappedBy = "advisor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private AdvisorList advisorList;

    @OneToMany(mappedBy = "advisor")
    private List<Student> students;

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "department_secretary_id")
    private DepartmentSecretary departmentSecretary;

    @Override
    public Role getRole() {
        return Role.ADVISOR;
    }
}
