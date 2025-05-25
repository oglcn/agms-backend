package com.agms.backend.repository;

import com.agms.backend.model.users.DepartmentSecretary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentSecretaryRepository extends JpaRepository<DepartmentSecretary, Long> {
    Optional<DepartmentSecretary> findByEmpId(String empId);
    Optional<DepartmentSecretary> findByEmail(String email);
    Optional<DepartmentSecretary> findByDepartment(String department);
    List<DepartmentSecretary> findByDeanOfficerEmpId(String deanOfficerEmpId);
    List<DepartmentSecretary> findByDeanOfficerFaculty(String faculty);
}