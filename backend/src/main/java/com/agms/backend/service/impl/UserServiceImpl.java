package com.agms.backend.service.impl;

import com.agms.backend.dto.UserProfileResponse;
import com.agms.backend.entity.Role;
import com.agms.backend.entity.Student;
import com.agms.backend.entity.User;
import com.agms.backend.repository.StudentRepository;
import com.agms.backend.repository.UserRepository;
import com.agms.backend.service.UserService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User createUser(String firstName, String lastName, String email, String password, Role role) {
        if (existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .id("U" + System.currentTimeMillis())
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();

        return userRepository.save(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Override
    public User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserProfileResponse getUserProfile(String email) {
        User user = findByEmail(email);
        UserProfileResponse.UserProfileResponseBuilder profileBuilder = UserProfileResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .firstname(user.getFirstName())
                .lastname(user.getLastName())
                .role(user.getRole());

        // Check if the user is a student and add student-specific details
        if (user.getRole() == Role.STUDENT) {
            Optional<Student> studentOpt = studentRepository.findByUser(user);
            if (studentOpt.isPresent()) {
                Student student = studentOpt.get();
                profileBuilder.studentId(student.getStudentId().toString());
            }
        }
        return profileBuilder.build();
    }
} 