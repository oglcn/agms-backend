// path: backend/src/main/java/com/agms/backend/service/AuthenticationService.java
package com.agms.backend.service;

import com.agms.backend.dto.AuthenticationRequest;
import com.agms.backend.dto.AuthenticationResponse;
import com.agms.backend.dto.RegisterRequest;
import com.agms.backend.dto.ResetPasswordRequest;
import com.agms.backend.entity.GraduationRequestStatus;
import com.agms.backend.entity.Role;
import com.agms.backend.entity.Student;
import com.agms.backend.entity.User;
import com.agms.backend.repository.StudentRepository;
import com.agms.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.agms.backend.dto.LogoutResponse;
import com.agms.backend.dto.NavigateToResetPasswordRequest;
import com.agms.backend.dto.UserProfileResponse;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.agms.backend.exception.EmailAlreadyExistsException;
import com.agms.backend.exception.ForbiddenException;
import com.agms.backend.exception.ResourceNotFoundException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StudentRepository studentRepository;

    @Autowired
    private EmailService emailService;

    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // Determine the role from either the Role object or the string representation
        Role userRole = Role.ROLE_USER;
        if (request.getRole() != null) {
            userRole = request.getRole();
        } else if (request.getRoleString() != null && !request.getRoleString().isEmpty()) {
            userRole = parseRole(request.getRoleString());
        }

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // In a real application, you should hash the
                                                                         // password
                .role(userRole)
                .build();
        userRepository.save(user);

        // If the role is ROLE_STUDENT, create a student record
        if (userRole == Role.ROLE_STUDENT) {
            // Determine the graduation request status
            GraduationRequestStatus status = GraduationRequestStatus.NOT_REQUESTED;
            if (request.getGraduationRequestStatus() != null) {
                status = request.getGraduationRequestStatus();
            } else if (request.getGraduationRequestStatusString() != null
                    && !request.getGraduationRequestStatusString().isEmpty()) {
                status = parseGraduationRequestStatus(request.getGraduationRequestStatusString());
            }

            createStudentRecord(user, request.getStudentId(), status);
        }

        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message("User registered successfully")
                .build();
    }

    /**
     * Create a student record for a user with ROLE_STUDENT
     * 
     * @param user                    The user for whom to create a student record
     * @param studentId               The student ID for the student record
     * @param graduationRequestStatus The graduation request status for the student
     *                                record
     */
    private void createStudentRecord(User user, String studentId, GraduationRequestStatus graduationRequestStatus) {
        // Create and save the student record
        Student student = Student.builder()
                .studentId(studentId)
                .user(user)
                .graduationRequestStatus(graduationRequestStatus)
                .build();
        studentRepository.save(student);
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ForbiddenException("Invalid password");
        }

        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message("Login successful")
                .build();
    }

    /**
     * Parse a string representation of a role into a Role enum value
     * 
     * @param roleStr String representation of the role (case insensitive)
     * @return The corresponding Role enum value, or Role.ROLE_USER if the role is
     *         invalid
     */
    public Role parseRole(String roleStr) {
        if (roleStr == null || roleStr.isEmpty()) {
            return Role.ROLE_USER;
        }

        try {
            // Format the role string properly for enum comparison
            String formattedRole = roleStr.toUpperCase();
            if (!formattedRole.startsWith("ROLE_")) {
                formattedRole = "ROLE_" + formattedRole;
            }

            // Try to find a matching enum value
            for (Role role : Role.values()) {
                if (role.name().equals(formattedRole)) {
                    return role;
                }
            }

            // No matching role found
            return Role.ROLE_USER;
        } catch (Exception e) {
            // If any error occurs, default to ROLE_USER
            return Role.ROLE_USER;
        }
    }

    /**
     * Parse a string representation of a graduation request status into a
     * GraduationRequestStatus enum value
     * 
     * @param statusStr String representation of the graduation request status (case
     *                  insensitive)
     * @return The corresponding GraduationRequestStatus enum value, or
     *         GraduationRequestStatus.NOT_REQUESTED if invalid
     */
    public GraduationRequestStatus parseGraduationRequestStatus(String statusStr) {
        if (statusStr == null || statusStr.isEmpty()) {
            return GraduationRequestStatus.NOT_REQUESTED;
        }

        try {
            // Convert to uppercase for enum comparison
            String formattedStatus = statusStr.toUpperCase();

            // Try to find a matching enum value
            for (GraduationRequestStatus status : GraduationRequestStatus.values()) {
                if (status.name().equals(formattedStatus)) {
                    return status;
                }
            }

            // No matching status found
            return GraduationRequestStatus.NOT_REQUESTED;
        } catch (Exception e) {
            // If any error occurs, default to NOT_REQUESTED
            return GraduationRequestStatus.NOT_REQUESTED;
        }
    }

    public AuthenticationResponse navigateToResetPassword(NavigateToResetPasswordRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }

        User user = userOptional.get();

        // Send email with reset password instructions
        String subject = "Reset Your Password - AGMS";
        String body = "Dear " + user.getFirstName() + ",\n\n" +
                "We received a request to reset your password for your AGMS account. " +
                "Please click on the link below to reset your password:\n\n" +
                "http://localhost:3000/auth/reset-password?token=" + jwtService.generateToken(user) + "\n\n" +
                "If you did not request a password reset, please ignore this email or contact support if you have concerns.\n\n"
                +
                "Best regards,\n" +
                "AGMS Team";

        emailService.sendEmail(user.getEmail(), subject, body);

        return AuthenticationResponse.builder()
                .message("Password reset instructions sent to your email")
                .build();
    }

    public AuthenticationResponse resetPassword(ResetPasswordRequest request) {
        // Extract username (email) from the JWT token
        String email;
        try {
            email = jwtService.extractEmail(request.getToken());
        } catch (Exception e) {
            throw new ForbiddenException("Invalid token");
        }

        // Find user by email
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }

        User user = userOptional.get();

        // Check if new password is the same as the old password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("New password must be different from the old password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return AuthenticationResponse.builder()
                .message("Password reset successfully")
                .build();
    }
}