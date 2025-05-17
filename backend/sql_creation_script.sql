-- Drop tables if they exist (in reverse order of creation to handle dependencies)
DROP TABLE IF EXISTS tSubmission CASCADE;
DROP TABLE IF EXISTS tFile CASCADE;
DROP TABLE IF EXISTS tGraduationList CASCADE;
DROP TABLE IF EXISTS tGraduation CASCADE;
DROP TABLE IF EXISTS tStudentAffairs CASCADE;
DROP TABLE IF EXISTS tFacultyList CASCADE;
DROP TABLE IF EXISTS tDeanOfficer CASCADE;
DROP TABLE IF EXISTS tDepartmentList CASCADE;
DROP TABLE IF EXISTS tDepartmentSecretary CASCADE;
DROP TABLE IF EXISTS tAdvisorList CASCADE;
DROP TABLE IF EXISTS tAdvisor CASCADE;
DROP TABLE IF EXISTS tStudent CASCADE;
DROP TABLE IF EXISTS tUser CASCADE;


-- Create tables for academic information system based on the ER diagram
-- Starting with the root entities


-- tUser table (Root entity)
CREATE TABLE tUser (
    firstName VARCHAR(100),
    lastName VARCHAR(100),
    userId INT PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    instituteId INT NOT NULL
);

-- tStudentAffairs table
CREATE TABLE tStudentAffairs (
    empId INT PRIMARY KEY,
    userId INT REFERENCES tUser(userId)
);

-- tGraduation table
CREATE TABLE tGraduation (
    graduationId INT PRIMARY KEY,
    requestDate DATE NOT NULL,
    term VARCHAR(50) NOT NULL,
    studentAffairsId INT REFERENCES tStudentAffairs(empId)
);

-- tGraduationList table
CREATE TABLE tGraduationList (
    listId INT PRIMARY KEY,
    creationDate DATE,
    graduationId INT REFERENCES tGraduation(graduationId)
);

-- tDeanOfficer table
CREATE TABLE tDeanOfficer (
    empId INT PRIMARY KEY,
    userId INT REFERENCES tUser(userId)
);

-- tFacultyList table
CREATE TABLE tFacultyList (
    facultyListId INT PRIMARY KEY,
    creationDate DATE,
    faculty VARCHAR(100),
    deanOfficerId INT REFERENCES tDeanOfficer(empId),
    tGraduationListId INT REFERENCES tGraduationList(listId)
);

-- tSecretary table
CREATE TABLE tDepartmentSecretary (
    empId INT PRIMARY KEY,
    userId INT REFERENCES tUser(userId)
);

-- tDepartmentList table
CREATE TABLE tDepartmentList (
    deptListId INT PRIMARY KEY,
    creationDate DATE,
    department VARCHAR(100),
    secretaryId INT REFERENCES tDepartmentSecretary(empId),
    facultyListId INT REFERENCES tFacultyList(facultyListId)
);

-- tAdvisor table
CREATE TABLE tAdvisor (
    empId INT PRIMARY KEY,
    userId INT REFERENCES tUser(userId)
);

-- tAdvisorList table
CREATE TABLE tAdvisorList (
    advisorListId INT PRIMARY KEY,
    creationDate DATE,
    advisorId INT REFERENCES tAdvisor(empId),
    deptListId INT REFERENCES tDepartmentList(deptListId)
);

-- tStudent table
CREATE TABLE tStudent (
    studentId INT PRIMARY KEY,
    userId INT REFERENCES tUser(userId),
    advisorListId INT REFERENCES tAdvisorList(advisorListId)
);

-- tSubmission table
CREATE TABLE tSubmission (
    submissionId INT PRIMARY KEY,
    submissionDate DATE NOT NULL,
    content TEXT,
    status VARCHAR(50) CHECK (status IN ('Pending', 'Approved', 'Rejected')),
    studentId INT REFERENCES tStudent(studentId)
);

-- tFile table
CREATE TABLE tFile (
    fileId INT PRIMARY KEY,
    fileName VARCHAR(200) NOT NULL,
    fileType VARCHAR(50) NOT NULL,
    uploadDate DATE NOT NULL,
    uploaderId INT REFERENCES tUser(userId),
    filePath VARCHAR(200) NOT NULL,
    submissionId INT REFERENCES tSubmission(submissionId)
);

