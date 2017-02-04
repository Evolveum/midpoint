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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

@Deprecated
public class ApprovalRequestImpl<I extends Serializable> implements ApprovalRequest<I> {

    private static final Trace LOGGER = TraceManager.getTrace(ApprovalRequestImpl.class);

    private static final long serialVersionUID = 5111362449970050179L;

    private I itemToApprove;

    private ApprovalSchemaType approvalSchemaType;

	public ApprovalRequestImpl(I itemToApprove, PcpAspectConfigurationType config, @NotNull PrismContext prismContext) {
		this.itemToApprove = itemToApprove;
		approvalSchemaType = getSchemaFromConfig(config, prismContext);
	}

	public ApprovalRequestImpl(I itemToApprove, PcpAspectConfigurationType config, ApprovalSchemaType approvalSchema,
			List<ObjectReferenceType> approverRef, List<ExpressionType> approverExpression,
			ExpressionType automaticallyApproved, @NotNull PrismContext prismContext) {
		this.itemToApprove = itemToApprove;
		approvalSchemaType = getSchemaFromConfigAndParameters(config, approvalSchema, approverRef, approverExpression,
				automaticallyApproved, prismContext);
	}

    private ApprovalSchemaType getSchemaFromConfig(PcpAspectConfigurationType config, @NotNull PrismContext prismContext) {
        if (config == null) {
			return null;
		} else {
        	return getSchema(config.getApprovalSchema(), config.getApproverRef(), config.getApproverExpression(),
					config.getAutomaticallyApproved(), prismContext);
		}
    }

    private ApprovalSchemaType getSchema(ApprovalSchemaType schema, List<ObjectReferenceType> approverRef,
			List<ExpressionType> approverExpression, ExpressionType automaticallyApproved, @NotNull PrismContext prismContext) {
		if (schema != null) {
			return schema;
		} else {
        	schema = new ApprovalSchemaType(prismContext);
        	ApprovalLevelType level = new ApprovalLevelType(prismContext);
        	level.getApproverRef().addAll(CloneUtil.cloneCollectionMembers(approverRef));
        	level.getApproverExpression().addAll(approverExpression);
        	level.setAutomaticallyApproved(automaticallyApproved);
        	schema.getLevel().add(level);
        	return schema;
        }
    }

    private ApprovalSchemaType getSchemaFromConfigAndParameters(PcpAspectConfigurationType config, ApprovalSchemaType approvalSchema,
            List<ObjectReferenceType> approverRef, List<ExpressionType> approverExpression, ExpressionType automaticallyApproved,
            @NotNull PrismContext prismContext) {
        if (config != null &&
                (!config.getApproverRef().isEmpty() ||
                config.getApprovalSchema() != null ||
                !config.getApproverExpression().isEmpty() ||
                config.getAutomaticallyApproved() != null)) {
        	return getSchemaFromConfig(config, prismContext);
        } else {
        	return getSchema(approvalSchema, approverRef, approverExpression, automaticallyApproved, prismContext);
        }
    }

	@Override
	public ApprovalSchemaType getApprovalSchemaType() {
		if (approvalSchemaType == null) {
			approvalSchemaType = new ApprovalSchemaType();
		}
		return approvalSchemaType;
	}

	@Override
	public ApprovalSchema getApprovalSchema() {
		return new ApprovalSchema(getApprovalSchemaType());
	}

	@Override
    public I getItemToApprove() {
		return itemToApprove;
    }

    @Override
    public String toString() {
        return "ApprovalRequest: [itemToApprove=" + itemToApprove + ", approvalSchema=" + approvalSchemaType + "]";
    }

}
