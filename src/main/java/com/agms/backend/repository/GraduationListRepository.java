package com.agms.backend.repository;

import com.agms.backend.model.GraduationList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GraduationListRepository extends JpaRepository<GraduationList, String> {
    List<GraduationList> findByGraduationGraduationId(String graduationId);
    
    // Finalization related methods
    @Modifying
    @Query("UPDATE GraduationList gl SET gl.isFinalized = :isFinalized WHERE gl.listId = :listId")
    int updateFinalizationStatus(@Param("listId") String listId, @Param("isFinalized") Boolean isFinalized);
    
    List<GraduationList> findByGraduationGraduationIdAndIsFinalized(String graduationId, Boolean isFinalized);
    
    boolean existsByGraduationGraduationIdAndIsFinalized(String graduationId, Boolean isFinalized);
} 