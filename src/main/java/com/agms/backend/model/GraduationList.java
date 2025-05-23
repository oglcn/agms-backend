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
@Table(name = "GraduationList")
public class GraduationList {
    @Id
    private String list_id;

    @Column(nullable = false)
    private LocalDate creation_date;

    @ManyToOne
    @JoinColumn(name = "graduation_id")
    private Graduation graduation;

    @OneToMany(mappedBy = "graduationList")
    private List<FacultyList> faculty_lists;
}