package com.agms.backend.service;

import com.agms.backend.dto.AuthenticationRequest;
import com.agms.backend.dto.AuthenticationResponse;
import com.agms.backend.dto.RegisterRequest;
import com.agms.backend.entity.Role;
import com.agms.backend.entity.User;
import com.agms.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Integrate Mockito with JUnit 5
class AuthenticationServiceTest {

    @Mock // Create a mock instance of UserRepository
    private UserRepository userRepository;

    @InjectMocks // Create an instance of AuthenticationService and inject the mocks (@Mock) into it
    private AuthenticationService authenticationService;

    private RegisterRequest registerRequest;
    private AuthenticationRequest authenticationRequest;
    private User user;

    @BeforeEach // Method run before each test
    void setUp() {
        // Prepare common test data
        registerRequest = new RegisterRequest("testuser", "test@example.com", "password123");
        authenticationRequest = new AuthenticationRequest("testuser", "password123");
        user = User.builder()
                .id(1L) // Assign an ID for clarity, though not strictly necessary for these tests
                .username("testuser")
                .email("test@example.com")
                .password("password123") // Store plain text password for mock comparison
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    void register_Success() {
        // Arrange: Define mock behavior
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user); // Return the saved user mock

        // Act: Call the method under test
        AuthenticationResponse response = authenticationService.register(registerRequest);

        // Assert: Verify the results and interactions
        assertNotNull(response);
        assertEquals("User registered successfully", response.getMessage());

        // Verify that save was called on the repository exactly once
        verify(userRepository, times(1)).save(any(User.class));

        // Optional: Capture the user argument passed to save and assert its properties
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(registerRequest.getUsername(), savedUser.getUsername());
        assertEquals(registerRequest.getEmail(), savedUser.getEmail());
        // Note: In a real app, you'd assert the *hashed* password here
        assertEquals(registerRequest.getPassword(), savedUser.getPassword());
        assertEquals(Role.ROLE_USER, savedUser.getRole());
    }

     @Test
     void register_UsernameExists() {
         // Arrange
         when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

         // Act & Assert
         RuntimeException exception = assertThrows(RuntimeException.class, () -> {
             authenticationService.register(registerRequest);
         });
         assertEquals("Username already exists", exception.getMessage());
         // Verify save was never called
         verify(userRepository, never()).save(any(User.class));
     }

     @Test
     void register_EmailExists() {
         // Arrange
         when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
         when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

         // Act & Assert
         RuntimeException exception = assertThrows(RuntimeException.class, () -> {
             authenticationService.register(registerRequest);
         });
         assertEquals("Email already exists", exception.getMessage());
         verify(userRepository, never()).save(any(User.class));
     }

    @Test
    void authenticate_Success() {
        // Arrange
        when(userRepository.findByUsername(authenticationRequest.getUsername())).thenReturn(Optional.of(user));

        // Act
        AuthenticationResponse response = authenticationService.authenticate(authenticationRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Login successful", response.getMessage());
        verify(userRepository, times(1)).findByUsername(authenticationRequest.getUsername());
    }

    @Test
    void authenticate_UserNotFound() {
        // Arrange
        when(userRepository.findByUsername(authenticationRequest.getUsername())).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authenticationService.authenticate(authenticationRequest);
        });
        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(authenticationRequest.getUsername());
    }

    @Test
    void authenticate_InvalidPassword() {
        // Arrange
        // Create a user with the same username but different password for the mock
        User userWithWrongPassword = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("wrongpassword") // Different password
                .role(Role.ROLE_USER)
                .build();
        when(userRepository.findByUsername(authenticationRequest.getUsername())).thenReturn(Optional.of(userWithWrongPassword));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authenticationService.authenticate(authenticationRequest);
        });
        assertEquals("Invalid password", exception.getMessage());
        verify(userRepository, times(1)).findByUsername(authenticationRequest.getUsername());
    }
}