/*
 * Copyright (c) 2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
