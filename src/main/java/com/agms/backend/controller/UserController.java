package com.agms.backend.controller;

import com.agms.backend.dto.UserProfileResponse;
import com.agms.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
