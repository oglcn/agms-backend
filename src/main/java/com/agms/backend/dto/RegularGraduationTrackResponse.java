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
public class RegularGraduationTrackResponse {
    
    private boolean isStarted;
    private String term;
    private String status;
    private String graduationId;
    private Timestamp requestDate;
    private String studentAffairsEmpId;
} 