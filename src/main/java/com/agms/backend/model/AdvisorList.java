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
@Table(name = "AdvisorList")
public class AdvisorList {
    @Id
    private String advisor_list_id;

    @Column
    private LocalDate finish_date;

    @Column
    private String status;

    @OneToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    @JoinColumn(name = "advisor_id", nullable = false)
    private Advisor advisor;

    @OneToMany(mappedBy = "advisorList", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Submission> submissions;
}