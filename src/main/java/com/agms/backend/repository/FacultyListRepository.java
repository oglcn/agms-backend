package com.agms.backend.repository;

import com.agms.backend.model.FacultyList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FacultyListRepository extends JpaRepository<FacultyList, String> {
} 