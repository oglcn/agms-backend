# Current Object-Oriented Design and Workflow Implementation

## Overview
This document describes the current object-oriented design implementation for the AGMS (Academic Graduation Management System) backend. The system has been refactored to follow proper OOP principles with department/faculty-based relationships instead of ID-based references.

## Data Structure (UBYS JSON)

### Current UBYS JSON Structure
```json
{
  "studentAffairs": {
    "SA001": {
      "empId": "SA001",
      "user": {
        "id": "U001",
        "firstName": "Student",
        "lastName": "Affairs",
        "email": "studentaffairs@test.com",
        "role": "STUDENT_AFFAIRS"
      }
    }
  },
  "deanOfficers": {
    "DEAN001": {
      "empId": "DEAN001",
      "faculty": "Engineering",
      "user": {
        "id": "U002",
        "firstName": "Dean",
        "lastName": "Officer",
        "email": "dean@test.com",
        "role": "DEAN_OFFICER"
      }
    }
  },
  "departmentSecretaries": {
    "DS001": {
      "empId": "DS001",
      "department": "Computer Engineering",
      "user": {
        "id": "U003",
        "firstName": "Department",
        "lastName": "Secretary",
        "email": "deptsec@test.com",
        "role": "DEPARTMENT_SECRETARY"
      }
    }
  },
  "advisors": {
    "ADV001": {
      "empId": "ADV001",
      "department": "Computer Engineering",
      "user": {
        "id": "U004",
        "firstName": "Test",
        "lastName": "Advisor",
        "email": "advisor@test.com",
        "role": "ADVISOR"
      }
    },
    "ADV002": {
      "empId": "ADV002",
      "department": "Computer Engineering",
      "user": {
        "id": "U007",
        "firstName": "Second",
        "lastName": "Advisor",
        "email": "advisor2@test.com",
        "role": "ADVISOR"
      }
    }
  },
  "students": {
    "123456789": {
      "studentNumber": "123456789",
      "department": "Computer Engineering",
      "advisorId": "ADV001",
      "user": {
        "id": "U005",
        "firstName": "Test",
        "lastName": "Student",
        "email": "student@test.com",
        "role": "STUDENT"
      },
      "semester": 8,
      "courses": [...]
    },
    "987654321": {
      "studentNumber": "987654321",
      "department": "Computer Engineering",
      "advisorId": "ADV002",
      "user": {
        "id": "U006",
        "firstName": "Ineligible",
        "lastName": "Student",
        "email": "ineligible@test.com",
        "role": "STUDENT"
      },
      "semester": 4,
      "courses": [...]
    },
    "111222333": {
      "studentNumber": "111222333",
      "department": "Computer Engineering",
      "advisorId": "ADV001",
      "user": {
        "id": "U008",
        "firstName": "Third",
        "lastName": "Student",
        "email": "student3@test.com",
        "role": "STUDENT"
      },
      "semester": 6,
      "courses": [...]
    }
  }
}
```

## Object-Oriented Design Principles

### 1. Hierarchical Relationships
```
Student Affairs (SA001)
└── Dean Officer (DEAN001) - Faculty: "Engineering"
    └── Department Secretary (DS001) - Department: "Computer Engineering"
        ├── Advisor (ADV001) - Department: "Computer Engineering"
        └── Advisor (ADV002) - Department: "Computer Engineering"
            ├── Student (123456789) - Department: "Computer Engineering", advisorId: "ADV001"
            ├── Student (987654321) - Department: "Computer Engineering", advisorId: "ADV002"
            └── Student (111222333) - Department: "Computer Engineering", advisorId: "ADV001"
```

### 2. Key OOP Principles Implemented
- **One Department Secretary per Department**: Each department has exactly one secretary
- **Multiple Advisors per Department**: Each department secretary can have multiple advisors
- **Specific Advisor Assignment**: Each student is assigned to a specific advisor within their department
- **Department/Faculty-based Matching**: Relationships based on department/faculty names, not IDs

## Data Initialization Logic

### Initialization Order
1. **Student Affairs** → No dependencies
2. **Dean Officers** → Linked to Student Affairs
3. **Department Secretaries** → Linked to Dean Officers by faculty mapping
4. **Advisors** → Linked to Department Secretaries by department matching
5. **Students** → Linked to specific Advisors by advisorId

### Key Initialization Methods

#### Department Secretary Initialization
```java
// Auto-match dean officer based on department -> faculty mapping
String facultyName = getFacultyForDepartment(department);
if (facultyName != null) {
    DeanOfficer deanOfficer = deanOfficerRepository.findByFaculty(facultyName).orElse(null);
    if (deanOfficer != null) {
        departmentSecretary.setDeanOfficer(deanOfficer);
    }
}
```

#### Advisor Initialization
```java
// Auto-match department secretary based on advisor's department
DepartmentSecretary departmentSecretary = secretaryRepository.findByDepartment(department).orElse(null);
if (departmentSecretary != null) {
    advisor.setDepartmentSecretary(departmentSecretary);
}
```

#### Student Initialization
```java
// Get the advisorId from the original UBYS data
Map<String, Object> studentData = (Map<String, Object>) studentsSection.get(student.getStudentNumber());
String advisorId = studentData != null ? (String) studentData.get("advisorId") : null;

if (advisorId != null) {
    // Find the specific advisor by empId
    Advisor assignedAdvisor = advisorRepository.findByEmpId(advisorId).orElse(null);
    if (assignedAdvisor != null) {
        // Verify that the advisor's department matches the student's department
        if (assignedAdvisor.getDepartment().equals(student.getDepartment())) {
            student.setAdvisor(assignedAdvisor);
        }
    }
}
```

