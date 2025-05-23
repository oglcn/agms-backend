package com.agms.backend.repository;

import com.agms.backend.entity.DeanOfficer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeanOfficerRepository extends JpaRepository<DeanOfficer, String> {
    Optional<DeanOfficer> findByEmpId(String empId);
}