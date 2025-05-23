package com.agms.backend.repository;

import com.agms.backend.entity.Advisor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdvisorRepository extends JpaRepository<Advisor, String> {
    Optional<Advisor> findByEmpId(String empId);
    boolean existsByEmpId(String empId);
} 