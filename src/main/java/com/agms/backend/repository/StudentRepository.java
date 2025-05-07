package com.agms.backend.repository;

import com.agms.backend.entity.Student;
import com.agms.backend.entity.User;
import com.agms.backend.entity.GraduationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    Optional<Student> findByUser(User user);

    Boolean existsByStudentId(String studentId);

    GraduationRequestStatus getGraduationRequestStatusByStudentId(String studentId);
}