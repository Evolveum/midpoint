/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.processes.general;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import java.io.Serializable;
import java.util.List;

public class ApprovalRequestImpl<I extends Serializable> implements ApprovalRequest<I> {

    private static final long serialVersionUID = 5111362449970050179L;

    private I itemToApprove;
    private ApprovalSchemaType approvalSchema;

    public ApprovalRequestImpl(I itemToApprove, ApprovalSchemaType approvalSchema) {
        this.itemToApprove = itemToApprove;
        this.approvalSchema = approvalSchema;
    }

    public ApprovalRequestImpl(I itemToApprove, ApprovalSchemaType approvalSchema, List<ObjectReferenceType> approverRef, List<ExpressionType> approverExpression, ExpressionType automaticallyApproved) {

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


    @Override
    public ApprovalSchemaType getApprovalSchema() {
        return approvalSchema;
    }

    public void setApprovalSchema(ApprovalSchemaType approvalSchema) {
        this.approvalSchema = approvalSchema;
    }

    @Override
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
