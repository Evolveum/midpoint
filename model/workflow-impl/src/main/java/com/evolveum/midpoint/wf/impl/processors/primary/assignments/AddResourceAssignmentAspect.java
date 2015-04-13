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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PcpAspectConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Generic change aspect for adding a resource assignment to (any) focus type.
 *
 * @author mederly
 */
@Component
public abstract class AddResourceAssignmentAspect<F extends FocusType> extends AddAssignmentAspect<ResourceType, F> {

    private static final Trace LOGGER = TraceManager.getTrace(AddResourceAssignmentAspect.class);

    @Autowired
    protected ResourceAssignmentHelper specificAssignmentHelper;

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    protected boolean isAssignmentRelevant(AssignmentType assignmentType) {
        return specificAssignmentHelper.isResourceAssignment(assignmentType);
    }

    @Override
    protected boolean shouldAssignmentBeApproved(PcpAspectConfigurationType config, ResourceType resourceType) {
        return specificAssignmentHelper.shouldAssignmentBeApproved(config, resourceType);
    }

    @Override
    protected ApprovalRequest<AssignmentType> createApprovalRequest(PcpAspectConfigurationType config, AssignmentType assignmentType, ResourceType resourceType) {
        return specificAssignmentHelper.createApprovalRequest(config, assignmentType, resourceType);
    }

    @Override
    protected ResourceType getAssignmentApprovalTarget(AssignmentType assignmentType, OperationResult result) {
        return specificAssignmentHelper.getAssignmentApprovalTarget(assignmentType, result);
    }

    @Override
    protected AssignmentType cloneAndCanonicalizeAssignment(AssignmentType assignmentType) {
        return specificAssignmentHelper.cloneAndCanonicalizeAssignment(assignmentType);
    }
}