-- Trigger function to insert into the necessary table based on the user's role
CREATE OR REPLACE FUNCTION insert_based_on_role() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.role = 'StudentAffairs' THEN
        INSERT INTO tStudentAffairs (empId, userId) VALUES (NEW.instituteId, NEW.userId);
    ELSIF NEW.role = 'DeanOfficer' THEN
        INSERT INTO tDeanOfficer (empId, userId) VALUES (NEW.instituteId, NEW.userId);
    ELSIF NEW.role = 'DepartmentSecretary' THEN
        INSERT INTO tDepartmentSecretary (empId, userId) VALUES (NEW.instituteId, NEW.userId);
    ELSIF NEW.role = 'Advisor' THEN
        INSERT INTO tAdvisor (empId, userId) VALUES (NEW.instituteId, NEW.userId);
    ELSIF NEW.role = 'Student' THEN
        INSERT INTO tStudent (studentId, userId) VALUES (NEW.instituteId, NEW.userId);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to call the function after insert on tUser
CREATE TRIGGER after_user_insert
AFTER INSERT ON tUser
FOR EACH ROW
EXECUTE FUNCTION insert_based_on_role();

-- MOCK DATA INSERTS
-- Insert a user with the role of StudentAffairs
INSERT INTO tUser (firstName, lastName, userId, email, password, role, instituteId)
VALUES ('John', 'Doe', 1, 'johndoe@example.com', 'password123', 'StudentAffairs', 101);

-- Insert a user with the role of DeanOfficer
INSERT INTO tUser (firstName, lastName, userId, email, password, role, instituteId)
VALUES ('Jane', 'Smith', 2, 'janesmith@example.com', 'password123', 'DeanOfficer', 102);

-- Insert a user with the role of DepartmentSecretary
INSERT INTO tUser (firstName, lastName, userId, email, password, role, instituteId)
VALUES ('Alice', 'Johnson', 3, 'alicejohnson@example.com', 'password123', 'DepartmentSecretary', 103);

-- Insert a user with the role of Advisor
INSERT INTO tUser (firstName, lastName, userId, email, password, role, instituteId)
VALUES ('Bob', 'Brown', 4, 'bobbrown@example.com', 'password123', 'Advisor', 104);

-- Insert a user with the role of Student
INSERT INTO tUser (firstName, lastName, userId, email, password, role, instituteId)
VALUES ('Charlie', 'Davis', 5, 'charliedavis@example.com', 'password123', 'Student', 105);


----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
-- MOCK DATA SQL SCRIPT
-- This script generates mock data for the academic information system
-- It creates 50 students, 5 advisors, 5 department secretaries, 5 dean officers, and 5 student affairs officers

-- Clear any existing mock data (except the ones already created)
DELETE FROM tFile;
DELETE FROM tSubmission;
DELETE FROM tStudent WHERE studentId > 105;
DELETE FROM tAdvisorList;
DELETE FROM tAdvisor WHERE empId > 104;
DELETE FROM tDepartmentList;
DELETE FROM tDepartmentSecretary WHERE empId > 103;
DELETE FROM tFacultyList;
DELETE FROM tDeanOfficer WHERE empId > 102;
DELETE FROM tGraduationList;
DELETE FROM tGraduation;
DELETE FROM tStudentAffairs WHERE empId > 101;
DELETE FROM tUser WHERE userId > 5;

-- Constants and configuration
DO $$
DECLARE
    user_id_counter INT := 6; -- Starting from 6 since you already have 5 users
    current_date DATE := CURRENT_DATE;
    departments VARCHAR[] := ARRAY['Computer Science', 'Electrical Engineering', 'Mechanical Engineering', 'Civil Engineering', 'Mathematics', 'Physics', 'Chemistry', 'Biology', 'Economics', 'Business'];
    faculties VARCHAR[] := ARRAY['Engineering', 'Science', 'Social Sciences', 'Business', 'Arts'];
