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

package com.evolveum.midpoint.wf.impl.processors.primary.user;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequestImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PcpAspectConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.AbstractRoleAssignmentApprovalFormType;
import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;

/**
 * Change aspect that manages role addition approval. It starts one process instance for each role
 * that has to be approved.
 *
 * In the past, we used to start one process instance for ALL roles to be approved. It made BPMN
 * approval process slightly more complex, while allowed to keep information about approval process
 * centralized from the user point of view (available via "single click"). If necessary, we can return
 * to this behavior.
 *
 * Alternatively, it is possible to start one process instance for a set of roles that share the
 * same approval mechanism. However, it is questionable what "the same approval mechanism" means,
 * for example, if there are expressions used to select an approver.
 *
 * @author mederly
 */
@Component
public class AddRoleAssignmentAspect extends AbstractAddAssignmentAspect<AbstractRoleType> {

    private static final Trace LOGGER = TraceManager.getTrace(AddRoleAssignmentAspect.class);

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
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

    @Override
    protected boolean shouldAssignmentBeApproved(PcpAspectConfigurationType config, AbstractRoleType role) {
        return primaryChangeAspectHelper.hasApproverInformation(config) ||
                !role.getApproverRef().isEmpty() || !role.getApproverExpression().isEmpty() || role.getApprovalSchema() != null;
    }

    @Override
    public ApprovalRequest<AssignmentType> createApprovalRequest(PcpAspectConfigurationType config, AssignmentType a, AbstractRoleType role) {
        return new ApprovalRequestImpl<>(a, config, role.getApprovalSchema(), role.getApproverRef(), role.getApproverExpression(), role.getAutomaticallyApproved(), prismContext);
    }

    // TODO is this ok?
    @Override
    protected AssignmentType cloneAndCanonicalizeAssignment(AssignmentType assignmentType) {
        AssignmentType assignmentClone = assignmentType.clone();
        PrismContainerValue.copyDefinition(assignmentClone, assignmentType);

        assignmentClone.setTarget(null);
        return assignmentClone;
    }

    @Override
    protected AbstractRoleType getAssignmentApprovalTarget(AssignmentType assignmentType, OperationResult result) {
        return primaryChangeAspectHelper.resolveTargetRef(assignmentType, result);
    }

    @Override
    protected PrismObject<AbstractRoleAssignmentApprovalFormType> createSpecificQuestionForm(AssignmentType assignment, String userName, OperationResult result) throws ObjectNotFoundException, SchemaException {
        ObjectReferenceType targetRef = assignment.getTargetRef();
        Validate.notNull(targetRef, "Approval request does not contain assignment target reference");
        String targetOid = targetRef.getOid();
        Validate.notNull(targetOid, "Approval request does not contain assignment target OID");

        AbstractRoleType target;
        try {
            target = primaryChangeAspectHelper.resolveTargetRefUnchecked(assignment, result);
        } catch (ObjectNotFoundException e) {
            throw new ObjectNotFoundException("Assignment target with OID " + targetOid + " does not exist anymore.", e);
        } catch (SchemaException e) {
            throw new SchemaException("Couldn't get assignment target with OID " + targetOid + " because of schema exception.", e);
        }

        PrismObjectDefinition<AbstractRoleAssignmentApprovalFormType> formDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(AbstractRoleAssignmentApprovalFormType.COMPLEX_TYPE);
        PrismObject<AbstractRoleAssignmentApprovalFormType> formPrism = formDefinition.instantiate();
        AbstractRoleAssignmentApprovalFormType form = formPrism.asObjectable();

        form.setUser(userName);
        form.setRole(target.getName() == null ? target.getOid() : target.getName().getOrig());        // ==null should not occur
        form.setRequesterComment(assignment.getDescription());
        form.setTimeInterval(formatTimeIntervalBrief(assignment));
        return formPrism;
    }

}