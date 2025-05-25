# Efficient Test Fix Prompt

Copy and paste this prompt into a new chat session for quick test fixes:

---

## 🚀 **EFFICIENT TEST FIX REQUEST**

I have a Spring Boot backend project with failing tests after implementing regular graduation tracking functionality. The core feature works perfectly (all `RegularGraduationTrackingTest` tests pass), but existing tests are failing due to authentication and validation changes.

### **CONTEXT:**
- ✅ **Working**: Regular graduation tracking functionality is complete
- ✅ **Working**: `RegularGraduationTrackingTest` - all 4 tests pass
- 🔴 **Failing**: `SubmissionServiceImplTest` and `SubmissionControllerTest`

### **ROOT CAUSE:**
Added authentication requirements to `SubmissionServiceImpl`:
- `getCurrentUserRole()`, `getCurrentUserEmpId()`, `getCurrentUserEmail()` methods
- Role validation in `startRegularGraduation()` (only STUDENT_AFFAIRS allowed)
- Changed error status from FORBIDDEN to CONFLICT
- Added new `/regular-graduation/track` endpoint

### **MAIN ISSUES:**
1. **Authentication Context Missing** - Tests calling auth-required methods without Spring Security context
2. **Wrong Roles** - Tests using incorrect roles for operations
3. **Status Code Changes** - Tests expecting old HTTP status codes
4. **Missing Test Data** - Tests need user entities for `getCurrentUserEmpId()`

### **QUICK FIX PATTERNS NEEDED:**

```java
// 1. Authentication Setup
private void setUpAuth(String email, String role) {
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    SecurityContextHolder.getContext().setAuthentication(auth);
}

// 2. Test Data Creation
StudentAffairs sa = StudentAffairs.builder()
    .empId("TEST_SA").email("test@edu").firstName("Test").lastName("SA").build();
studentAffairsRepository.save(sa);

// 3. Status Code Updates
.andExpect(status().isConflict()); // Changed from isForbidden()
```

### **REQUEST:**
Please help me fix the failing tests by:
1. **Priority 1**: Fix authentication context issues in failing tests
2. **Priority 2**: Update expected HTTP status codes 
3. **Priority 3**: Add missing test data setup
4. Use `RegularGraduationTrackingTest` as reference for proper patterns

**Files to focus on:**
- `SubmissionServiceImplTest.java`
- `SubmissionControllerTest.java`

**Run specific tests with:** `mvn test -Dtest=SubmissionServiceImplTest`

---

*Attach the failing test files and I'll provide targeted fixes for each failing test method.* 