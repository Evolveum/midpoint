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
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalProcessInterface;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildJobCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PcpAspectConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.AssignmentCreationApprovalFormType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.QuestionFormType;
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
 * Aspect for adding assignments of any type (abstract role or resource).
 *
 * T is type of the objects being assigned (AbstractRoleType, ResourceType).
 * F is the type of the objects to which assignments are made (UserType, AbstractRoleType).
 *
 * @author mederly
 */
@Component
public abstract class AddAssignmentAspect<T extends ObjectType, F extends FocusType> extends BasePrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(AddAssignmentAspect.class);

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected ItemApprovalProcessInterface itemApprovalProcessInterface;

    @Autowired
    protected AssignmentHelper assignmentHelper;

    //region ------------------------------------------------------------ Things that execute on request arrival

    @Override
    public List<PcpChildJobCreationInstruction> prepareJobCreationInstructions(ModelContext<?> modelContext, WfConfigurationType wfConfigurationType, ObjectDelta<? extends ObjectType> change, Task taskFromModel, OperationResult result) throws SchemaException {
        if (!isFocusRelevant(modelContext)) {
            return null;
        }
        List<ApprovalRequest<AssignmentType>> approvalRequestList = getApprovalRequests(modelContext, wfConfigurationType, change, result);
        if (approvalRequestList == null || approvalRequestList.isEmpty()) {
            return null;
        }
        return prepareJobCreateInstructions(modelContext, taskFromModel, result, approvalRequestList);
    }

    private List<ApprovalRequest<AssignmentType>> getApprovalRequests(ModelContext<?> modelContext, WfConfigurationType wfConfigurationType, ObjectDelta<? extends ObjectType> change, OperationResult result) {
        if (change.getChangeType() != ChangeType.ADD && change.getChangeType() != ChangeType.MODIFY) {
            return null;
        }
        PcpAspectConfigurationType config = primaryChangeAspectHelper.getPcpAspectConfigurationType(wfConfigurationType, this);
        if (change.getChangeType() == ChangeType.ADD) {
            return getApprovalRequestsFromFocusAdd(config, change, result);
        } else {
            return getApprovalRequestsFromFocusModify(config, modelContext.getFocusContext().getObjectOld(), change, result);
        }
    }

    private List<ApprovalRequest<AssignmentType>> getApprovalRequestsFromFocusAdd(PcpAspectConfigurationType config, ObjectDelta<? extends ObjectType> change, OperationResult result) {
        LOGGER.trace("Relevant assignments in focus add delta:");

        List<ApprovalRequest<AssignmentType>> approvalRequestList = new ArrayList<>();
        FocusType focusType = (FocusType) change.getObjectToAdd().asObjectable();
        Iterator<AssignmentType> assignmentTypeIterator = focusType.getAssignment().iterator();
        while (assignmentTypeIterator.hasNext()) {
            AssignmentType a = assignmentTypeIterator.next();
            if (isAssignmentRelevant(a)) {
                T specificObjectType = getAssignmentApprovalTarget(a, result);
                boolean approvalRequired = shouldAssignmentBeApproved(config, specificObjectType);
                LOGGER.trace(" - {} (approval required = {})", specificObjectType, approvalRequired);
                if (approvalRequired) {
                    AssignmentType aCopy = cloneAndCanonicalizeAssignment(a);
                    approvalRequestList.add(createApprovalRequest(config, aCopy, specificObjectType));
                    assignmentTypeIterator.remove();
                }
            }
        }
        return approvalRequestList;
    }

    private List<ApprovalRequest<AssignmentType>> getApprovalRequestsFromFocusModify(PcpAspectConfigurationType config,
                                                                                     PrismObject<? extends ObjectType> focusOld,
                                                                                     ObjectDelta<? extends ObjectType> change, OperationResult result) {
        LOGGER.trace("Relevant assignments in focus modify delta:");

        List<ApprovalRequest<AssignmentType>> approvalRequestList = new ArrayList<>();
        Iterator<? extends ItemDelta> deltaIterator = change.getModifications().iterator();

        final ItemPath ASSIGNMENT_PATH = new ItemPath(FocusType.F_ASSIGNMENT);

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
                    ApprovalRequest<AssignmentType> req = processAssignmentToAdd(config, assignmentValue, result);
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
                    if (existsEquivalentValue(focusOld, assignmentValue)) {
                        continue;
                    }
                    ApprovalRequest<AssignmentType> req = processAssignmentToAdd(config, assignmentValue, result);
                    if (req != null) {
                        approvalRequestList.add(req);
                        valueIterator.remove();
                    }
                }
            }
            // let's sanitize the delta
            if (delta.getValuesToAdd() != null && delta.getValuesToAdd().isEmpty()) {         // empty set of values to add is an illegal state
                delta.resetValuesToAdd();
            }
            if (delta.getValuesToAdd() == null && delta.getValuesToReplace() == null && delta.getValuesToDelete() == null) {
                deltaIterator.remove();
            }
        }
        return approvalRequestList;
    }

    private boolean existsEquivalentValue(PrismObject<? extends ObjectType> focusOld, PrismContainerValue<AssignmentType> assignmentValue) {
        FocusType focusType = (FocusType) focusOld.asObjectable();
        for (AssignmentType existing : focusType.getAssignment()) {
            if (existing.asPrismContainerValue().equalsRealValue(assignmentValue)) {
                return true;
            }
        }
        return false;
    }

    private ApprovalRequest<AssignmentType> processAssignmentToAdd(PcpAspectConfigurationType config, PrismContainerValue<AssignmentType> assignmentCVal, OperationResult result) {
        AssignmentType assignmentType = assignmentCVal.asContainerable();
        if (isAssignmentRelevant(assignmentType)) {
            T specificObjectType = getAssignmentApprovalTarget(assignmentType, result);
            boolean approvalRequired = shouldAssignmentBeApproved(config, specificObjectType);
            LOGGER.trace(" - {} (approval required = {})", specificObjectType, approvalRequired);
            if (approvalRequired) {
                AssignmentType aCopy = cloneAndCanonicalizeAssignment(assignmentType);
                return createApprovalRequest(config, aCopy, specificObjectType);
            }
        }
        return null;
    }

    private List<PcpChildJobCreationInstruction> prepareJobCreateInstructions(ModelContext<?> modelContext, Task taskFromModel,
                                                                              OperationResult result, List<ApprovalRequest<AssignmentType>> approvalRequestList) throws SchemaException {

        List<PcpChildJobCreationInstruction> instructions = new ArrayList<>();
        String assigneeName = MiscDataUtil.getFocusObjectName(modelContext);
        String assigneeOid = primaryChangeAspectHelper.getObjectOid(modelContext);
        PrismObject<UserType> requester = primaryChangeAspectHelper.getRequester(taskFromModel, result);

        for (ApprovalRequest<AssignmentType> approvalRequest : approvalRequestList) {

            assert approvalRequest.getPrismContext() != null;

            LOGGER.trace("Approval request = {}", approvalRequest);
            AssignmentType assignmentType = approvalRequest.getItemToApprove();
            T target = getAssignmentApprovalTarget(assignmentType, result);
            Validate.notNull(target, "No target in assignment to be approved");

            String targetName = target.getName() != null ? target.getName().getOrig() : "(unnamed)";

            // create a JobCreateInstruction for a given change processor (primaryChangeProcessor in this case)
            PcpChildJobCreationInstruction instruction =
                    PcpChildJobCreationInstruction.createInstruction(getChangeProcessor());

            // set some common task/process attributes
            instruction.prepareCommonAttributes(this, modelContext, assigneeOid, requester);

            // prepare and set the delta that has to be approved
            ObjectDelta<? extends ObjectType> delta = assignmentToDelta(modelContext, assignmentType, assigneeOid);
            instruction.setDeltaProcessAndTaskVariables(delta);

            // set the names of midPoint task and activiti process instance
            String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and executing " : "";
            instruction.setTaskName("Workflow for approving " + andExecuting + "adding " + targetName + " to " + assigneeName);
            instruction.setProcessInstanceName("Adding " + targetName + " to " + assigneeName);

            // setup general item approval process
            String approvalTaskName = "Approve adding " + targetName + " to " + assigneeName;
            itemApprovalProcessInterface.prepareStartInstruction(instruction, approvalRequest, approvalTaskName);

            // set some aspect-specific variables
            instruction.addProcessVariable(AddRoleVariableNames.FOCUS_NAME, assigneeName);

            instructions.add(instruction);
        }
        return instructions;
    }

    // creates an ObjectDelta that will be executed after successful approval of the given assignment
    private ObjectDelta<? extends ObjectType> assignmentToDelta(ModelContext<?> modelContext, AssignmentType assignmentType, String objectOid) {
        PrismObject<FocusType> focus = (PrismObject<FocusType>) modelContext.getFocusContext().getObjectNew();
        PrismContainerDefinition<AssignmentType> prismContainerDefinition = focus.getDefinition().findContainerDefinition(FocusType.F_ASSIGNMENT);

        ItemDelta<PrismContainerValue<AssignmentType>> addRoleDelta = new ContainerDelta<>(new ItemPath(), FocusType.F_ASSIGNMENT, prismContainerDefinition, prismContext);
        PrismContainerValue<AssignmentType> assignmentValue = assignmentType.asPrismContainerValue().clone();
        addRoleDelta.addValueToAdd(assignmentValue);

        Class focusClass = primaryChangeAspectHelper.getFocusClass(modelContext);
        String focusOid = objectOid != null ? objectOid : PrimaryChangeProcessor.UNKNOWN_OID;
        return ObjectDelta.createModifyDelta(focusOid, addRoleDelta, focusClass, ((LensContext) modelContext).getPrismContext());
    }

    //endregion

    //region ------------------------------------------------------------ Things that execute when item is being approved

    @Override
    public PrismObject<? extends QuestionFormType> prepareQuestionForm(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("prepareQuestionForm starting: execution id {}, pid {}, variables = {}", task.getExecutionId(), task.getProcessInstanceId(), variables);
        }

        // todo check type compatibility
        ApprovalRequest request = (ApprovalRequest) variables.get(ProcessVariableNames.APPROVAL_REQUEST);
        request.setPrismContext(prismContext);
        Validate.notNull(request, "Approval request is not present among process variables");

        AssignmentType assignment = (AssignmentType) request.getItemToApprove();
        Validate.notNull(assignment, "Approval request does not contain as assignment");

        T target = getAssignmentApprovalTarget(assignment, result);     // may throw an (unchecked) exception

        PrismObjectDefinition<AssignmentCreationApprovalFormType> formDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(AssignmentCreationApprovalFormType.COMPLEX_TYPE);
        PrismObject<AssignmentCreationApprovalFormType> formPrism = formDefinition.instantiate();
        AssignmentCreationApprovalFormType form = formPrism.asObjectable();

        String focusName = (String) variables.get(AddRoleVariableNames.FOCUS_NAME);
        form.setFocusName(focusName);                                   // TODO disginguish somehow between users/roles/orgs
        form.setAssignedObjectName(getTargetDisplayName(target));
        form.setRequesterComment(assignment.getDescription());
        form.setTimeInterval(formatTimeIntervalBrief(assignment));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resulting prism object instance = {}", formPrism.debugDump());
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
        T target = getAssignmentApprovalTarget(approvalRequest.getItemToApprove(), result);
        return target != null ? target.asPrismObject() : null;
    }
    //endregion

    //region ------------------------------------------------------------ Things to override in concrete aspect classes

    // a quick check whether expected focus type (User, Role) matches the actual focus type in current model operation context
    protected abstract boolean isFocusRelevant(ModelContext modelContext);

    // is the assignment relevant for a given aspect? (e.g. is this an assignment of a role?)
    protected abstract boolean isAssignmentRelevant(AssignmentType assignmentType);

    // should the given assignment be approved? (typically, does the target object have an approver specified?)
    protected abstract boolean shouldAssignmentBeApproved(PcpAspectConfigurationType config, T target);

    // before creating a delta for the assignment, it has to be cloned and canonicalized by removing full target object
    protected abstract AssignmentType cloneAndCanonicalizeAssignment(AssignmentType a);

    // creates an approval requests (e.g. by providing approval schema) for a given assignment and a target
    protected abstract ApprovalRequest<AssignmentType> createApprovalRequest(PcpAspectConfigurationType config, AssignmentType assignmentType, T target);

    // retrieves the relevant target for a given assignment - a role, an org, or a resource
    protected abstract T getAssignmentApprovalTarget(AssignmentType assignmentType, OperationResult result);

    // creates name to be displayed in the question form (may be overriden by child objects)
    protected String getTargetDisplayName(T target) {
        if (target.getName() != null) {
            return target.getName().getOrig();
        } else {
            return target.getOid();
        }
    }
    //endregion
}