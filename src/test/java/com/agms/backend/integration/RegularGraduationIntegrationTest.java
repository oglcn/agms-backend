// package com.agms.backend.integration;

// import static org.junit.jupiter.api.Assertions.*;

// import java.util.List;

// import org.junit.jupiter.api.Test;
// import org.springframework.transaction.annotation.Transactional;

// import com.agms.backend.dto.RegularGraduationTrackResponse;
// import com.agms.backend.dto.SubmissionResponse;
// import com.agms.backend.model.SubmissionStatus;

// import lombok.extern.slf4j.Slf4j;

// @Slf4j
// class RegularGraduationIntegrationTest extends BaseIntegrationTest {

// @Test
// @Transactional
// void testTrackBeforeStartingRegularGraduation() {
// log.info("=== Testing Track Before Starting Regular Graduation ===");

// // Set up authentication for Student Affairs
// setUpAuthenticationForStudentAffairs();

// String term = "Fall 2025";

// // PART 1: Track graduation process BEFORE starting it
// log.info("--- Part 1: Track Before Starting ---");

// RegularGraduationTrackResponse trackResponse =
// submissionService.trackRegularGraduation(term);

// // Verify the response indicates graduation has NOT been started
// assertNotNull(trackResponse);
// assertFalse(trackResponse.isStarted(), "Graduation should not be started
// yet");
// assertEquals(term, trackResponse.getTerm(), "Term should match the requested
// term");
// assertNull(trackResponse.getStatus(), "Status should be null when not
// started");
// assertNull(trackResponse.getGraduationId(), "Graduation ID should be null
// when not started");
// assertNull(trackResponse.getRequestDate(), "Request date should be null when
// not started");
// assertNull(trackResponse.getStudentAffairsEmpId(), "Student Affairs EmpId
// should be null when not started");

// log.info("✅ Track response before starting: isStarted={}, term={},
// status={}",
// trackResponse.isStarted(), trackResponse.getTerm(),
// trackResponse.getStatus());

// // PART 2: Start the regular graduation process
// log.info("--- Part 2: Start Regular Graduation ---");

// List<SubmissionResponse> createdSubmissions =
// submissionService.startRegularGraduation(term);

// // Verify submissions were created
// assertNotNull(createdSubmissions);
// assertTrue(createdSubmissions.size() > 0, "At least one submission should be
// created for eligible students");

// log.info("Started regular graduation and created {} submissions",
// createdSubmissions.size());

// // PART 3: Track graduation process AFTER starting it
// log.info("--- Part 3: Track After Starting ---");

// RegularGraduationTrackResponse trackResponseAfter =
// submissionService.trackRegularGraduation(term);

// // Verify the response indicates graduation HAS been started
// assertNotNull(trackResponseAfter);
// assertTrue(trackResponseAfter.isStarted(), "Graduation should be started
// now");
// assertEquals(term, trackResponseAfter.getTerm(), "Term should match the
// requested term");
// assertEquals("IN_PROGRESS", trackResponseAfter.getStatus(), "Status should be
// IN_PROGRESS");
// assertNotNull(trackResponseAfter.getGraduationId(), "Graduation ID should not
// be null when started");
// assertNotNull(trackResponseAfter.getRequestDate(), "Request date should not
// be null when started");
// assertNotNull(trackResponseAfter.getStudentAffairsEmpId(), "Student Affairs
// EmpId should not be null when started");

// log.info("✅ Track response after starting: isStarted={}, term={}, status={},
// graduationId={}",
// trackResponseAfter.isStarted(), trackResponseAfter.getTerm(),
// trackResponseAfter.getStatus(), trackResponseAfter.getGraduationId());

// // PART 4: Verify tracking different terms
// log.info("--- Part 4: Track Different Term ---");

// String differentTerm = "Spring 2026";
// RegularGraduationTrackResponse trackDifferentTerm =
// submissionService.trackRegularGraduation(differentTerm);

// // Verify different term shows as not started
// assertNotNull(trackDifferentTerm);
// assertFalse(trackDifferentTerm.isStarted(), "Different term should not be
// started");
// assertEquals(differentTerm, trackDifferentTerm.getTerm(), "Term should match
// the requested different term");
// assertNull(trackDifferentTerm.getStatus(), "Status should be null for
// different term");

// log.info("✅ Track response for different term: isStarted={}, term={}",
// trackDifferentTerm.isStarted(), trackDifferentTerm.getTerm());

// log.info("=== Track Before Starting Regular Graduation Test Completed
// Successfully ===");
// }

// @Test
// @Transactional
// void testRegularGraduationProcessAndDuplicatePrevention() {
// log.info("=== Testing Regular Graduation Process and Duplicate Prevention
// ===");

// // Set up authentication for Student Affairs
// setUpAuthenticationForStudentAffairs();

// String term = "Spring 2025";

// // PART 1: Test initial regular graduation process
// log.info("--- Part 1: Initial Regular Graduation Process ---");

// List<SubmissionResponse> firstRun =
// submissionService.startRegularGraduation(term);

// // Verify submissions were created
// assertNotNull(firstRun);
// assertTrue(firstRun.size() > 0, "At least one submission should be created
// for eligible students");

// log.info("First run created {} submissions for regular graduation",
// firstRun.size());

// // Verify that all created submissions have the correct properties
// for (SubmissionResponse submission : firstRun) {
// assertNotNull(submission.getSubmissionId());
// assertEquals(SubmissionStatus.PENDING, submission.getStatus());
// assertNotNull(submission.getStudentNumber());
// assertNotNull(submission.getStudentName());
// assertNotNull(submission.getAdvisorListId());
// assertTrue(submission.getContent().contains("Regular graduation
// application"));
// assertTrue(submission.getContent().contains(term));

// log.info("Verified submission for student: {} with ID: {}",
// submission.getStudentNumber(), submission.getSubmissionId());
// }

// // Verify that only eligible students got submissions
// // According to ubys.json, students should be eligible based on their data
// boolean foundEligibleStudent = firstRun.stream()
// .anyMatch(s -> s.getStudentNumber().startsWith("S"));

// assertTrue(foundEligibleStudent, "At least one eligible student should have a
// submission created");

// // PART 2: Test duplicate prevention
// log.info("--- Part 2: Duplicate Prevention Test ---");

// // Attempt to start the regular graduation process again for the same term
// // This should either throw an exception or return an empty list
// try {
// List<SubmissionResponse> secondRun =
// submissionService.startRegularGraduation(term);

// // If no exception is thrown, verify that no new submissions were created
// assertEquals(0, secondRun.size(),
// "Second run should not create any submissions as graduation already exists
// for this term");

// log.info("✅ Duplicate prevention successful: Second run created {}
// submissions", secondRun.size());

// } catch (IllegalStateException e) {
// // This is also acceptable - the service should prevent duplicate graduation
// processes
// assertTrue(e.getMessage().contains("already been started"),
// "Exception should indicate that graduation has already been started");

// log.info("✅ Duplicate prevention successful: Exception thrown - {}",
// e.getMessage());
// }

// log.info("=== Regular Graduation Process and Duplicate Prevention Test
// Completed Successfully ===");
// }
// }