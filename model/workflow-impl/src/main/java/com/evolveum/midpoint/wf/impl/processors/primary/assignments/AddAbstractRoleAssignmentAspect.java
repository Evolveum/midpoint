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

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PcpAspectConfigurationType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Generic change aspect for adding an abstract role assignment to (any) focus type.
 *
 * @author mederly
 */
public abstract class AddAbstractRoleAssignmentAspect<F extends FocusType> extends AddAssignmentAspect<AbstractRoleType,F> {

    private static final Trace LOGGER = TraceManager.getTrace(AddAbstractRoleAssignmentAspect.class);

    @Autowired
    protected RoleAssignmentHelper specificAssignmentHelper;

    @Override
    public boolean isAssignmentRelevant(AssignmentType assignmentType) {
        return specificAssignmentHelper.isAssignmentRelevant(assignmentType);
    }

    @Override
    public boolean shouldAssignmentBeApproved(PcpAspectConfigurationType config, AbstractRoleType role) {
        return specificAssignmentHelper.shouldAssignmentBeApproved(config, role);
    }

    @Override
    public ApprovalRequest<AssignmentType> createApprovalRequest(PcpAspectConfigurationType config, AssignmentType a,
			AbstractRoleType role, ModelContext<?> modelContext, Task taskFromModel, OperationResult result) {
        return specificAssignmentHelper.createApprovalRequest(config, a, role, createRelationResolver(role, result),
                createReferenceResolver(modelContext, taskFromModel, result));
    }

    @Override
    public AssignmentType cloneAndCanonicalizeAssignment(AssignmentType assignmentType) {
        return specificAssignmentHelper.cloneAndCanonicalizeAssignment(assignmentType);
    }

    @Override
    public AbstractRoleType getAssignmentApprovalTarget(AssignmentType assignmentType, OperationResult result) {
        return specificAssignmentHelper.getAssignmentApprovalTarget(assignmentType, result);
    }

    @Override
    protected String getTargetDisplayName(AbstractRoleType target) {
        return specificAssignmentHelper.getTargetDisplayName(target);
    }
}
