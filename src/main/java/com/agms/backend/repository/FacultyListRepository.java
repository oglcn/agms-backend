package com.agms.backend.repository;

import com.agms.backend.model.FacultyList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyListRepository extends JpaRepository<FacultyList, String> {
    List<FacultyList> findByGraduationListListId(String graduationListId);
    boolean existsByGraduationListListId(String graduationListId);
    
    // Finalization related methods
    Optional<FacultyList> findByDeanOfficerEmpId(String deanOfficerEmpId);
    
    @Modifying
    @Query("UPDATE FacultyList fl SET fl.isFinalized = :isFinalized WHERE fl.facultyListId = :facultyListId")
    int updateFinalizationStatus(@Param("facultyListId") String facultyListId, @Param("isFinalized") Boolean isFinalized);
    
    List<FacultyList> findByGraduationListListIdAndIsFinalized(String graduationListId, Boolean isFinalized);
    
    boolean existsByGraduationListListIdAndIsFinalized(String graduationListId, Boolean isFinalized);
} 