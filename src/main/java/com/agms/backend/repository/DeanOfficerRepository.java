package com.agms.backend.repository;

import com.agms.backend.model.users.DeanOfficer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeanOfficerRepository extends JpaRepository<DeanOfficer, Long> {
    Optional<DeanOfficer> findByEmpId(String empId);
    Optional<DeanOfficer> findByEmail(String email);
    Optional<DeanOfficer> findByFaculty(String faculty);
    List<DeanOfficer> findByStudentAffairsEmpId(String studentAffairsEmpId);
}