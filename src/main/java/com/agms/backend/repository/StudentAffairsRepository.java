package com.agms.backend.repository;

import com.agms.backend.entity.StudentAffairs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentAffairsRepository extends JpaRepository<StudentAffairs, String> {
    Optional<StudentAffairs> findByEmpId(String empId);
}