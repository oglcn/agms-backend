package com.agms.backend.repository;

import com.agms.backend.entity.DepartmentSecretary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentSecretaryRepository extends JpaRepository<DepartmentSecretary, String> {
    Optional<DepartmentSecretary> findByEmpId(String empId);

    boolean existsByEmpId(String empId);
}