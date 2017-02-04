/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.wf.impl.processors.primary.assignments;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.*;
import com.evolveum.midpoint.wf.impl.processes.modifyAssignment.AssignmentModification;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspectHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PcpAspectConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class RoleAssignmentHelper {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAssignmentHelper.class);

    @Autowired
    private PrimaryChangeAspectHelper primaryChangeAspectHelper;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ApprovalSchemaHelper approvalSchemaHelper;

    protected boolean isAssignmentRelevant(AssignmentType assignmentType) {
        if (assignmentType.getTarget() != null) {
            return assignmentType.getTarget() instanceof AbstractRoleType;
        } else if (assignmentType.getTargetRef() != null) {
            QName targetType = assignmentType.getTargetRef().getType();
            return QNameUtil.match(targetType, RoleType.COMPLEX_TYPE) ||
                    QNameUtil.match(targetType, OrgType.COMPLEX_TYPE) ||
                    QNameUtil.match(targetType, AbstractRoleType.COMPLEX_TYPE);         // this case should not occur
        } else {
            return false;       // should not occur
        }
    }

    boolean shouldAssignmentBeApproved(PcpAspectConfigurationType config, AbstractRoleType role) {
        return primaryChangeAspectHelper.hasApproverInformation(config) ||
                !role.getApproverRef().isEmpty() || !role.getApproverExpression().isEmpty() || role.getApprovalSchema() != null;
    }

    ApprovalRequest<AssignmentType> createApprovalRequest(PcpAspectConfigurationType config, AssignmentType a,
			AbstractRoleType role, RelationResolver relationResolver, ReferenceResolver referenceResolver) {
        ApprovalRequest<AssignmentType> request = new ApprovalRequestImpl<>(a, config, role.getApprovalSchema(), role.getApproverRef(),
                role.getApproverExpression(), role.getAutomaticallyApproved(), prismContext);
        approvalSchemaHelper.prepareSchema(request.getApprovalSchemaType(), relationResolver, referenceResolver);
        return request;
    }

    ApprovalRequest<AssignmentModification> createApprovalRequestForModification(PcpAspectConfigurationType config,
			AssignmentType assignmentType, AbstractRoleType role, List<ItemDeltaType> modifications,
			RelationResolver relationResolver, ReferenceResolver referenceResolver) {
        AssignmentModification itemToApprove = new AssignmentModification(assignmentType, role, modifications);
        ApprovalRequest<AssignmentModification> request = new ApprovalRequestImpl<>(itemToApprove, config, role.getApprovalSchema(),
				role.getApproverRef(), role.getApproverExpression(), role.getAutomaticallyApproved(), prismContext);
		approvalSchemaHelper.prepareSchema(request.getApprovalSchemaType(), relationResolver, referenceResolver);
		return request;
	}

    // TODO is this ok?
	AssignmentType cloneAndCanonicalizeAssignment(AssignmentType assignmentType) {
        AssignmentType assignmentClone = assignmentType.clone();
        PrismContainerValue.copyDefinition(assignmentClone, assignmentType, prismContext);

        assignmentClone.setTarget(null);
        return assignmentClone;
    }

    AbstractRoleType getAssignmentApprovalTarget(AssignmentType assignmentType, OperationResult result) {
        return primaryChangeAspectHelper.resolveTargetRef(assignmentType, result);
    }

    String getTargetDisplayName(AbstractRoleType target) {
        String simpleNameOrOid;
        if (target.getName() != null) {
            simpleNameOrOid = target.getName().getOrig();
        } else {
            simpleNameOrOid = target.getOid();
        }

        if (target instanceof RoleType) {
            return "role " + simpleNameOrOid;
        } else if (target instanceof OrgType) {
            return "org " + simpleNameOrOid;
        } else {
            return simpleNameOrOid;       // should not occur
        }
    }
}
