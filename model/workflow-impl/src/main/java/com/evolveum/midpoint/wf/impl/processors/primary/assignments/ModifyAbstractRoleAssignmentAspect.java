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
import com.evolveum.midpoint.wf.impl.processes.modifyAssignment.AssignmentModification;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PcpAspectConfigurationType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Generic change aspect for modifying an abstract role assignment to (any) focus type.
 *
 * @author mederly
 */
public abstract class ModifyAbstractRoleAssignmentAspect<F extends FocusType> extends ModifyAssignmentAspect<AbstractRoleType,F> {

    private static final Trace LOGGER = TraceManager.getTrace(ModifyAbstractRoleAssignmentAspect.class);

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
    public ApprovalRequest<AssignmentModification> createApprovalRequestForModification(PcpAspectConfigurationType config, AssignmentType assignmentType, AbstractRoleType role, List<ItemDeltaType> modifications) {
        return specificAssignmentHelper.createApprovalRequestForModification(config, assignmentType, role, modifications);
    }

    @Override
    public AssignmentType cloneAndCanonicalizeAssignment(AssignmentType assignmentType) {
        return specificAssignmentHelper.cloneAndCanonicalizeAssignment(assignmentType);
    }

    @Override
    public AbstractRoleType getAssignmentApprovalTarget(AssignmentType assignmentType, OperationResult result) {
        return specificAssignmentHelper.getAssignmentApprovalTarget(assignmentType, result);
    }
}