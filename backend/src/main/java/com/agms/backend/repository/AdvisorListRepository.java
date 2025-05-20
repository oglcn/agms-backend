package com.agms.backend.repository;

import com.agms.backend.entity.AdvisorList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvisorListRepository extends JpaRepository<AdvisorList, String> {
    List<AdvisorList> findByDepartmentListDeptListId(String departmentListId);
    boolean existsByDepartmentListDeptListId(String departmentListId);
} 