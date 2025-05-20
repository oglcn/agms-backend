package com.agms.backend.service;

import com.agms.backend.entity.DepartmentList;
import com.agms.backend.entity.DepartmentSecretary;
import java.util.List;

public interface DepartmentService {
    // Department management
    DepartmentList createDepartmentList(String department, String secretaryId, String facultyListId);
    List<DepartmentList> getDepartmentListsByFaculty(String facultyId);
    DepartmentList findDepartmentListById(String deptListId);
    
    // Secretary management
    DepartmentSecretary createSecretary(String empId, String userId);
    DepartmentSecretary findSecretaryByEmpId(String empId);
} 