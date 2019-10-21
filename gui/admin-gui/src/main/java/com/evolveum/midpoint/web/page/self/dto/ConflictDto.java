/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self.dto;

import java.io.Serializable;

/**
 * Created by honchar.
 */
public class ConflictDto implements Serializable {
    private AssignmentConflictDto assignment1;
    private AssignmentConflictDto assignment2;

    private boolean resolved;
    private boolean warning;

    public ConflictDto(AssignmentConflictDto assignment1, AssignmentConflictDto assignment2, boolean warning){
        this.assignment1 = assignment1;
        this.assignment2 = assignment2;
        this.warning = warning;
    }

    public AssignmentConflictDto getAssignment1() {
        return assignment1;
    }

    public AssignmentConflictDto getAssignment2() {
        return assignment2;
    }

    public boolean isResolved() {
        return assignment1.isResolved() || assignment2.isResolved();
    }

    public boolean isWarning() {
        return warning;
    }
}
