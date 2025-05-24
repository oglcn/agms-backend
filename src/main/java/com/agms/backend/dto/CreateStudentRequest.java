package com.agms.backend.dto;

import com.agms.backend.model.users.GraduationRequestStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStudentRequest {
    // User information
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
    
        // Student specific information    
        @NotBlank(message = "Student number is required")    
        @Pattern(regexp = "^\\d{8}$", message = "Student number must be 8 digits")
    private String studentNumber;
    
    private GraduationRequestStatus graduationRequestStatus;
} 