### Faculty-Department Mapping
```java
private String getFacultyForDepartment(String department) {
    switch (department) {
        // Engineering departments
        case "Computer Engineering":
        case "Electronics and Communication Engineering":
        case "Civil Engineering":
        case "Mechanical Engineering":
        // ... other engineering departments
            return "Engineering";
        
        // Science departments
        case "Physics":
        case "Chemistry":
        case "Mathematics":
        // ... other science departments
            return "Science";
        
        // Architecture and Design departments
        case "Industrial Design":
        case "Architecture":
        case "City and Regional Planning":
            return "Architecture and Design";
        
        default:
            return null;
    }
}
```

## Graduation Hierarchy Initialization

### Hierarchy Structure
```
Graduation (GRAD_2024_Spring)
└── GraduationList (GL_GRAD_2024_Spring)
    └── FacultyList (FL_DEAN001) - Faculty: "Engineering"
        └── DepartmentList (DL_DS001) - Department: "Computer Engineering"
            ├── AdvisorList (AL_ADV001) - Advisor: ADV001
            └── AdvisorList (AL_ADV002) - Advisor: ADV002
```

### Hierarchy Initialization Logic
```java
// Create faculty lists for each dean officer
List<DeanOfficer> deanOfficers = deanOfficerRepository.findAll();
for (DeanOfficer deanOfficer : deanOfficers) {
    FacultyList facultyList = createFacultyList(facultyListId, deanOfficer.getFaculty(), 
        deanOfficer, graduationList);

    // Create department lists for each department secretary under this specific dean officer
    List<DepartmentSecretary> departmentSecretaries = secretaryRepository.findByDeanOfficerEmpId(deanOfficer.getEmpId());
    for (DepartmentSecretary secretary : departmentSecretaries) {
        DepartmentList departmentList = createDepartmentList(deptListId, secretary.getDepartment(),
            secretary, facultyList);

        // Create advisor lists for each advisor under this specific department secretary
        List<Advisor> advisors = advisorRepository.findByDepartmentSecretaryEmpId(secretary.getEmpId());
        for (Advisor advisor : advisors) {
            AdvisorList advisorList = createAdvisorList(advisorListId, advisor, departmentList);
        }
    }
}
```

## Repository Methods Used

### Department/Faculty-based Queries
- `deanOfficerRepository.findByFaculty(facultyName)`
- `secretaryRepository.findByDepartment(department)`
- `secretaryRepository.findByDeanOfficerEmpId(deanOfficer.getEmpId())`
- `advisorRepository.findByDepartmentSecretaryEmpId(secretary.getEmpId())`
- `advisorRepository.findByEmpId(advisorId)` - for specific advisor assignment

### New Repository Method Added
```java
// In DepartmentSecretaryRepository
List<DepartmentSecretary> findByDeanOfficerFaculty(String faculty);
```

## Current Test Results

### ✅ Successful Data Initialization
```
Found 1 student affairs in ubys.json to initialize
Found 1 dean officers in ubys.json to initialize  
Found 1 department secretaries in ubys.json to initialize
Found 2 advisors in ubys.json to initialize
Found 3 students in ubys.json to initialize
```

### ✅ Proper Relationship Matching
```
Auto-matched DepartmentSecretary DS001 (dept: Computer Engineering) with DeanOfficer DEAN001 (faculty: Engineering)
Auto-matched Advisor ADV001 with DepartmentSecretary DS001 (dept: Computer Engineering)
Auto-matched Advisor ADV002 with DepartmentSecretary DS001 (dept: Computer Engineering)
Assigned student 123456789 (dept: Computer Engineering) to advisor ADV001 (dept: Computer Engineering)
```

### ✅ Correct Hierarchy Creation
```
Created FacultyList: FL_DEAN001 for faculty: Engineering
Created DepartmentList: DL_DS001 for department: Computer Engineering
Created AdvisorList: AL_ADV001 for advisor: ADV001
Created AdvisorList: AL_ADV002 for advisor: ADV002
```

## Test Failures to Fix

The following test failures need to be addressed (workflow logic issues, not OOP design):

1. **testAdvisorCannotFinalizeWithPendingSubmissions** - Status expected:<409> but was:<200>
2. **testAdvisorFinalizesAfterProcessing** - Response content expected:<true> but was:<false>
3. **testDepartmentSecretaryProcessesAndFinalizes** - Status expected:<200> but was:<409>
4. **testDeanOfficerProcessesAndFinalizes** - Status expected:<200> but was:<409>
5. **testStudentAffairsCompletesGraduation** - Response content expected:<true> but was:<false>
6. **testWorkflowConstraintsAreMaintained** - Advisor list should be finalized expected:<true> but was:<false>

## Key Files Modified

1. **src/test/resources/data/ubys.json** - Updated with OOP structure
2. **src/main/java/com/agms/backend/service/UbysService.java** - Simplified student creation logic
3. **src/main/java/com/agms/backend/config/DataInitializer.java** - Updated initialization logic
4. **src/main/java/com/agms/backend/repository/DepartmentSecretaryRepository.java** - Added faculty-based query

## Next Steps

The object-oriented design is working correctly. The test failures are related to workflow constraint validation and finalization logic that needs to be adjusted to work with the new OOP structure. Focus on:

1. Advisor finalization constraints
2. Department Secretary prerequisite checking
3. Dean Officer prerequisite validation
4. Student Affairs completion logic
5. Overall workflow constraint maintenance

The core OOP data initialization and relationships are solid and working as expected. 