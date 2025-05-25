package com.agms.backend.dto;

import com.agms.backend.model.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {

    private String submissionId;
    private Timestamp submissionDate;
    private String content;
    private SubmissionStatus status;
    /**
     * Student number - submissions are only for students
     */
    private String studentNumber;
    private String studentName;
    private String advisorListId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        private String fileId;
        private String fileName;
        private String fileType;
        private Timestamp uploadDate;
        private String uploaderName;
    }

    private List<FileInfo> files;
}