package com.agms.backend.repository;

import com.agms.backend.model.GraduationList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GraduationListRepository extends JpaRepository<GraduationList, String> {
} 