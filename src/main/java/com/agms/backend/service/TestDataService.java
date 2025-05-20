package com.agms.backend.service;

/**
 * Service interface for managing test data operations.
 * Follows Interface Segregation Principle by having focused methods.
 */
public interface TestDataService {
    /**
     * Initializes basic test data if the system is empty
     */
    void initializeTestData();
    
    /**
     * Creates a test student with given details
     * @param firstName First name of the student
     * @param lastName Last name of the student
     * @param email Email of the student
     * @param password Password for the student
     * @param studentId Student ID
     * @return Created student's ID
     */
    String createTestStudent(String firstName, String lastName, String email, String password, String studentId);
} 