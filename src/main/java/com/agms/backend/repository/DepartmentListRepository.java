package com.agms.backend.repository;

import com.agms.backend.model.DepartmentList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentListRepository extends JpaRepository<DepartmentList, String> {
    List<DepartmentList> findByFacultyListFacultyListId(String facultyListId);
    boolean existsByFacultyListFacultyListId(String facultyListId);
    List<DepartmentList> findByDepartment(String department);
    
    // Finalization related methods
    Optional<DepartmentList> findBySecretaryEmpId(String secretaryEmpId);
    
    @Modifying
    @Query("UPDATE DepartmentList dl SET dl.isFinalized = :isFinalized WHERE dl.deptListId = :deptListId")
    int updateFinalizationStatus(@Param("deptListId") String deptListId, @Param("isFinalized") Boolean isFinalized);
    
    List<DepartmentList> findByFacultyListFacultyListIdAndIsFinalized(String facultyListId, Boolean isFinalized);
    
    boolean existsByFacultyListFacultyListIdAndIsFinalized(String facultyListId, Boolean isFinalized);
} 