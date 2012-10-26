/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf.processes.addroles;

import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AssignmentsApprovals implements Serializable {

    private static final long serialVersionUID = -8603661687788638920L;

    private List<AssignmentApproval> results = new ArrayList<AssignmentApproval>();

    public List<AssignmentApproval> getAssignmentsApproval() {
        return results;
    }

    @Override
    public String toString() {
        return "AssignmentsApprovals: " + getAssignmentsApproval();
    }

    public void addResult(AssignmentType assignment, Boolean approved) {
        results.add(new AssignmentApproval(assignment, approved));
    }

    public class AssignmentApproval implements Serializable {

        private static final long serialVersionUID = -3746700225140955339L;

        AssignmentType assignmentType;
        boolean approved;

        public AssignmentApproval(AssignmentType assignment, Boolean approved) {
            this.assignmentType = assignment;
            this.approved = approved;
        }

        public String toString() {
            return "AssignmentApproval: assignment = " + assignmentType + ", result = " + approved;
        }

        public boolean isApproved() {
            return approved;
        }

        public void setApproved(boolean approved) {
            this.approved = approved;
        }

        public AssignmentType getAssignmentType() {
            return assignmentType;
        }

        public void setAssignmentType(AssignmentType assignmentType) {
            this.assignmentType = assignmentType;
        }
    }
}
