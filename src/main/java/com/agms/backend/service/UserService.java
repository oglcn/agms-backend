// path: backend/src/main/java/com/agms/backend/service/AuthenticationService.java
package com.agms.backend.service;

import com.agms.backend.entity.Role;
import com.agms.backend.entity.Student;
import com.agms.backend.entity.User;
import com.agms.backend.repository.StudentRepository;
import com.agms.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.agms.backend.dto.UserProfileResponse;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public UserProfileResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        UserProfileResponse.UserProfileResponseBuilder profileBuilder = UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstname(user.getFirstName())
                .lastname(user.getLastName())
                .role(user.getRole());

        // Check if the user is a student and add student-specific details
        if (user.getRole() == Role.ROLE_STUDENT || user.getRole() == Role.ROLE_USER) { // Or however you identify
                                                                                       // students
            Optional<Student> studentOpt = studentRepository.findByUser(user);
            if (studentOpt.isPresent()) {
                Student student = studentOpt.get();
                profileBuilder.studentId(student.getStudentId());
                profileBuilder.graduationRequestStatus(student.getGraduationRequestStatus());
            }
        }
        return profileBuilder.build();
    }
}