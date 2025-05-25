package com.agms.backend.service.impl;

import com.agms.backend.model.users.Advisor;
import com.agms.backend.model.AdvisorList;
import com.agms.backend.model.users.User;
import com.agms.backend.model.DepartmentList;
import com.agms.backend.repository.AdvisorRepository;
import com.agms.backend.repository.AdvisorListRepository;
import com.agms.backend.service.AdvisorService;
import com.agms.backend.service.UserService;
import com.agms.backend.service.DepartmentService;
import com.agms.backend.exception.ResourceNotFoundException;
import com.agms.backend.repository.DepartmentListRepository;
import com.agms.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
public class AdvisorServiceImpl implements AdvisorService {
    private final AdvisorRepository advisorRepository;
    private final AdvisorListRepository advisorListRepository;
    private final UserService userService;
    private final DepartmentService departmentService;

    @Autowired
    public AdvisorServiceImpl(
            AdvisorRepository advisorRepository,
            AdvisorListRepository advisorListRepository,
            UserService userService,
            DepartmentService departmentService) {
        this.advisorRepository = advisorRepository;
        this.advisorListRepository = advisorListRepository;
        this.userService = userService;
        this.departmentService = departmentService;
    }

    @Override
    @Transactional
    public Advisor createAdvisor(String empId, String userId) {
        // This method is now deprecated since Advisor creation should go through
        // AuthenticationService
        // But if we need to support it, we need to find the existing user and cast it
        // to Advisor
        User user = userService.findById(userId);

        if (!(user instanceof Advisor)) {
            throw new IllegalArgumentException("User with ID " + userId + " is not an Advisor");
        }

        Advisor advisor = (Advisor) user;
        advisor.setEmpId(empId);

        return advisorRepository.save(advisor);
    }

    @Override
    public Advisor findByEmpId(String empId) {
        return advisorRepository.findByEmpId(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Advisor not found with employee ID: " + empId));
    }

    @Override
    @Transactional
    public AdvisorList createAdvisorList(String advisorId, String departmentListId) {
        Advisor advisor = advisorRepository.findById(advisorId)
                .orElseThrow(() -> new ResourceNotFoundException("Advisor not found with ID: " + advisorId));

        DepartmentList departmentList = departmentService.findDepartmentListById(departmentListId);

        AdvisorList advisorList = AdvisorList.builder()
                .advisorListId("AL_" + advisor.getEmpId())
                .creationDate(new Timestamp(System.currentTimeMillis()))
                .advisor(advisor)
                .departmentList(departmentList)
                .build();

        return advisorListRepository.save(advisorList);
    }

    @Override
    public List<AdvisorList> getAdvisorListsByDepartment(String departmentId) {
        return advisorListRepository.findByDepartmentListDeptListId(departmentId);
    }

    @Override
    public AdvisorList findAdvisorListById(String advisorListId) {
        return advisorListRepository.findById(advisorListId)
                .orElseThrow(() -> new ResourceNotFoundException("AdvisorList not found with ID: " + advisorListId));
    }
}