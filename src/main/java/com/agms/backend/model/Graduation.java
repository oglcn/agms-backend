package com.agms.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.agms.backend.model.users.StudentAffairs;
import java.sql.Timestamp;
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
    private Timestamp requestDate;

    @Column(nullable = false)
    private String term;

    @Column(nullable = false)
    private String type;

    @ManyToOne
    @JoinColumn(name = "studentAffairsId")
    private StudentAffairs studentAffairs;

    @OneToMany(mappedBy = "graduation")
    private List<GraduationList> graduationLists;
}