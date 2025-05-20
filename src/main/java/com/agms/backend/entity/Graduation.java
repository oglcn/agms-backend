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
@Table(name = "Graduation")
public class Graduation {
    @Id
    private String graduationId;

    @Column(nullable = false)
    private LocalDate requestDate;

    @Column(nullable = false)
    private String term;

    @ManyToOne
    @JoinColumn(name = "studentAffairsId")
    private StudentAffairs studentAffairs;

    @OneToMany(mappedBy = "graduation")
    private List<GraduationList> graduationLists;
} 