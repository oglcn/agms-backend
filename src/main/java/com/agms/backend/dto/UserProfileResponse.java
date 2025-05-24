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
    /**
     * For students: studentNumber
     * For employees: employeeId
     */
    private String instituteNumber;
}
