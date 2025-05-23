package com.agms.backend.repository;

import com.agms.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    Optional<Student> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);

    Optional<Student> findByEmail(String email);
}