BEGIN

    -- Generate Student Affairs Officers (5)
    FOR i IN 1..5 LOOP
        IF i > 1 THEN -- Skip the first one since it's already inserted
            INSERT INTO tUser (firstName, lastName, userId, email, password, role, instituteId)
            VALUES (
                'SA_First' || i, 
                'SA_Last' || i, 
                user_id_counter, 
                'studentaffairs' || i || '@university.edu', 
                'sa_password' || i, 
                'StudentAffairs', 
                100 + i
            );
            user_id_counter := user_id_counter + 1;
        END IF;
    END LOOP;

    -- Generate Dean Officers (5)
    FOR i IN 1..5 LOOP
        IF i > 1 THEN -- Skip the first one since it's already inserted
            INSERT INTO tUser (firstName, lastName, userId, email, password, role, instituteId)
            VALUES (
                'Dean_First' || i, 
                'Dean_Last' || i, 
                user_id_counter, 
                'deanofficer' || i || '@university.edu', 
                'dean_password' || i, 
                'DeanOfficer', 
                200 + i
            );
            user_id_counter := user_id_counter + 1;
        END IF;
    END LOOP;

    -- Generate Department Secretaries (5)
    FOR i IN 1..5 LOOP
        IF i > 1 THEN -- Skip the first one since it's already inserted
            INSERT INTO tUser (firstName, lastName, userId, email, password, role, instituteId)
            VALUES (
                'Sec_First' || i, 
                'Sec_Last' || i, 
                user_id_counter, 
                'secretary' || i || '@university.edu', 
                'sec_password' || i, 
                'DepartmentSecretary', 
                300 + i
            );
            user_id_counter := user_id_counter + 1;
        END IF;
    END LOOP;

    -- Generate Advisors (5)
    FOR i IN 1..5 LOOP
        IF i > 1 THEN -- Skip the first one since it's already inserted
            INSERT INTO tUser (firstName, lastName, userId, email, password, role, instituteId)
            VALUES (
                'Adv_First' || i, 
                'Adv_Last' || i, 
                user_id_counter, 
                'advisor' || i || '@university.edu', 
                'adv_password' || i, 
                'Advisor', 
                400 + i
            );
            user_id_counter := user_id_counter + 1;
        END IF;
    END LOOP;

    -- Generate Students (50)
    FOR i IN 1..50 LOOP
        IF i > 1 THEN -- Skip the first one since it's already inserted
            INSERT INTO tUser (firstName, lastName, userId, email, password, role, instituteId)
            VALUES (
                'Student_First' || i, 
                'Student_Last' || i, 
                user_id_counter, 
                'student' || i || '@university.edu', 
                'student_password' || i, 
                'Student', 
                500 + i
            );
            user_id_counter := user_id_counter + 1;
        END IF;
    END LOOP;

    -- Create Graduation records
    FOR i IN 1..3 LOOP
        INSERT INTO tGraduation (graduationId, requestDate, term, studentAffairsId)
        VALUES (
            i, 
            current_date - (i * 30), 
            CASE 
                WHEN i = 1 THEN 'Spring 2025'
                WHEN i = 2 THEN 'Fall 2024'
                ELSE 'Summer 2024'
            END,
            101
        );

        -- Create Graduation Lists for each graduation
        INSERT INTO tGraduationList (listId, creationDate, graduationId)
        VALUES (i, current_date - (i * 25), i);
    END LOOP;

    -- Create Faculty Lists connected to Graduation Lists
    FOR i IN 1..5 LOOP
        INSERT INTO tFacultyList (facultyListId, creationDate, faculty, deanOfficerId, tGraduationListId)
        VALUES (
            i, 
            current_date - (i * 20), 
            faculties[(i % array_length(faculties, 1)) + 1], 
            102, 
            (i % 3) + 1
        );
    END LOOP;

    -- Create Department Lists connected to Faculty Lists
    FOR i IN 1..10 LOOP
        INSERT INTO tDepartmentList (deptListId, creationDate, department, secretaryId, facultyListId)
        VALUES (
            i, 
            current_date - (i * 15), 
            departments[i],
            103, 
            (i % 5) + 1
        );
    END LOOP;

    -- Create Advisor Lists connected to Department Lists
    FOR i IN 1..5 LOOP
        INSERT INTO tAdvisorList (advisorListId, creationDate, advisorId, deptListId)
        VALUES (
            i, 
            current_date - (i * 10), 
            CASE 
                WHEN i = 1 THEN 104  -- First advisor already exists
                ELSE 400 + i         -- Rest were created above
            END, 
            i * 2    -- Connected to different departments
        );
    END LOOP;

    -- Update students to be connected to advisor lists
    UPDATE tStudent 
    SET advisorListId = (studentId % 5) + 1
    WHERE studentId >= 105;

    -- Create sample submissions for each student
    FOR i IN 1..50 LOOP
        -- Only create submissions for existing students
        IF (i = 1) OR (500 + i IN (SELECT instituteId FROM tUser WHERE role = 'Student')) THEN
            INSERT INTO tSubmission (submissionId, submissionDate, content, status, studentId)
            VALUES (
                i, 
                current_date - (i % 60), 
                'Graduation application for student ' || (500 + i), 
                CASE 
                    WHEN i % 3 = 0 THEN 'Approved'
                    WHEN i % 3 = 1 THEN 'Rejected'
                    ELSE 'Pending'
                END,
                CASE 
                    WHEN i = 1 THEN 105  -- First student already exists
                    ELSE 500 + i         -- Rest were created above
                END
            );

            -- Create sample files for submissions
            INSERT INTO tFile (fileId, fileName, fileType, uploadDate, uploaderId, filePath, submissionId)
            VALUES (
                i, 
                'transcript_student_' || (500 + i) || '.pdf', 
                'application/pdf', 
                current_date - (i % 60),
                CASE 
                    WHEN i = 1 THEN 5  -- First student's user ID
                    ELSE user_id_counter - 50 + i - 1  -- Calculate the user IDs for the students we created
                END,
                '/files/transcripts/' || (500 + i) || '.pdf',
                i
            );
        END IF;
    END LOOP;

