package com.agms.backend.repository;

import com.agms.backend.model.DepartmentList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentListRepository extends JpaRepository<DepartmentList, String> {
    List<DepartmentList> findByFacultyListFacultyListId(String facultyListId);
    boolean existsByFacultyListFacultyListId(String facultyListId);
    List<DepartmentList> findByDepartment(String department);
} 