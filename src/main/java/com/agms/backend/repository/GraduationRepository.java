package com.agms.backend.repository;

import com.agms.backend.model.Graduation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GraduationRepository extends JpaRepository<Graduation, String> {
} 