/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
