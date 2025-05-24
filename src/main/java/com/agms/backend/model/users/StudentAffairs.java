package com.agms.backend.model.users;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.agms.backend.model.Graduation;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "student_affairs")
@DiscriminatorValue("STUDENT_AFFAIRS")
public class StudentAffairs extends User {

    @Column(name = "emp_id", nullable = false, unique = true)
    private String emp_id;

    @OneToMany(mappedBy = "studentAffairs")
    private List<Graduation> graduations;

    @OneToMany(mappedBy = "studentAffairs")
    private List<DeanOfficer> dean_officers;

    @Override
    public Role getRole() {
        return Role.STUDENT_AFFAIRS;
    }
}