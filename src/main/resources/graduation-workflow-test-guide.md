# Graduation Workflow Test Guide

This guide demonstrates the complete graduation workflow from start to finish, including the finalization constraints and how students check their graduation status.

## Prerequisites
- System must have test data with Student Affairs, Dean Officer, Department Secretary, Advisor, and Student
- All users must be properly linked in the hierarchy
- List structure must be set up (GraduationList → FacultyList → DepartmentList → AdvisorList)

## Workflow Steps

### Step 1: Student Affairs Starts Regular Graduation
**User**: Student Affairs  
**Endpoint**: `POST /api/submissions/regular-graduation/start`
```json
{
  "term": "2023-FALL"
}
```
**Expected Result**: Creates submissions for all eligible students with status `PENDING`

### Step 2: Advisor Reviews Submissions
**User**: Advisor  
**Actions**:
1. `GET /api/submissions/my-pending` - See pending submissions
2. `POST /api/submissions/finalize-my-list` - Should FAIL (409 Conflict) because submissions are still pending
3. `PUT /api/submissions/{submissionId}/approve` - Approve each submission (changes status to `APPROVED_BY_ADVISOR`)
4. `POST /api/submissions/finalize-my-list` - Should SUCCESS (200 OK) after all submissions processed
5. `GET /api/submissions/my-list/finalized` - Confirm finalization status is `true`

**Key Constraint**: Advisor cannot finalize unless all submissions in their list are processed (no PENDING status)

### Step 3: Department Secretary Reviews Submissions
**User**: Department Secretary  
**Actions**:
1. `GET /api/submissions/prerequisite-lists/finalized` - Check if advisor lists are finalized (should be `true`)
2. `GET /api/submissions/my-pending` - See submissions with `APPROVED_BY_ADVISOR` status
3. `PUT /api/submissions/{submissionId}/approve` - Approve each submission (changes status to `APPROVED_BY_DEPT`)
4. `POST /api/submissions/finalize-my-list` - Should SUCCESS after all submissions processed
5. `GET /api/submissions/my-list/finalized` - Confirm finalization

**Key Constraint**: Department Secretary cannot finalize unless:
- All advisor lists under their department are finalized
- All submissions awaiting their review are processed

### Step 4: Dean Officer Reviews Submissions
**User**: Dean Officer  
**Actions**:
1. `GET /api/submissions/prerequisite-lists/finalized` - Check if department lists are finalized
2. `GET /api/submissions/my-pending` - See submissions with `APPROVED_BY_DEPT` status
3. `PUT /api/submissions/{submissionId}/approve` - Approve each submission (changes status to `APPROVED_BY_DEAN`)
4. `POST /api/submissions/finalize-my-list` - Should SUCCESS after processing
5. `GET /api/submissions/my-list/finalized` - Confirm finalization

**Key Constraint**: Dean Officer cannot finalize unless:
- All department lists under their faculty are finalized
- All submissions awaiting their review are processed

### Step 5: Student Affairs Completes Graduation
**User**: Student Affairs  
**Actions**:
1. `GET /api/submissions/prerequisite-lists/finalized` - Check if faculty lists are finalized
2. `GET /api/submissions/my-pending` - See submissions with `APPROVED_BY_DEAN` status
3. `PUT /api/submissions/{submissionId}/approve` - Give final approval (changes status to `FINAL_APPROVED`)
4. `POST /api/submissions/finalize-my-list` - **COMPLETES ENTIRE GRADUATION PROCESS**

**Key Constraint**: Student Affairs cannot finalize unless:
- All faculty lists are finalized
- All submissions awaiting final review are processed

**Result**: When Student Affairs finalizes:
- Graduation object status changes from "IN_PROGRESS" to "COMPLETED"
- Graduation process is officially completed
- All students with `FINAL_APPROVED` submissions have graduated

### Step 6: Student Checks Graduation Status
**User**: Student  
**Actions**:
1. `GET /api/submissions/my-submissions` - View all their submissions
2. Look for submissions with status `FINAL_APPROVED` - indicates graduation

**How Students Know They Graduated**:
- If they have any submission with `FINAL_APPROVED` status = **GRADUATED** 🎓
- If all submissions are rejected or no `FINAL_APPROVED` found = **NOT GRADUATED**

## Key Workflow Constraints

### Finalization Rules:
1. **No Unfinalizing**: Once a list is finalized, it cannot be changed (maintains integrity)
2. **Sequential Finalization**: Each level must wait for previous levels to finalize
3. **Complete Processing**: All submissions must be processed before finalization
4. **Hierarchy Respect**: Higher levels cannot finalize until lower levels are complete

### Constraint Testing:
- Try finalizing with pending submissions → Should fail
- Try finalizing without prerequisite lists finalized → Should fail
- Try accessing endpoints with wrong roles → Should fail with 403

## Example Test Sequence

```bash
# 1. Start graduation (Student Affairs)
curl -X POST /api/submissions/regular-graduation/start \
  -H "Content-Type: application/json" \
  -d '{"term": "2023-FALL"}'

# 2. Try to finalize immediately (Advisor) - Should fail
curl -X POST /api/submissions/finalize-my-list

# 3. Process submissions first (Advisor)
curl -X PUT /api/submissions/{submissionId}/approve

# 4. Then finalize (Advisor) - Should succeed
curl -X POST /api/submissions/finalize-my-list

# 5. Continue up the hierarchy...
# (Department Secretary, Dean Officer, Student Affairs)

# 6. Student checks graduation status
curl -X GET /api/submissions/my-submissions
# Look for "FINAL_APPROVED" in response = graduated!
```

## Success Indicators
- ✅ Each role can only finalize after completing their responsibilities
- ✅ Finalization follows strict hierarchy (Advisor → Dept → Dean → Student Affairs)
- ✅ Graduation object status changes to "COMPLETED" when Student Affairs finalizes
- ✅ Students can see graduation status through their submission status
- ✅ No unfinalizing allowed (maintains process integrity)
- ✅ Clear audit trail of the entire process

This workflow ensures academic integrity while providing a clear, trackable graduation process. 