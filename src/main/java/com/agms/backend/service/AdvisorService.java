package com.agms.backend.service;

import com.agms.backend.model.users.Advisor;
import com.agms.backend.model.AdvisorList;
import java.util.List;

public interface AdvisorService {
    // Advisor management
    Advisor createAdvisor(String empId, String userId);
    Advisor findByEmpId(String empId);
    
    // AdvisorList operations
    AdvisorList createAdvisorList(String advisorId, String departmentListId);
    List<AdvisorList> getAdvisorListsByDepartment(String departmentId);
    AdvisorList findAdvisorListById(String advisorListId);
} 