END $$;

-- Create temporary tables for names
CREATE TEMPORARY TABLE temp_first_names (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100)
);

CREATE TEMPORARY TABLE temp_last_names (
    id SERIAL PRIMARY KEY, 
    name VARCHAR(100)
);

-- Insert first names
INSERT INTO temp_first_names (name) VALUES 
    ('James'), ('John'), ('Robert'), ('Michael'), ('William'),
    ('David'), ('Richard'), ('Joseph'), ('Thomas'), ('Charles'),
    ('Christopher'), ('Daniel'), ('Matthew'), ('Anthony'), ('Mark'),
    ('Donald'), ('Steven'), ('Paul'), ('Andrew'), ('Joshua'),
    ('Emma'), ('Olivia'), ('Ava'), ('Isabella'), ('Sophia'),
    ('Mia'), ('Charlotte'), ('Amelia'), ('Harper'), ('Evelyn'),
    ('Abigail'), ('Emily'), ('Elizabeth'), ('Sofia'), ('Avery'),
    ('Ella'), ('Scarlett'), ('Grace'), ('Victoria'), ('Riley'),
    ('Aria'), ('Lily'), ('Aubrey'), ('Zoey'), ('Penelope'),
    ('Layla'), ('Lillian'), ('Addison'), ('Natalie'), ('Camila');

-- Insert last names
INSERT INTO temp_last_names (name) VALUES
    ('Smith'), ('Johnson'), ('Williams'), ('Jones'), ('Brown'),
    ('Davis'), ('Miller'), ('Wilson'), ('Moore'), ('Taylor'),
    ('Anderson'), ('Thomas'), ('Jackson'), ('White'), ('Harris'),
    ('Martin'), ('Thompson'), ('Garcia'), ('Martinez'), ('Robinson'),
    ('Clark'), ('Rodriguez'), ('Lewis'), ('Lee'), ('Walker'),
    ('Hall'), ('Allen'), ('Young'), ('Hernandez'), ('King'),
    ('Wright'), ('Lopez'), ('Hill'), ('Scott'), ('Green'),
    ('Adams'), ('Baker'), ('Gonzalez'), ('Nelson'), ('Carter'),
    ('Mitchell'), ('Perez'), ('Roberts'), ('Turner'), ('Phillips'),
    ('Campbell'), ('Parker'), ('Evans'), ('Edwards'), ('Collins');

-- Update students with realistic names
DO $$
DECLARE
    student_rec RECORD;
    first_name_count INTEGER;
    last_name_count INTEGER;
    first_idx INTEGER;
    last_idx INTEGER;
