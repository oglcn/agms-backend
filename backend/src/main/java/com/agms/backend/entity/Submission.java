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
@Table(name = "Submission")
public class Submission {
    @Id
    private Integer submissionId;

    @Column(nullable = false)
    private LocalDate submissionDate;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    @ManyToOne
    @JoinColumn(name = "studentId")
    private Student student;

    @OneToMany(mappedBy = "submission")
    private List<File> files;
}