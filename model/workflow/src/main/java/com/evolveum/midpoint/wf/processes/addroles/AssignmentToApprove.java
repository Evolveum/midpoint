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

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import java.io.Serializable;

public class AssignmentToApprove implements Serializable {

    private static final long serialVersionUID = 5834362449970050178L;

    private RoleType role;
    private AssignmentType assignment;
//    private ApprovalStructure approvalStructure;
    private ApprovalSchemaType approvalSchema;

    public AssignmentToApprove(AssignmentType assignment, RoleType role) {
        this.role = role;
        this.assignment = assignment;

        if (role.getApprovalSchema() != null) {
            approvalSchema = role.getApprovalSchema();
        } else if (!role.getApproverRef().isEmpty() || !role.getApproverExpression().isEmpty()) {
            approvalSchema = new ApprovalSchemaType();
            fillInApprovalSchema(approvalSchema, role);
        } else {
            throw new SystemException("Neither approvalSchema nor approverRef/approverExpression is filled-in for role " + role);
        }
    }

    private void fillInApprovalSchema(ApprovalSchemaType ast, RoleType role) {
        //default: all approvers in one level, evaluation strategy = allMustApprove

        ApprovalLevelType level = new ApprovalLevelType();
        level.getApproverRef().addAll(role.getApproverRef());
        level.getApproverExpression().addAll(role.getApproverExpression());
        level.setEvaluationStrategy(LevelEvaluationStrategyType.ALL_MUST_AGREE);
        level.setAutomaticallyApproved(role.getAutomaticallyApproved());

        ast.getLevel().add(level);
    }

    public AssignmentType getAssignment() {
        return assignment;
    }

    public RoleType getRole() {
        return role;
    }

    // todo i18n
    public String getTimeInterval() {
        return AddRoleAssignmentWrapper.formatTimeIntervalBrief(assignment);
    }

    // todo i18n
    public String getRoleWithTimeInterval() {
        String role = getRole().getName().getOrig();
        String time = getTimeInterval();
        return role + (time.isEmpty() ? "" : " (" + time + ")");
    }

    public ApprovalSchemaType getApprovalSchema() {
        return approvalSchema;
    }

    public void setApprovalSchema(ApprovalSchemaType approvalSchema) {
        this.approvalSchema = approvalSchema;
    }

    @Override
    public String toString() {
        return "AssignmentToApprove: [role=" + role + ", assignment=" + assignment + ", approvalSchema=" + approvalSchema + "]";
    }
}
