package com.agms.backend.dto;

import com.agms.backend.entity.GraduationRequestStatus;
import com.agms.backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NavigateToResetPasswordRequest {
    private String email;
}