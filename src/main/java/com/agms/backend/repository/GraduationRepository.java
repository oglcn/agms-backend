package com.agms.backend.repository;

import com.agms.backend.model.Graduation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GraduationRepository extends JpaRepository<Graduation, String> {
    
    /**
     * Find graduation by term
     */
    Optional<Graduation> findByTerm(String term);
    
    /**
     * Check if graduation exists for a specific term with IN_PROGRESS status
     */
    boolean existsByTermAndStatus(String term, String status);
} 