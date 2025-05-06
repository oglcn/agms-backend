package com.agms.backend.controller;

import com.agms.backend.dto.AuthenticationRequest;
import com.agms.backend.dto.AuthenticationResponse;
import com.agms.backend.dto.RegisterRequest;
import com.agms.backend.dto.ResetPasswordRequest;
import com.agms.backend.dto.NavigateToResetPasswordRequest;
// UserProfileResponse import removed as the endpoint is moved
import com.agms.backend.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// Imports for SecurityContextHolder and Authentication removed as they are no longer used here for /profile
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
// GetMapping import removed

@RestController
@RequestMapping("/api/auth") // Changed from /api/v1/auth to /api/auth to match UserController
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService; // Renamed from 'service' for clarity

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request) {
        try {
            return ResponseEntity.ok(authenticationService.authenticate(request));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(AuthenticationResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/navigate-to-reset-password")
    public ResponseEntity<AuthenticationResponse> navigateToResetPassword(
            @RequestBody NavigateToResetPasswordRequest request) {
        return ResponseEntity.ok(authenticationService.navigateToResetPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthenticationResponse> resetPassword(
            @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authenticationService.resetPassword(request));
    }

}