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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.QuestionFormType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.RoleApprovalFormType;

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
public class AddRoleAssignmentAspect extends BasePrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(AddRoleAssignmentAspect.class);

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ItemApprovalProcessInterface itemApprovalProcessInterface;

    //region ------------------------------------------------------------ Things that execute on request arrival

    @Override
    public List<PcpChildJobCreationInstruction> prepareJobCreationInstructions(ModelContext<?> modelContext, ObjectDelta<? extends ObjectType> change, Task taskFromModel, OperationResult result) throws SchemaException {

        List<ApprovalRequest<AssignmentType>> approvalRequestList = getAssignmentToApproveList(change, result);
        if (approvalRequestList == null) {
            return null;
        } else {
            return prepareJobCreateInstructions(modelContext, taskFromModel, result, approvalRequestList);
        }
    }

    private List<ApprovalRequest<AssignmentType>> getAssignmentToApproveList(ObjectDelta<? extends ObjectType> change, OperationResult result) {

        List<ApprovalRequest<AssignmentType>> approvalRequestList = new ArrayList<ApprovalRequest<AssignmentType>>();

        /*
         * We either add a user; then the list of roles to be added is given by the assignment property,
         * or we modify a user; then the list of roles is given by the assignment property modification.
         */

        if (change.getChangeType() == ChangeType.ADD) {

            PrismObject<?> prismToAdd = change.getObjectToAdd();
            UserType user = (UserType) prismToAdd.asObjectable();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Role-related assignments in user add delta (" + user.getAssignment().size() + "): ");
            }

            // in the following we look for assignments of roles that should be approved

            Iterator<AssignmentType> assignmentTypeIterator = user.getAssignment().iterator();
            while (assignmentTypeIterator.hasNext()) {
                AssignmentType a = assignmentTypeIterator.next();
                ObjectType objectType = primaryChangeAspectHelper.resolveObjectRef(a, result);
                if (objectType != null && objectType instanceof RoleType) {         // later might be changed to AbstractRoleType but we should do this change at all places in this aspect
                    RoleType role = (RoleType) objectType;
                    boolean approvalRequired = shouldRoleBeApproved(role);
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace(" - role: " + role + " (approval required = " + approvalRequired + ")");
                    }
                    if (approvalRequired) {
                        AssignmentType aCopy = a.clone();
                        PrismContainerValue.copyDefinition(aCopy, a);
                        aCopy.setTarget(role);
                        approvalRequestList.add(createApprovalRequest(aCopy, role));
                        assignmentTypeIterator.remove();
                    }
                }
            }

        } else if (change.getChangeType() == ChangeType.MODIFY) {

            Iterator<? extends ItemDelta> deltaIterator = change.getModifications().iterator();

            // are any 'sensitive' roles assigned?

            while (deltaIterator.hasNext()) {
                ItemDelta delta = deltaIterator.next();
                if (UserType.F_ASSIGNMENT.equals(delta.getElementName()) && delta.getValuesToAdd() != null && !delta.getValuesToAdd().isEmpty()) {          // todo: what if assignments are modified?
                    Iterator valueIterator = delta.getValuesToAdd().iterator();
                    while (valueIterator.hasNext()) {
                        Object o = valueIterator.next();
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Assignment to add = " + ((PrismContainerValue) o).debugDump());
                        }
                        PrismContainerValue<AssignmentType> at = (PrismContainerValue<AssignmentType>) o;
                        ObjectType objectType = primaryChangeAspectHelper.resolveObjectRef(at.getValue(), result);
                        if (objectType != null && objectType instanceof RoleType) {
                            RoleType role = (RoleType) objectType;
                            boolean approvalRequired = shouldRoleBeApproved(role);
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace(" - role: " + role + " (approval required = " + approvalRequired + ")");
                            }
                            if (approvalRequired) {
                                AssignmentType aCopy = at.asContainerable().clone();
                                PrismContainerValue.copyDefinition(aCopy, at.asContainerable());
                                approvalRequestList.add(createApprovalRequest(aCopy, role));
                                valueIterator.remove();
                            }
                        }
                    }
                    if (delta.getValuesToAdd().isEmpty()) {         // empty set of values to add is an illegal state
                        delta.resetValuesToAdd();
                        if (delta.getValuesToReplace() == null && delta.getValuesToDelete() == null) {
                            deltaIterator.remove();
                        }
                    }
                }
            }
        } else {
            return null;
        }
        return approvalRequestList;
    }

    // creates an approval request for a given role assignment
    private ApprovalRequest<AssignmentType> createApprovalRequest(AssignmentType a, AbstractRoleType role) {
        return new ApprovalRequestImpl(a, role.getApprovalSchema(), role.getApproverRef(), role.getApproverExpression(), role.getAutomaticallyApproved(), prismContext);
    }

    // approvalRequestList should contain de-referenced roles and approvalRequests that have prismContext set
    private List<PcpChildJobCreationInstruction> prepareJobCreateInstructions(ModelContext<?> modelContext, Task taskFromModel, OperationResult result, List<ApprovalRequest<AssignmentType>> approvalRequestList) throws SchemaException {
        List<PcpChildJobCreationInstruction> instructions = new ArrayList<>();

        String userName = MiscDataUtil.getFocusObjectName(modelContext);

        for (ApprovalRequest<AssignmentType> approvalRequest : approvalRequestList) {

            assert(approvalRequest.getPrismContext() != null);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Approval request = " + approvalRequest);
            }

            AssignmentType assignmentType = approvalRequest.getItemToApprove();
            RoleType roleType = (RoleType) assignmentType.getTarget();

            assignmentType.setTarget(null);         // we must remove the target object (leaving only target OID) in order to avoid problems with deserialization
            ((ApprovalRequestImpl<AssignmentType>) approvalRequest).setItemToApprove(assignmentType);   // set the modified value back

            Validate.notNull(roleType);
            Validate.notNull(roleType.getName());
            String roleName = roleType.getName().getOrig();

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
            instruction.setTaskName("Workflow for approving " + andExecuting + "adding " + roleName + " to " + userName);
            instruction.setProcessInstanceName("Adding " + roleName + " to " + userName);

            // setup general item approval process
            String approvalTaskName = "Approve adding " + roleName + " to " + userName;
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

        ItemDelta<PrismContainerValue<AssignmentType>> addRoleDelta = new ContainerDelta<>(new ItemPath(), UserType.F_ASSIGNMENT, prismContainerDefinition, prismContext);
        PrismContainerValue<AssignmentType> assignmentValue = approvalRequest.getItemToApprove().asPrismContainerValue().clone();
        addRoleDelta.addValueToAdd(assignmentValue);

        return ObjectDelta.createModifyDelta(objectOid != null ? objectOid : PrimaryChangeProcessor.UNKNOWN_OID, addRoleDelta, UserType.class, ((LensContext) modelContext).getPrismContext());
    }

    private boolean shouldRoleBeApproved(AbstractRoleType role) {
        return !role.getApproverRef().isEmpty() || !role.getApproverExpression().isEmpty() || role.getApprovalSchema() != null;
    }
    //endregion

    //region ------------------------------------------------------------ Things that execute when item is being approved

    @Override
    public PrismObject<? extends QuestionFormType> prepareQuestionForm(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("getRequestSpecific starting: execution id " + task.getExecutionId() + ", pid " + task.getProcessInstanceId() + ", variables = " + variables);
        }

        PrismObjectDefinition<RoleApprovalFormType> formDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(RoleApprovalFormType.COMPLEX_TYPE);
        PrismObject<RoleApprovalFormType> formPrism = formDefinition.instantiate();
        RoleApprovalFormType form = formPrism.asObjectable();

        form.setUser((String) variables.get(AddRoleVariableNames.USER_NAME));

        // todo check type compatibility
        ApprovalRequest request = (ApprovalRequest) variables.get(ProcessVariableNames.APPROVAL_REQUEST);
        request.setPrismContext(prismContext);
        Validate.notNull(request, "Approval request is not present among process variables");

        AssignmentType assignment = (AssignmentType) request.getItemToApprove();
        Validate.notNull(assignment, "Approval request does not contain as assignment");

        ObjectReferenceType roleRef = assignment.getTargetRef();
        Validate.notNull(roleRef, "Approval request does not contain role reference");
        String roleOid = roleRef.getOid();
        Validate.notNull(roleOid, "Approval request does not contain role OID");

        RoleType role;
        try {
            role = repositoryService.getObject(RoleType.class, roleOid, null, result).asObjectable();
        } catch (ObjectNotFoundException e) {
            throw new ObjectNotFoundException("Role with OID " + roleOid + " does not exist anymore.");
        } catch (SchemaException e) {
            throw new SchemaException("Couldn't get role with OID " + roleOid + " because of schema exception.");
        }

        form.setRole(role.getName() == null ? role.getOid() : role.getName().getOrig());        // ==null should not occur
        form.setRequesterComment(assignment.getDescription());
        form.setTimeInterval(formatTimeIntervalBrief(assignment));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resulting prism object instance = " + formPrism.debugDump());
        }
        return formPrism;
    }

    public static String formatTimeIntervalBrief(AssignmentType assignment) {
        StringBuilder sb = new StringBuilder();
        if (assignment != null && assignment.getActivation() != null &&
                (assignment.getActivation().getValidFrom() != null || assignment.getActivation().getValidTo() != null)) {
            if (assignment.getActivation().getValidFrom() != null && assignment.getActivation().getValidTo() != null) {
                sb.append(formatTime(assignment.getActivation().getValidFrom()));
                sb.append("-");
                sb.append(formatTime(assignment.getActivation().getValidTo()));
            } else if (assignment.getActivation().getValidFrom() != null) {
                sb.append("from ");
                sb.append(formatTime(assignment.getActivation().getValidFrom()));
            } else {
                sb.append("to ");
                sb.append(formatTime(assignment.getActivation().getValidTo()));
            }
        }
        return sb.toString();
    }

    private static String formatTime(XMLGregorianCalendar time) {
        DateFormat formatter = DateFormat.getDateInstance();
        return formatter.format(time.toGregorianCalendar().getTime());
    }

    @Override
    public PrismObject<? extends ObjectType> prepareRelatedObject(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {

        ApprovalRequest<AssignmentType> approvalRequest = (ApprovalRequest<AssignmentType>) variables.get(ProcessVariableNames.APPROVAL_REQUEST);
        approvalRequest.setPrismContext(prismContext);
        if (approvalRequest == null) {
            throw new IllegalStateException("No approval request in activiti task " + task);
        }

        String oid = approvalRequest.getItemToApprove().getTargetRef().getOid();
        return repositoryService.getObject(RoleType.class, oid, null, result);
    }


    //endregion
}