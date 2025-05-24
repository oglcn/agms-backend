// path: backend/src/main/java/com/agms/backend/service/AuthenticationService.java
package com.agms.backend.service;

import com.agms.backend.model.users.Role;
import com.agms.backend.model.users.User;
import com.agms.backend.dto.UserProfileResponse;

public interface UserService {
    // User management
    User createUser(String firstName, String lastName, String email, String password, Role role);
    User findByEmail(String email);
    User findById(String id);
    boolean existsByEmail(String email);
    
    // Profile operations
    UserProfileResponse getUserProfile(String email);
}