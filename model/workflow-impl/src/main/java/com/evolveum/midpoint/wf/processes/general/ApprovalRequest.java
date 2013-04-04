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

package com.evolveum.midpoint.wf.processes.general;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.wf.processes.addrole.AddRoleAssignmentWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import java.io.Serializable;
import java.util.List;

public class ApprovalRequest<I extends Serializable> implements Serializable {

    private static final long serialVersionUID = 5834362449970050179L;

    private I itemToApprove;
    private ApprovalSchemaType approvalSchema;

    public ApprovalRequest(I itemToApprove, ApprovalSchemaType approvalSchema) {
        this.itemToApprove = itemToApprove;
        this.approvalSchema = approvalSchema;
    }

    public ApprovalRequest(I itemToApprove, ApprovalSchemaType approvalSchema, List<ObjectReferenceType> approverRef, List<ExpressionType> approverExpression, ExpressionType automaticallyApproved) {

        this.itemToApprove = itemToApprove;
        if (approvalSchema != null) {
            this.approvalSchema = approvalSchema;
        } else if ((approverRef != null && !approverRef.isEmpty()) || (approverExpression != null && !approverExpression.isEmpty())) {
            this.approvalSchema = new ApprovalSchemaType();
            fillInApprovalSchema(this.approvalSchema, approverRef, approverExpression, automaticallyApproved);
        } else {
            throw new IllegalArgumentException("Neither approvalSchema nor approverRef/approverExpression is filled-in for itemToApprove = " + itemToApprove);
        }
    }

    private void fillInApprovalSchema(ApprovalSchemaType approvalSchema, List<ObjectReferenceType> approverRef, List<ExpressionType> approverExpression, ExpressionType automaticallyApproved) {
        //default: all approvers in one level, evaluation strategy = allMustApprove

        ApprovalLevelType level = new ApprovalLevelType();
        if (approverRef != null) {
            level.getApproverRef().addAll(approverRef);
        }
        if (approverExpression != null) {
            level.getApproverExpression().addAll(approverExpression);
        }
        level.setEvaluationStrategy(LevelEvaluationStrategyType.ALL_MUST_AGREE);
        level.setAutomaticallyApproved(automaticallyApproved);

        approvalSchema.getLevel().add(level);
    }


    public ApprovalSchemaType getApprovalSchema() {
        return approvalSchema;
    }

    public void setApprovalSchema(ApprovalSchemaType approvalSchema) {
        this.approvalSchema = approvalSchema;
    }

    public I getItemToApprove() {
        return itemToApprove;
    }

    public void setItemToApprove(I itemToApprove) {
        this.itemToApprove = itemToApprove;
    }

    @Override
    public String toString() {
        return "ApprovalRequest: [itemToApprove=" + itemToApprove + ", approvalSchema=" + approvalSchema + "]";
    }
}
