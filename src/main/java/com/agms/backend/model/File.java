package com.agms.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.agms.backend.model.users.User;
import java.sql.Timestamp;

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

    @Lob
    private byte[] data;

    @Column(nullable = false)
    private Timestamp uploadDate;

    @ManyToOne
    @JoinColumn(name = "uploaderId")
    private User uploader;

    @Column(nullable = false)
    private String filePath;

    @ManyToOne
    @JoinColumn(name = "submissionId")
    private Submission submission;
}