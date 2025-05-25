package com.agms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {
    private String fileId;
    private String fileName;
    private String fileType;
    private Timestamp uploadDate;
    private String uploaderName;
    private String downloadUrl;
} 