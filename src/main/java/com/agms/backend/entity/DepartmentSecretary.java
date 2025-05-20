package com.agms.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "DepartmentSecretary")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentSecretary {
    @Id
    private String empId;

    @OneToOne
    @JoinColumn(name = "userId")
    private User user;

    @OneToMany(mappedBy = "secretary")
    private List<DepartmentList> departmentLists;
} 