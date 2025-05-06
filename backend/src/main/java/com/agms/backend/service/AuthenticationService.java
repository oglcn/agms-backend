// path: backend/src/main/java/com/agms/backend/service/AuthenticationService.java
package com.agms.backend.service;

import com.agms.backend.dto.AuthenticationRequest;
import com.agms.backend.dto.AuthenticationResponse;
import com.agms.backend.dto.RegisterRequest;
import com.agms.backend.entity.GraduationRequestStatus;
import com.agms.backend.entity.Role;
import com.agms.backend.entity.Student;
import com.agms.backend.entity.User;
import com.agms.backend.repository.StudentRepository;
import com.agms.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StudentRepository studentRepository;

    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
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
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
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
}