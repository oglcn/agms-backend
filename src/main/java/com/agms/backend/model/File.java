package com.agms.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "File")
public class File {
    @Id
    private Integer file_id;

    @Column(nullable = false)
    private String file_name;

    @Column(nullable = false)
    private String file_type;

    @Column(nullable = false)
    private LocalDate upload_date;

    @ManyToOne
    @JoinColumn(name = "uploader_id")
    private User uploader;

    @Column(nullable = false)
    private String file_path;

    @ManyToOne
    @JoinColumn(name = "submission_id")
    private Submission submission;
}