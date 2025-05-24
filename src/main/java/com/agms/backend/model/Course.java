package com.agms.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    private String code;
    private String name;
    private String grade;
    private int credit;
    
    /**
     * Converts letter grade to GPA points
     * @return GPA points for this course
     */
    public double getGpaPoints() {
        switch (grade) {
            case "AA": return 4.0;
            case "BA": return 3.5;
            case "BB": return 3.0;
            case "CB": return 2.5;
            case "CC": return 2.0;
            case "DC": return 1.5;
            case "DD": return 1.0;
            case "FF": return 0.0;
            default: return 0.0;
        }
    }

    /**
     * Checks if the grade is a passing grade
     * @return true if the grade is passing (CC or better), false otherwise
     */
    public boolean isPassing() {
        return getGpaPoints() >= 2.0;
    }
} 