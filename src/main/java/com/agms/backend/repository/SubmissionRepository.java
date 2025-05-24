package com.agms.backend.repository;

import com.agms.backend.model.Submission;
import com.agms.backend.model.SubmissionStatus;
import com.agms.backend.model.users.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, String> {
    
    /**
     * Find all submissions by student
     */
    List<Submission> findByStudent(Student student);
    
    /**
     * Find all submissions by student number
     */
    @Query("SELECT s FROM Submission s WHERE s.student.studentNumber = :studentNumber")
    List<Submission> findByStudentNumber(@Param("studentNumber") String studentNumber);
    
    /**
     * Find all submissions by advisor list id
     */
    @Query("SELECT s FROM Submission s WHERE s.advisorList.advisorListId = :advisorListId")
    List<Submission> findByAdvisorListId(@Param("advisorListId") String advisorListId);
    
    /**
     * Find all submissions by status
     */
    List<Submission> findByStatus(SubmissionStatus status);
    
    /**
     * Find submissions by student and status
     */
    @Query("SELECT s FROM Submission s WHERE s.student.studentNumber = :studentNumber AND s.status = :status")
    List<Submission> findByStudentNumberAndStatus(@Param("studentNumber") String studentNumber, @Param("status") SubmissionStatus status);
    
    /**
     * Find the latest submission for a student (most recent by submission date)
     */
    @Query("SELECT s FROM Submission s WHERE s.student.studentNumber = :studentNumber ORDER BY s.submissionDate DESC")
    List<Submission> findByStudentNumberOrderBySubmissionDateDesc(@Param("studentNumber") String studentNumber);
    
    /**
     * Check if student has any pending submissions
     */
    @Query("SELECT COUNT(s) > 0 FROM Submission s WHERE s.student.studentNumber = :studentNumber AND s.status = 'PENDING'")
    boolean hasActivePendingSubmission(@Param("studentNumber") String studentNumber);
} 