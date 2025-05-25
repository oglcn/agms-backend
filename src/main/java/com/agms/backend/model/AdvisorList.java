package com.agms.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.agms.backend.model.users.Advisor;
import com.agms.backend.model.users.Student;
import java.sql.Timestamp;
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
    private Timestamp creationDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isFinalized = false;

    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @JoinColumn(name = "advisorId", nullable = false)
    private Advisor advisor;

    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @JoinColumn(name = "deptListId", nullable = false)
    private DepartmentList departmentList;

    @OneToMany(mappedBy = "advisorList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Submission> submissions;
}