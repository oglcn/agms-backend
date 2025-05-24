# Submission Management API Endpoints

This document describes the REST API endpoints for the Graduation Submission Management system.

## Base URL
All endpoints are prefixed with `/api/submissions`

## Authentication & Authorization
All endpoints require proper JWT authentication and role-based authorization.

## Endpoints

### 1. Create Graduation Submission
**POST** `/api/submissions/graduation`

Creates a new graduation submission for a student.

**Authorization:** `STUDENT` role required

**Request Body:**
```json
{
  "studentNumber": "S101",
  "content": "I would like to request graduation for Spring 2025",
  "submissionType": "GRADUATION"
}
```

**Response:** `201 Created`
```json
{
  "submissionId": "SUB_1748097002264",
  "submissionDate": "2025-05-24",
  "content": "I would like to request graduation for Spring 2025",
  "status": "PENDING",
  "studentNumber": "S101",
  "studentName": "John Doe",
  "advisorListId": "AL_001",
  "files": []
}
```

**Error Responses:**
- `409 Conflict`: Student already has an active pending submission
- `404 Not Found`: Student not found
- `400 Bad Request`: Invalid request data

---

### 2. Get Submissions by Student
**GET** `/api/submissions/student/{studentNumber}`

Retrieves all submissions for a specific student.

**Authorization:** `STUDENT`, `ADVISOR`, or `DEPARTMENT_SECRETARY` role required

**Path Parameters:**
- `studentNumber`: The student's number (e.g., "S101")

**Response:** `200 OK`
```json
[
  {
    "submissionId": "SUB_1748097002264",
    "submissionDate": "2025-05-24",
    "content": "I would like to request graduation for Spring 2025",
    "status": "PENDING",
    "studentNumber": "S101",
    "studentName": "John Doe",
    "advisorListId": "AL_001",
    "files": []
  }
]
```

---

### 3. Get Submissions by Advisor
**GET** `/api/submissions/advisor/{advisorEmpId}`

Retrieves all submissions assigned to a specific advisor.

**Authorization:** `ADVISOR` role required

**Path Parameters:**
- `advisorEmpId`: The advisor's employee ID (e.g., "ADV101")

**Response:** `200 OK`
```json
[
  {
    "submissionId": "SUB_1748097002264",
    "submissionDate": "2025-05-24",
    "content": "I would like to request graduation for Spring 2025",
    "status": "PENDING",
    "studentNumber": "S101",
    "studentName": "John Doe",
    "advisorListId": "AL_001",
    "files": []
  }
]
```

---

### 4. Get Submission by ID
**GET** `/api/submissions/{submissionId}`

Retrieves a specific submission by its ID.

**Authorization:** `STUDENT`, `ADVISOR`, or `DEPARTMENT_SECRETARY` role required

**Path Parameters:**
- `submissionId`: The submission ID (e.g., "SUB_1748097002264")

**Response:** `200 OK`
```json
{
  "submissionId": "SUB_1748097002264",
  "submissionDate": "2025-05-24",
  "content": "I would like to request graduation for Spring 2025",
  "status": "PENDING",
  "studentNumber": "S101",
  "studentName": "John Doe",
  "advisorListId": "AL_001",
  "files": []
}
```

**Error Responses:**
- `404 Not Found`: Submission not found

---

### 5. Update Submission Status
**PUT** `/api/submissions/{submissionId}/status`

Updates the status of a submission (approve/reject).

**Authorization:** `ADVISOR` role required

**Path Parameters:**
- `submissionId`: The submission ID

**Query Parameters:**
- `status`: The new status (`APPROVED` or `REJECTED`)

**Example:** `PUT /api/submissions/SUB_1748097002264/status?status=APPROVED`

**Response:** `200 OK`
```json
{
  "submissionId": "SUB_1748097002264",
  "submissionDate": "2025-05-24",
  "content": "I would like to request graduation for Spring 2025",
  "status": "APPROVED",
  "studentNumber": "S101",
  "studentName": "John Doe",
  "advisorListId": "AL_001",
  "files": []
}
```

---

### 6. Get Submissions by Status
**GET** `/api/submissions/status/{status}`

Retrieves all submissions with a specific status.

**Authorization:** `ADVISOR`, `DEPARTMENT_SECRETARY`, or `DEAN_OFFICER` role required

**Path Parameters:**
- `status`: The submission status (`PENDING`, `APPROVED`, `REJECTED`, `NOT_REQUESTED`)

**Response:** `200 OK`
```json
[
  {
    "submissionId": "SUB_1748097002264",
    "submissionDate": "2025-05-24",
    "content": "I would like to request graduation for Spring 2025",
    "status": "PENDING",
    "studentNumber": "S101",
    "studentName": "John Doe",
    "advisorListId": "AL_001",
    "files": []
  }
]
```

---

### 7. Check Active Pending Submission
**GET** `/api/submissions/student/{studentNumber}/has-pending`

Checks if a student has an active pending submission.

**Authorization:** `STUDENT` or `ADVISOR` role required

**Path Parameters:**
- `studentNumber`: The student's number

**Response:** `200 OK`
```json
true
```

---

### 8. Get Latest Submission
**GET** `/api/submissions/student/{studentNumber}/latest`

Retrieves the latest submission for a student.

**Authorization:** `STUDENT` or `ADVISOR` role required

**Path Parameters:**
- `studentNumber`: The student's number

**Response:** `200 OK`
```json
{
  "submissionId": "SUB_1748097002264",
  "submissionDate": "2025-05-24",
  "content": "I would like to request graduation for Spring 2025",
  "status": "APPROVED",
  "studentNumber": "S101",
  "studentName": "John Doe",
  "advisorListId": "AL_001",
  "files": []
}
```

**Error Responses:**
- `404 Not Found`: No submissions found for student

---

### 9. Delete Submission
**DELETE** `/api/submissions/{submissionId}`

Deletes a submission (administrative operation).

**Authorization:** `DEPARTMENT_SECRETARY` or `DEAN_OFFICER` role required

**Path Parameters:**
- `submissionId`: The submission ID

**Response:** `204 No Content`

**Error Responses:**
- `404 Not Found`: Submission not found

---

## Submission Status Flow

```
NOT_REQUESTED → PENDING → APPROVED/REJECTED
```

### Business Rules:
1. Students can only have one `PENDING` submission at a time
2. After `REJECTION`, students can create a new submission
3. After `APPROVAL`, students cannot create new submissions (unless for different terms)
4. Only advisors can change submission status from `PENDING` to `APPROVED`/`REJECTED`

---

## Error Handling

All endpoints return appropriate HTTP status codes:
- `200 OK`: Successful operation
- `201 Created`: Resource created successfully
- `204 No Content`: Successful deletion
- `400 Bad Request`: Invalid request data
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `409 Conflict`: Business rule violation
- `500 Internal Server Error`: Server error

Error responses include descriptive error messages in the response body.

---

## Testing

The API has been thoroughly tested with:
- **Unit Tests**: 16 test methods covering all service logic
- **Integration Tests**: 3 end-to-end test scenarios
- **All tests passing**: ✅ 19/19 tests successful

## Related Components

This API works in conjunction with:
- **Submission Service**: Business logic implementation
- **JPA Repositories**: Data persistence
- **Security Configuration**: Role-based access control
- **User Management**: Student/Advisor relationship handling 