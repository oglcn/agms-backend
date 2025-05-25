package com.agms.backend.service.impl;

import com.agms.backend.model.DepartmentList;
import com.agms.backend.model.users.DepartmentSecretary;
import com.agms.backend.model.users.User;
import com.agms.backend.model.FacultyList;
import com.agms.backend.repository.DepartmentListRepository;
import com.agms.backend.repository.DepartmentSecretaryRepository;
import com.agms.backend.repository.UserRepository;
import com.agms.backend.repository.FacultyListRepository;
import com.agms.backend.service.DepartmentService;
import com.agms.backend.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {
        private final DepartmentListRepository departmentListRepository;
        private final DepartmentSecretaryRepository secretaryRepository;
        private final UserRepository userRepository;
        private final FacultyListRepository facultyListRepository;

        @Autowired
        public DepartmentServiceImpl(
                        DepartmentListRepository departmentListRepository,
                        DepartmentSecretaryRepository secretaryRepository,
                        UserRepository userRepository,
                        FacultyListRepository facultyListRepository) {
                this.departmentListRepository = departmentListRepository;
                this.secretaryRepository = secretaryRepository;
                this.userRepository = userRepository;
                this.facultyListRepository = facultyListRepository;
        }

        @Override
        @Transactional
        public DepartmentList createDepartmentList(String department, String secretaryId, String facultyListId) {
                DepartmentSecretary secretary = secretaryRepository.findByEmpId(secretaryId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Secretary not found with empId: " + secretaryId));

                FacultyList facultyList = facultyListRepository.findById(facultyListId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Faculty list not found with ID: " + facultyListId));

                DepartmentList departmentList = DepartmentList.builder()
                                .department(department)
                                .secretary(secretary)
                                .facultyList(facultyList)
                                .build();

                return departmentListRepository.save(departmentList);
        }

        @Override
        public List<DepartmentList> getDepartmentListsByFaculty(String facultyId) {
                return departmentListRepository.findByFacultyListFacultyListId(facultyId);
        }

        @Override
        public DepartmentList findDepartmentListById(String deptListId) {
                return departmentListRepository.findById(deptListId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Department list not found with ID: " + deptListId));
        }

        @Override
        @Transactional
        public DepartmentSecretary createSecretary(String empId, String userId) {
                // This method is now deprecated since DepartmentSecretary creation should go
                // through AuthenticationService
                // But if we need to support it, we need to find the existing user and cast it
                // to DepartmentSecretary
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

                if (!(user instanceof DepartmentSecretary)) {
                        throw new IllegalArgumentException("User with ID " + userId + " is not a DepartmentSecretary");
                }

                DepartmentSecretary secretary = (DepartmentSecretary) user;
                secretary.setEmpId(empId);

                return secretaryRepository.save(secretary);
        }

        @Override
        public DepartmentSecretary findSecretaryByEmpId(String empId) {
                return secretaryRepository.findByEmpId(empId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Secretary not found with employee ID: " + empId));
        }
}