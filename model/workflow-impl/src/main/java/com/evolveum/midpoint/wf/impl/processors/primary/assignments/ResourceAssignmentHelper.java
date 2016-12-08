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
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequestImpl;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.RelationResolver;
import com.evolveum.midpoint.wf.impl.processes.modifyAssignment.AssignmentModification;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspectHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author mederly
 */
@Component
public class ResourceAssignmentHelper {

    //private static final Trace LOGGER = TraceManager.getTrace(ResourceAssignmentHelper.class);

    @Autowired
    private PrimaryChangeAspectHelper primaryChangeAspectHelper;

    @Autowired
    private PrismContext prismContext;

    boolean isResourceAssignment(AssignmentType assignmentType) {
        return assignmentType.getConstruction() != null;
    }

    boolean shouldAssignmentBeApproved(PcpAspectConfigurationType config, ResourceType resourceType) {
        return primaryChangeAspectHelper.hasApproverInformation(config) ||
                (resourceType.getBusiness() != null && !resourceType.getBusiness().getApproverRef().isEmpty());
    }

    ApprovalRequest<AssignmentType> createApprovalRequest(PcpAspectConfigurationType config, AssignmentType assignmentType, ResourceType resourceType, RelationResolver relationResolver) {
        return new ApprovalRequestImpl<>(assignmentType, config, null, resourceType.getBusiness().getApproverRef(),
                null, null, prismContext, relationResolver);
    }

    ApprovalRequest<AssignmentModification> createApprovalRequestForModification(PcpAspectConfigurationType config, AssignmentType assignmentType, ResourceType resourceType, List<ItemDeltaType> modifications, RelationResolver relationResolver) {
        AssignmentModification itemToApprove = new AssignmentModification(assignmentType, resourceType, modifications);
        return new ApprovalRequestImpl<>(itemToApprove.wrap(prismContext), config, null,
                resourceType.getBusiness().getApproverRef(), null, null, prismContext, relationResolver);
    }

    ResourceType getAssignmentApprovalTarget(AssignmentType assignmentType, OperationResult result) {
        if (assignmentType.getConstruction() == null) {
            return null;
        }
        if (assignmentType.getConstruction().getResource() != null) {
            return assignmentType.getConstruction().getResource();
        }
        ObjectReferenceType resourceRef = assignmentType.getConstruction().getResourceRef();
        return primaryChangeAspectHelper.resolveTargetRef(resourceRef, ResourceType.class, result);
    }

    AssignmentType cloneAndCanonicalizeAssignment(AssignmentType assignmentType) {
        AssignmentType assignmentClone = assignmentType.clone();
        PrismContainerValue.copyDefinition(assignmentClone, assignmentType, prismContext);
        ConstructionType constructionType = assignmentClone.getConstruction();
        if (constructionType != null) {     // it should always be non-null
            constructionType.setResource(null);
        }
        return assignmentClone;
    }
}