BEGIN
    SELECT COUNT(*) INTO first_name_count FROM temp_first_names;
    SELECT COUNT(*) INTO last_name_count FROM temp_last_names;
    
    FOR student_rec IN SELECT userId FROM tUser WHERE role = 'Student' AND userId > 5 ORDER BY userId
    LOOP
        first_idx := 1 + (student_rec.userId % first_name_count);
        last_idx := 1 + ((student_rec.userId * 3) % last_name_count);
        
        UPDATE tUser 
        SET firstName = (SELECT name FROM temp_first_names WHERE id = first_idx),
            lastName = (SELECT name FROM temp_last_names WHERE id = last_idx)
        WHERE userId = student_rec.userId;
    END LOOP;
END $$;

-- Update faculty data using temporary tables
DO $$
DECLARE
    advisor_rec RECORD;
    first_names VARCHAR[] := ARRAY['Professor Steven', 'Professor Maria', 'Professor David', 'Professor Angela', 'Dr. Robert'];
    last_names VARCHAR[] := ARRAY['Anderson', 'Rodriguez', 'Smith', 'Patel', 'Johnson'];
    counter INTEGER := 1;
BEGIN
    FOR advisor_rec IN SELECT userId FROM tUser WHERE role = 'Advisor' AND userId > 4 ORDER BY userId
    LOOP
        UPDATE tUser 
        SET firstName = first_names[counter],
            lastName = last_names[counter]
        WHERE userId = advisor_rec.userId;
        
        counter := counter + 1;
        IF counter > array_length(first_names, 1) THEN
            counter := 1;
        END IF;
    END LOOP;
END $$;

-- Update dean officer data
DO $$
DECLARE
    dean_rec RECORD;
    first_names VARCHAR[] := ARRAY['Dean Alexandra', 'Dean Jonathan', 'Dean Patricia', 'Dean Richard', 'Dean Michelle'];
    last_names VARCHAR[] := ARRAY['Thomas', 'Wyatt', 'Henderson', 'Davis', 'Wong'];
    counter INTEGER := 1;
BEGIN
    FOR dean_rec IN SELECT userId FROM tUser WHERE role = 'DeanOfficer' AND userId > 2 ORDER BY userId
    LOOP
        UPDATE tUser 
        SET firstName = first_names[counter],
            lastName = last_names[counter]
        WHERE userId = dean_rec.userId;
        
        counter := counter + 1;
        IF counter > array_length(first_names, 1) THEN
            counter := 1;
        END IF;
    END LOOP;
END $$;

-- Update secretary data
DO $$
DECLARE
    sec_rec RECORD;
    first_names VARCHAR[] := ARRAY['Secretary Linda', 'Secretary James', 'Secretary Sarah', 'Secretary Michael', 'Secretary Lisa'];
    last_names VARCHAR[] := ARRAY['Martinez', 'Wilson', 'Johnson', 'Chen', 'Taylor'];
    counter INTEGER := 1;
BEGIN
    FOR sec_rec IN SELECT userId FROM tUser WHERE role = 'DepartmentSecretary' AND userId > 3 ORDER BY userId
    LOOP
        UPDATE tUser 
        SET firstName = first_names[counter],
            lastName = last_names[counter]
        WHERE userId = sec_rec.userId;
        
        counter := counter + 1;
        IF counter > array_length(first_names, 1) THEN
            counter := 1;
        END IF;
    END LOOP;
END $$;

-- Update student affairs data
DO $$
DECLARE
    sa_rec RECORD;
    first_names VARCHAR[] := ARRAY['Affairs Lisa', 'Affairs Robert', 'Affairs Emily', 'Affairs Daniel', 'Affairs Jennifer'];
    last_names VARCHAR[] := ARRAY['Parker', 'Washington', 'Brown', 'Garcia', 'Williams'];
    counter INTEGER := 1;
BEGIN
    FOR sa_rec IN SELECT userId FROM tUser WHERE role = 'StudentAffairs' AND userId > 1 ORDER BY userId
    LOOP
        UPDATE tUser 
        SET firstName = first_names[counter],
            lastName = last_names[counter]
        WHERE userId = sa_rec.userId;
        
        counter := counter + 1;
        IF counter > array_length(first_names, 1) THEN
            counter := 1;
        END IF;
    END LOOP;
END $$;

-- Display counts of all users by role
SELECT role, COUNT(*) FROM tUser GROUP BY role ORDER BY role;

-- Drop temporary tables used for names
DROP TABLE IF EXISTS temp_first_names;
DROP TABLE IF EXISTS temp_last_names;