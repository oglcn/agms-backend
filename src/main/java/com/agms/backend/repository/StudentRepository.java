package com.agms.backend.repository;

import com.agms.backend.model.users.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    Optional<Student> findByStudentNumber(String studentNumber);    
    boolean existsByStudentNumber(String studentNumber);

    Optional<Student> findByEmail(String email);
}