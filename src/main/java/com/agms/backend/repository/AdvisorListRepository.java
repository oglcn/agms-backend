package com.agms.backend.repository;

import com.agms.backend.model.AdvisorList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdvisorListRepository extends JpaRepository<AdvisorList, String> {
    List<AdvisorList> findByDepartmentListDeptListId(String departmentListId);
    boolean existsByDepartmentListDeptListId(String departmentListId);
    
    // Finalization related methods
    Optional<AdvisorList> findByAdvisorEmpId(String advisorEmpId);
    
    @Modifying
    @Query("UPDATE AdvisorList al SET al.isFinalized = :isFinalized WHERE al.advisorListId = :advisorListId")
    int updateFinalizationStatus(@Param("advisorListId") String advisorListId, @Param("isFinalized") Boolean isFinalized);
    
    List<AdvisorList> findByDepartmentListDeptListIdAndIsFinalized(String departmentListId, Boolean isFinalized);
    
    boolean existsByDepartmentListDeptListIdAndIsFinalized(String departmentListId, Boolean isFinalized);
} 