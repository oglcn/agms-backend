// path: backend/src/main/java/com/agms/backend/service/AuthenticationService.java
package com.agms.backend.service;

import com.agms.backend.dto.AuthenticationRequest;
import com.agms.backend.dto.AuthenticationResponse;
import com.agms.backend.dto.RegisterRequest;
import com.agms.backend.dto.ResetPasswordRequest;
import com.agms.backend.dto.NavigateToResetPasswordRequest;
import com.agms.backend.model.users.GraduationRequestStatus;
import com.agms.backend.model.users.Role;
import com.agms.backend.model.users.Student;
import com.agms.backend.model.users.Advisor;
import com.agms.backend.model.users.DeanOfficer;
import com.agms.backend.model.users.DepartmentSecretary;
import com.agms.backend.model.users.StudentAffairs;
import com.agms.backend.model.users.User;
import com.agms.backend.repository.StudentRepository;
import com.agms.backend.repository.AdvisorRepository;
import com.agms.backend.repository.DeanOfficerRepository;
import com.agms.backend.repository.DepartmentSecretaryRepository;
import com.agms.backend.repository.StudentAffairsRepository;
import com.agms.backend.repository.UserRepository;
import com.agms.backend.exception.EmailAlreadyExistsException;
import com.agms.backend.exception.ForbiddenException;
import com.agms.backend.exception.ResourceNotFoundException;
import com.agms.backend.exception.InvalidRoleException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StudentRepository studentRepository;
    private final AdvisorRepository advisorRepository;
    private final DeanOfficerRepository deanOfficerRepository;
    private final DepartmentSecretaryRepository departmentSecretaryRepository;
    private final StudentAffairsRepository studentAffairsRepository;

    @Autowired
    private EmailService emailService;

    public AuthenticationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // Get role from the request
        Role userRole;
        if (request.getRole() != null) {
            userRole = request.getRole();
        } else if (request.getRoleString() != null && !request.getRoleString().isEmpty()) {
            userRole = parseRole(request.getRoleString());
        } else {
            throw new InvalidRoleException("Role must be specified");
        }

        User user = createUserByRole(request, userRole);

        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder()
                .token(jwtToken)
                .message("User registered successfully")
                .build();
    }

    private User createUserByRole(RegisterRequest request, Role role) {
        String userId = UUID.randomUUID().toString();
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        switch (role) {
            case STUDENT:
                if (request.getStudentNumber() == null || request.getStudentNumber().isEmpty()) {
                    throw new InvalidRoleException("Student number is required for student registration");
                }

                // Determine the graduation request status
                GraduationRequestStatus status = GraduationRequestStatus.NOT_REQUESTED;
                if (request.getGraduationRequestStatus() != null) {
                    status = request.getGraduationRequestStatus();
                } else if (request.getGraduationRequestStatusString() != null
                        && !request.getGraduationRequestStatusString().isEmpty()) {
                    status = parseGraduationRequestStatus(request.getGraduationRequestStatusString());
                }

                Student student = Student.builder()
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(request.getEmail())
                        .password(encodedPassword)
                        .studentNumber(request.getStudentNumber()) // This is now the primary key
                        .build();
                return studentRepository.save(student);

            case ADVISOR:
                String advisorEmpId = generateEmpId();
                Advisor advisor = Advisor.builder()
                        .id(userId)
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(request.getEmail())
                        .password(encodedPassword)
                        .empId(advisorEmpId)
                        .build();
                return advisorRepository.save(advisor);

            case DEAN_OFFICER:
                String deanEmpId = generateEmpId();
                DeanOfficer deanOfficer = DeanOfficer.builder()
                        .id(userId)
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(request.getEmail())
                        .password(encodedPassword)
                        .empId(deanEmpId)
                        .build();
                return deanOfficerRepository.save(deanOfficer);

            case DEPARTMENT_SECRETARY:
                String secretaryEmpId = generateEmpId();
                DepartmentSecretary secretary = DepartmentSecretary.builder()
                        .id(userId)
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(request.getEmail())
                        .password(encodedPassword)
                        .empId(secretaryEmpId)
                        .build();
                return departmentSecretaryRepository.save(secretary);

            case STUDENT_AFFAIRS:
                String studentAffairsEmpId = generateEmpId();
                StudentAffairs studentAffairs = StudentAffairs.builder()
                        .id(userId)
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .email(request.getEmail())
                        .password(encodedPassword)
                        .empId(studentAffairsEmpId)
                        .build();
                return studentAffairsRepository.save(studentAffairs);

            default:
                throw new InvalidRoleException("Unsupported role: " + role);
        }
    }

    private String generateEmpId() {
        return "EMP" + System.currentTimeMillis();
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
     * @return The corresponding Role enum value
     * @throws InvalidRoleException if the role string is invalid
     */
    public Role parseRole(String roleStr) {
        if (roleStr == null || roleStr.isEmpty()) {
            throw new InvalidRoleException("Role cannot be empty");
        }

        try {
            return Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException("Invalid role: " + roleStr + ". Valid roles are: " +
                    String.join(", ", java.util.Arrays.stream(Role.values())
                            .map(Role::name)
                            .toArray(String[]::new)));
        }
    }

    /**
     * Parse a string representation of a graduation request status into a
     * GraduationRequestStatus enum value
     */
    public GraduationRequestStatus parseGraduationRequestStatus(String statusStr) {
        if (statusStr == null || statusStr.isEmpty()) {
            return GraduationRequestStatus.NOT_REQUESTED;
        }

        try {
            return GraduationRequestStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
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
                "http://localhost:3000/auth/reset-password?token=" + jwtService.generatePasswordResetToken(user)
                + "\n\n" +
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