package com.agms.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.agms.backend.model.users.StudentAffairs;
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
    private String graduation_id;

    @Column(nullable = false)
    private LocalDate request_date;

    @Column(nullable = false)
    private String term;

    @ManyToOne
    @JoinColumn(name = "student_affairs_id")
    private StudentAffairs student_affairs;

    @OneToMany(mappedBy = "graduation")
    private List<GraduationList> graduation_lists;
}