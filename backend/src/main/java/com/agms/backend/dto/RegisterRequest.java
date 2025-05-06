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
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Role role;
    private String roleString;
    private String studentId;
    private GraduationRequestStatus graduationRequestStatus;
    private String graduationRequestStatusString;
}