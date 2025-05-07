package com.agms.backend.dto;

import com.agms.backend.entity.GraduationRequestStatus;
import com.agms.backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long userId; // Changed from Integer to Long
    private String email;
    private String firstname;
    private String lastname;
    private Role role;
    private String studentId; // Nullable, only if the user is a student
    private GraduationRequestStatus graduationRequestStatus; // Nullable
}
