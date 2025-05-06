// path: backend/src/main/java/com/agms/backend/config/SecurityConfig.java
package com.agms.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider; // No change here
import org.springframework.security.authentication.dao.DaoAuthenticationProvider; // No change here
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // No change here
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // No change here
import org.springframework.security.config.http.SessionCreationPolicy; // No change here
import org.springframework.security.core.userdetails.UserDetailsService; // Already injected
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // No change here
import org.springframework.security.crypto.password.PasswordEncoder; // No change here
import org.springframework.security.web.SecurityFilterChain; // No change here
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // No change here

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService; // Spring will inject the bean from ApplicationConfig

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll() // Public endpoints
                .anyRequest().authenticated() // All other requests need authentication
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // No sessions
            )
            .authenticationProvider(authenticationProvider()) // Set the custom provider
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // Add JWT filter

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService); // Wire UserDetailsService
        authProvider.setPasswordEncoder(passwordEncoder());     // Wire PasswordEncoder
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}