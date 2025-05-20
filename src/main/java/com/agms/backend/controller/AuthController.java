package com.agms.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agms.backend.dto.AuthenticationRequest;
import com.agms.backend.dto.AuthenticationResponse;
import com.agms.backend.dto.NavigateToResetPasswordRequest;
import com.agms.backend.dto.RegisterRequest;
import com.agms.backend.dto.ResetPasswordRequest;
import com.agms.backend.exception.EmailAlreadyExistsException;
import com.agms.backend.exception.ForbiddenException;
import com.agms.backend.exception.ResourceNotFoundException;
import com.agms.backend.service.AuthenticationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
// GetMapping import removed

@RestController
@RequestMapping("/api/auth") // Changed from /api/v1/auth to /api/auth to match UserController
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication and authorization")
public class AuthController {

    private final AuthenticationService authenticationService; // Renamed from 'service' for clarity

    @Operation(summary = "Register new user", description = "Registers a new user in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
        @ApiResponse(responseCode = "409", description = "Email already exists"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Parameter(description = "Registration details", required = true)
            @RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authenticationService.register(request));
        } catch (EmailAlreadyExistsException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(AuthenticationResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "User login", description = "Authenticates a user and returns a JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Parameter(description = "Login credentials", required = true)
            @RequestBody AuthenticationRequest request) {
        try {
            return ResponseEntity.ok(authenticationService.authenticate(request));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(AuthenticationResponse.builder()
                            .message(e.getMessage())
                            .build());
        } catch (ForbiddenException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(AuthenticationResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Initiate password reset", description = "Sends a password reset link to the user's email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset link sent successfully",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/navigate-to-reset-password")
    public ResponseEntity<AuthenticationResponse> navigateToResetPassword(
            @Parameter(description = "Email for password reset", required = true)
            @RequestBody NavigateToResetPasswordRequest request) {
        try {
            return ResponseEntity.ok(authenticationService.navigateToResetPassword(request));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(AuthenticationResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Reset password", description = "Resets the user's password using the reset token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password reset successful",
            content = @Content(schema = @Schema(implementation = AuthenticationResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Invalid or expired reset token")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<AuthenticationResponse> resetPassword(
            @Parameter(description = "Password reset details", required = true)
            @RequestBody ResetPasswordRequest request) {
        try {
            return ResponseEntity.ok(authenticationService.resetPassword(request));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(AuthenticationResponse.builder()
                            .message(e.getMessage())
                            .build());
        } catch (ForbiddenException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(AuthenticationResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }

}