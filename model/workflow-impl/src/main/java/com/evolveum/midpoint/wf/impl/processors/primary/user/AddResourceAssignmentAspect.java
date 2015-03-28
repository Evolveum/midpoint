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

package com.evolveum.midpoint.wf.impl.processors.primary.user;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequestImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PcpAspectConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.ResourceAssignmentApprovalFormType;
import org.apache.commons.lang.Validate;
import org.springframework.stereotype.Component;

/**
 * Change aspect that manages resource assignment addition approval.
 * It starts one process instance for each resource assignment that has to be approved.
 *
 * @author mederly
 */
@Component
public class AddResourceAssignmentAspect extends AbstractAddAssignmentAspect<ResourceType> {

    private static final Trace LOGGER = TraceManager.getTrace(AddResourceAssignmentAspect.class);

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    protected boolean isAssignmentRelevant(AssignmentType assignmentType) {
        return assignmentType.getConstruction() != null;
    }

    @Override
    protected boolean shouldAssignmentBeApproved(PcpAspectConfigurationType config, ResourceType resourceType) {
        return primaryChangeAspectHelper.hasApproverInformation(config) ||
                (resourceType.getBusiness() != null && !resourceType.getBusiness().getApproverRef().isEmpty());
    }

    @Override
    protected ApprovalRequest<AssignmentType> createApprovalRequest(PcpAspectConfigurationType config, AssignmentType assignmentType, ResourceType resourceType) {
        return new ApprovalRequestImpl<>(assignmentType, config, null, resourceType.getBusiness().getApproverRef(), null, null, prismContext);
    }

    @Override
    protected ResourceType getAssignmentApprovalTarget(AssignmentType assignmentType, OperationResult result) {
        if (assignmentType.getConstruction() == null) {
            return null;
        }
        if (assignmentType.getConstruction().getResource() != null) {
            return assignmentType.getConstruction().getResource();
        }
        ObjectReferenceType resourceRef = assignmentType.getConstruction().getResourceRef();
        return primaryChangeAspectHelper.resolveTargetRef(resourceRef, ResourceType.class, result);
    }

    @Override
    protected PrismObject<ResourceAssignmentApprovalFormType> createSpecificQuestionForm(AssignmentType assignment, String userName, OperationResult result) throws ObjectNotFoundException, SchemaException {

        PrismObjectDefinition<ResourceAssignmentApprovalFormType> formDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(ResourceAssignmentApprovalFormType.COMPLEX_TYPE);
        PrismObject<ResourceAssignmentApprovalFormType> formPrism = formDefinition.instantiate();
        ResourceAssignmentApprovalFormType form = formPrism.asObjectable();

        ConstructionType constructionType = assignment.getConstruction();
        Validate.notNull(constructionType, "Approval request does not contain a construction");
        ObjectReferenceType resourceRef = constructionType.getResourceRef();
        Validate.notNull(resourceRef, "Approval request does not contain resource reference");
        String resourceOid = resourceRef.getOid();
        Validate.notNull(resourceOid, "Approval request does not contain resource OID");

        ResourceType resourceType;
        try {
            resourceType = repositoryService.getObject(ResourceType.class, resourceOid, null, result).asObjectable();
        } catch (ObjectNotFoundException e) {
            throw new ObjectNotFoundException("Resource with OID " + resourceOid + " does not exist anymore.");
        } catch (SchemaException e) {
            throw new SchemaException("Couldn't get resource with OID " + resourceOid + " because of schema exception.");
        }
        form.setUser(userName);

        form.setResource(resourceType.getName() == null ? resourceType.getOid() : resourceType.getName().getOrig());        // ==null should not occur
        form.setRequesterComment(assignment.getDescription());
        form.setTimeInterval(AddRoleAssignmentAspect.formatTimeIntervalBrief(assignment));
        return form.asPrismObject();
    }

    // replaces prism object in targetRef by real reference
    // TODO make sure it's OK
    protected AssignmentType cloneAndCanonicalizeAssignment(AssignmentType assignmentType) {
        AssignmentType assignmentClone = assignmentType.clone();
        PrismContainerValue.copyDefinition(assignmentClone, assignmentType);
        ConstructionType constructionType = assignmentClone.getConstruction();
        if (constructionType != null) {     // it should always be non-null
            constructionType.setResource(null);
        }
        return assignmentClone;
    }
}

