package com.agms.backend.model;

public enum SubmissionStatus {
    // Initial state
    PENDING,                    // Submitted by student, waiting for advisor review
    
    // Advisor level
    APPROVED_BY_ADVISOR,        // Approved by advisor, forwarded to department secretary
    REJECTED_BY_ADVISOR,        // Rejected by advisor, workflow ends
    
    // Department Secretary level
    APPROVED_BY_DEPT,           // Approved by dept secretary, forwarded to dean
    REJECTED_BY_DEPT,           // Rejected by dept secretary, workflow ends
    
    // Dean Officer level  
    APPROVED_BY_DEAN,           // Approved by dean, forwarded to student affairs
    REJECTED_BY_DEAN,           // Rejected by dean, workflow ends
    
    // Student Affairs level (final)
    FINAL_APPROVED,             // Final approval by student affairs
    FINAL_REJECTED              // Final rejection by student affairs
}
