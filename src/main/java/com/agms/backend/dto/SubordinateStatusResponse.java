package com.agms.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubordinateStatusResponse {
    private String empId;
    private String name;
    private String email;
    private String department;
    private String faculty;
    private Boolean isFinalized;
    private String listId;
    private String role;
} 