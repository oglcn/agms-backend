package com.agms.backend.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.agms.backend.dto.UserProfileResponse;
import com.agms.backend.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing user profiles")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;


    @Operation(summary = "Get current user profile", description = "Retrieves the profile of the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).build();
        }
        String currentPrincipalName = authentication.getName();
        return ResponseEntity.ok(userService.getUserProfile(currentPrincipalName));
    }

    // Other CRUD endpoints for User will be added here later
    // For example:
    // @GetMapping("/{id}")
    // public ResponseEntity<User> getUserById(@PathVariable Integer id) { ... }
    //
    // @PostMapping
    // public ResponseEntity<User> createUser(@RequestBody UserCreateRequest
    // request) { ... }
    //
    // @PutMapping("/{id}")
    // public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody
    // UserUpdateRequest request) { ... }
    //
    // @DeleteMapping("/{id}")
    // public ResponseEntity<Void> deleteUser(@PathVariable Integer id) { ... }
}
