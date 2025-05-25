package com.agms.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.agms.backend.model.users.Student;
import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Submission")
public class Submission {
    @Id
    private String submissionId;

    @Column(nullable = false)
    private Timestamp submissionDate;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    @ManyToOne
    @JoinColumn(name = "studentNumber")
    private Student student;

    @ManyToOne(cascade = CascadeType.REFRESH)
    @JoinColumn(name = "advisor_list_id")
    private AdvisorList advisorList;

    @OneToMany(mappedBy = "submission")
    private List<File> files;

}
