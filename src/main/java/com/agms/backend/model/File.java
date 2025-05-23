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
    private Integer fileId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private LocalDate uploadDate;

    @ManyToOne
    @JoinColumn(name = "uploaderId")
    private User uploader;

    @Column(nullable = false)
    private String filePath;

    @ManyToOne
    @JoinColumn(name = "submissionId")
    private Submission submission;
} 