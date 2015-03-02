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

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.addrole.AddRoleVariableNames;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequestImpl;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalProcessInterface;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildJobCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.AbstractRoleAssignmentApprovalFormType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.QuestionFormType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.ResourceAssignmentApprovalFormType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Change aspect that manages resource assignment addition approval.
 * It starts one process instance for each resource assignment that has to be approved.
 *
 * @author mederly
 */
@Component
public class AddResourceAssignmentAspect extends BasePrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(AddResourceAssignmentAspect.class);

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ItemApprovalProcessInterface itemApprovalProcessInterface;

    //region ------------------------------------------------------------ Things that execute on request arrival

    @Override
    public List<PcpChildJobCreationInstruction> prepareJobCreationInstructions(ModelContext<?> modelContext, ObjectDelta<? extends ObjectType> change, Task taskFromModel, OperationResult result) throws SchemaException {

        List<ApprovalRequest<AssignmentType>> approvalRequestList = getApprovalRequests(modelContext, change, result);
        if (approvalRequestList == null || approvalRequestList.isEmpty()) {
            return null;
        }
        return prepareJobCreateInstructions(modelContext, taskFromModel, result, approvalRequestList);
    }

    private List<ApprovalRequest<AssignmentType>> getApprovalRequests(ModelContext<?> modelContext, ObjectDelta<? extends ObjectType> change, OperationResult result) {
        if (change.getChangeType() == ChangeType.ADD) {
            return getApprovalRequestsFromUserAdd(change, result);
        } else if (change.getChangeType() == ChangeType.MODIFY) {
            return getApprovalRequestsFromUserModify(modelContext.getFocusContext().getObjectOld(), change, result);
        }
        return null;
    }

    // we look for assignments of resources that should be approved
    private List<ApprovalRequest<AssignmentType>> getApprovalRequestsFromUserAdd(ObjectDelta<? extends ObjectType> change, OperationResult result) {
        List<ApprovalRequest<AssignmentType>> approvalRequestList = new ArrayList<>();
        UserType user = (UserType) change.getObjectToAdd().asObjectable();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resource-related assignments in user add delta ({}): ", user.getAssignment().size());
        }

        Iterator<AssignmentType> assignmentTypeIterator = user.getAssignment().iterator();
        while (assignmentTypeIterator.hasNext()) {
            AssignmentType assignmentType = assignmentTypeIterator.next();
            if (assignmentType.getConstruction() == null) {
                continue;
            }
            ConstructionType constructionType = assignmentType.getConstruction();
            ResourceType resourceType = (ResourceType) primaryChangeAspectHelper.resolveObjectRef(constructionType.getResourceRef(), result);
            if (resourceType == null) {
                continue;
            }
            boolean approvalRequired = shouldResourceBeApproved(resourceType);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(" - resource: {} (approval required = {}", resourceType, approvalRequired);
            }
            if (!approvalRequired) {
                continue;
            }
            AssignmentType assignmentClone = cloneAndCanonicalizeAssignment(assignmentType);
            approvalRequestList.add(createApprovalRequest(assignmentClone, resourceType));
            assignmentTypeIterator.remove();
        }
        return approvalRequestList;
    }

    // replaces prism object in targetRef by real reference
    // TODO make sure it's OK
    private AssignmentType cloneAndCanonicalizeAssignment(AssignmentType assignmentType) {
        AssignmentType assignmentClone = assignmentType.clone();
        PrismContainerValue.copyDefinition(assignmentClone, assignmentType);
        ConstructionType constructionType = assignmentClone.getConstruction();
        ObjectReferenceType resourceRef = constructionType.getResourceRef();
        resourceRef.setupReferenceValue(resourceRef.asReferenceValue().toCannonical());
        constructionType.setResource(null);
        constructionType.setResourceRef(resourceRef);
        return assignmentClone;
    }

    private List<ApprovalRequest<AssignmentType>> getApprovalRequestsFromUserModify(PrismObject<? extends ObjectType> userOld, ObjectDelta<? extends ObjectType> change, OperationResult result) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resource-related assignments in user modify delta: ");
        }

        List<ApprovalRequest<AssignmentType>> approvalRequestList = new ArrayList<>();
        Iterator<? extends ItemDelta> deltaIterator = change.getModifications().iterator();

        final ItemPath ASSIGNMENT_PATH = new ItemPath(UserType.F_ASSIGNMENT);

        while (deltaIterator.hasNext()) {
            ItemDelta delta = deltaIterator.next();
            if (!ASSIGNMENT_PATH.equivalent(delta.getPath())) {
                continue;
            }

            if (delta.getValuesToAdd() != null && !delta.getValuesToAdd().isEmpty()) {
                Iterator<PrismContainerValue<AssignmentType>> valueIterator = delta.getValuesToAdd().iterator();
                while (valueIterator.hasNext()) {
                    PrismContainerValue<AssignmentType> assignmentValue = valueIterator.next();
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Assignment to add = {}", assignmentValue.debugDump());
                    }
                    ApprovalRequest<AssignmentType> req = processAssignmentToAdd(assignmentValue, result);
                    if (req != null) {
                        approvalRequestList.add(req);
                        valueIterator.remove();
                    }
                }
            }
            if (delta.getValuesToReplace() != null && !delta.getValuesToReplace().isEmpty()) {
                Iterator<PrismContainerValue<AssignmentType>> valueIterator = delta.getValuesToReplace().iterator();
                while (valueIterator.hasNext()) {
                    PrismContainerValue<AssignmentType> assignmentValue = valueIterator.next();
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Assignment to replace = {}", assignmentValue.debugDump());
                    }
                    if (existsEquivalentValue(userOld, assignmentValue)) {
                        continue;
                    }
                    ApprovalRequest<AssignmentType> req = processAssignmentToAdd(assignmentValue, result);
                    if (req != null) {
                        approvalRequestList.add(req);
                        valueIterator.remove();
                    }
                }
            }
            // let's sanitize the delta
            if (delta.getValuesToAdd().isEmpty()) {         // empty set of values to add is an illegal state
                delta.resetValuesToAdd();
            }
            if (delta.getValuesToAdd() == null && delta.getValuesToReplace() == null && delta.getValuesToDelete() == null) {
                deltaIterator.remove();
            }
        }
        return approvalRequestList;
    }

    private boolean existsEquivalentValue(PrismObject<? extends ObjectType> userOld, PrismContainerValue<AssignmentType> assignmentValue) {
        UserType userType = (UserType) userOld.asObjectable();
        for (AssignmentType existing : userType.getAssignment()) {
            if (existing.asPrismContainerValue().equalsRealValue(assignmentValue)) {
                return true;
            }
        }
        return false;
    }

    private ApprovalRequest<AssignmentType> processAssignmentToAdd(PrismContainerValue<AssignmentType> assignmentCVal, OperationResult result) {
        if (assignmentCVal == null) {
            return null;
        }
        AssignmentType assignmentType = assignmentCVal.getValue();
        if (assignmentType.getConstruction() == null) {
            return null;
        }
        ConstructionType constructionType = assignmentType.getConstruction();
        ResourceType resourceType = (ResourceType) primaryChangeAspectHelper.resolveObjectRef(constructionType.getResourceRef(), result);
        if (resourceType == null) {
            return null;
        }
        boolean approvalRequired = shouldResourceBeApproved(resourceType);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(" - resource: {} (approval required = {}", resourceType, approvalRequired);
        }
        if (!approvalRequired) {
            return null;
        }
        AssignmentType assignmentClone = assignmentType.clone();
        PrismContainerValue.copyDefinition(assignmentClone, assignmentType);
        return createApprovalRequest(assignmentClone, resourceType);
    }

    // creates an approval request for a given resource assignment
    // @pre there are approvers defined
    private ApprovalRequest<AssignmentType> createApprovalRequest(AssignmentType a, ResourceType resourceType) {
        return new ApprovalRequestImpl(a, null, resourceType.getBusiness().getApproverRef(), null, null, prismContext);
    }

    // @pre there is a construction with a resource ref
    private List<PcpChildJobCreationInstruction> prepareJobCreateInstructions(ModelContext<?> modelContext, Task taskFromModel, OperationResult result, List<ApprovalRequest<AssignmentType>> approvalRequestList) throws SchemaException {
        List<PcpChildJobCreationInstruction> instructions = new ArrayList<>();

        String userName = MiscDataUtil.getFocusObjectName(modelContext);

        for (ApprovalRequest<AssignmentType> approvalRequest : approvalRequestList) {

            assert approvalRequest.getPrismContext() != null;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Approval request = {}", approvalRequest);
            }

            AssignmentType assignmentType = approvalRequest.getItemToApprove();
            ResourceType resourceType = (ResourceType) primaryChangeAspectHelper.resolveObjectRef(assignmentType.getConstruction().getResourceRef(), result);

            String resourceName = resourceType.getName().getOrig();

            String objectOid = primaryChangeAspectHelper.getObjectOid(modelContext);
            PrismObject<UserType> requester = primaryChangeAspectHelper.getRequester(taskFromModel, result);

            // create a JobCreateInstruction for a given change processor (primaryChangeProcessor in this case)
            PcpChildJobCreationInstruction instruction =
                    PcpChildJobCreationInstruction.createInstruction(getChangeProcessor());

            // set some common task/process attributes
            instruction.prepareCommonAttributes(this, modelContext, objectOid, requester);

            // prepare and set the delta that has to be approved
            ObjectDelta<? extends ObjectType> delta = assignmentToDelta(modelContext, approvalRequest, objectOid);
            instruction.setDeltaProcessAndTaskVariables(delta);

            // set the names of midPoint task and activiti process instance
            String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and executing " : "";
            instruction.setTaskName("Workflow for approving " + andExecuting + "assignment of " + resourceName + " to " + userName);
            instruction.setProcessInstanceName("Adding assignment of " + resourceName + " to " + userName);

            // setup general item approval process
            String approvalTaskName = "Approve adding assignment of " + resourceName + " to " + userName;
            itemApprovalProcessInterface.prepareStartInstruction(instruction, approvalRequest, approvalTaskName);

            // set some aspect-specific variables
            instruction.addProcessVariable(AddRoleVariableNames.USER_NAME, userName);

            instructions.add(instruction);
        }
        return instructions;
    }

    private ObjectDelta<? extends ObjectType> assignmentToDelta(ModelContext<?> modelContext, ApprovalRequest<AssignmentType> approvalRequest, String objectOid) {
        PrismObject<UserType> user = (PrismObject<UserType>) modelContext.getFocusContext().getObjectNew();
        PrismContainerDefinition<AssignmentType> prismContainerDefinition = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);

        ItemDelta<PrismContainerValue<AssignmentType>> addDelta = new ContainerDelta<>(new ItemPath(), UserType.F_ASSIGNMENT, prismContainerDefinition, prismContext);
        PrismContainerValue<AssignmentType> assignmentValue = approvalRequest.getItemToApprove().asPrismContainerValue().clone();
        addDelta.addValueToAdd(assignmentValue);

        return ObjectDelta.createModifyDelta(objectOid != null ? objectOid : PrimaryChangeProcessor.UNKNOWN_OID, addDelta, UserType.class, ((LensContext) modelContext).getPrismContext());
    }

    public static boolean shouldResourceBeApproved(ResourceType resourceType) {
        return resourceType != null && resourceType.getBusiness() != null && !resourceType.getBusiness().getApproverRef().isEmpty();
    }
    //endregion

    //region ------------------------------------------------------------ Things that execute when item is being approved

    @Override
    public PrismObject<? extends QuestionFormType> prepareQuestionForm(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("prepareQuestionForm starting: execution id " + task.getExecutionId() + ", pid " + task.getProcessInstanceId() + ", variables = " + variables);
        }

        PrismObjectDefinition<ResourceAssignmentApprovalFormType> formDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(ResourceAssignmentApprovalFormType.COMPLEX_TYPE);
        PrismObject<ResourceAssignmentApprovalFormType> formPrism = formDefinition.instantiate();
        ResourceAssignmentApprovalFormType form = formPrism.asObjectable();

        form.setUser((String) variables.get(AddRoleVariableNames.USER_NAME));

        // todo check type compatibility
        ApprovalRequest request = (ApprovalRequest) variables.get(ProcessVariableNames.APPROVAL_REQUEST);
        request.setPrismContext(prismContext);
        Validate.notNull(request, "Approval request is not present among process variables");

        AssignmentType assignment = (AssignmentType) request.getItemToApprove();
        Validate.notNull(assignment, "Approval request does not contain as assignment");

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

        form.setResource(resourceType.getName() == null ? resourceType.getOid() : resourceType.getName().getOrig());        // ==null should not occur
        form.setRequesterComment(assignment.getDescription());
        form.setTimeInterval(AddRoleAssignmentAspect.formatTimeIntervalBrief(assignment));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resulting prism object instance = " + formPrism.debugDump());
        }
        return formPrism;
    }

    @Override
    public PrismObject<? extends ObjectType> prepareRelatedObject(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {

        ApprovalRequest<AssignmentType> approvalRequest = (ApprovalRequest<AssignmentType>) variables.get(ProcessVariableNames.APPROVAL_REQUEST);
        approvalRequest.setPrismContext(prismContext);
        if (approvalRequest == null) {
            throw new IllegalStateException("No approval request in activiti task " + task);
        }

        String oid = approvalRequest.getItemToApprove().getConstruction().getResourceRef().getOid();
        return repositoryService.getObject(ResourceType.class, oid, null, result);
    }

    //endregion
}