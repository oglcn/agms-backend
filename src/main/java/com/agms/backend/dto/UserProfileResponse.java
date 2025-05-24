package com.agms.backend.dto;

import com.agms.backend.model.users.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String userId;
    private String email;
    private String firstname;
    private String lastname;
    private Role role;
    private String studentNumber; // Nullable, only if the user is a student    // Note: Graduation status is now handled through submissions, not directly on student
}
