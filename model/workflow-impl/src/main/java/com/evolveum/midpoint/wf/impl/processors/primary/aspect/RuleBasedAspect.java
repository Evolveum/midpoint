/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.wf.impl.processors.primary.aspect;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequestImpl;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalProcessInterface;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalSpecificContent;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.assignments.AssignmentHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.wf.impl.util.MiscDataUtil.getFocusObjectName;
import static com.evolveum.midpoint.wf.impl.util.MiscDataUtil.getFocusObjectOid;

/**
 *
 * @author mederly
 */
@Component
public abstract class RuleBasedAspect extends BasePrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(RuleBasedAspect.class);

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected ItemApprovalProcessInterface itemApprovalProcessInterface;

    @Autowired
    protected AssignmentHelper assignmentHelper;

    //region ------------------------------------------------------------ Things that execute on request arrival

    @NotNull
	@Override
    public List<PcpChildWfTaskCreationInstruction> prepareTasks(@NotNull ModelContext<?> modelContext,
            PrimaryChangeProcessorConfigurationType wfConfigurationType, @NotNull ObjectTreeDeltas objectTreeDeltas,
            @NotNull Task taskFromModel, @NotNull OperationResult result) throws SchemaException {

		List<ApprovalRequest<AssignmentType>> requests = new ArrayList<>();

		DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = ((LensContext<?>) modelContext).getEvaluatedAssignmentTriple();
		LOGGER.trace("Processing evaluatedAssignmentTriple:\n{}", DebugUtil.debugDumpLazily(evaluatedAssignmentTriple));
        for (EvaluatedAssignment<?> newAssignment : evaluatedAssignmentTriple.getPlusSet()) {
			LOGGER.trace("Assignment to be added: -> {} ({} policy rules)", newAssignment.getTarget(), newAssignment.getPolicyRules().size());
			List<ApprovalPolicyActionType> approvalActions = new ArrayList<>();
            for (EvaluatedPolicyRule rule : newAssignment.getPolicyRules()) {
				if (rule.getTriggers().isEmpty()) {
					LOGGER.trace("Skipping rule {} that is present but not triggered", rule.getName());
					continue;
				}
				if (rule.getActions() == null || rule.getActions().getApproval() == null) {
					LOGGER.trace("Skipping rule {} that doesn't contain an approval action", rule.getName());
					continue;
				}
				approvalActions.add(rule.getActions().getApproval());
            }
            if (approvalActions.isEmpty()) {
				LOGGER.trace("This assignment hasn't triggered approval actions, continuing with a next one.");
				continue;
			}
			PrismContainerValue<AssignmentType> assignmentValue = newAssignment.getAssignmentType().asPrismContainerValue();
			boolean removed = objectTreeDeltas.subtractFromFocusDelta(new ItemPath(FocusType.F_ASSIGNMENT), assignmentValue);
			if (!removed) {
				throw new IllegalStateException("Assignment with a value of " + assignmentValue.debugDump()
						+ " was not found in deltas: " + objectTreeDeltas.debugDump());
			}
			requests.add(createAssignmentApprovalRequest(newAssignment, approvalActions));
        }
        return prepareTaskInstructions(modelContext, taskFromModel, result, requests);
    }

	private ApprovalRequest<AssignmentType> createAssignmentApprovalRequest(EvaluatedAssignment<?> newAssignment,
			List<ApprovalPolicyActionType> approvalActions) {
		ApprovalSchemaType approvalSchema = new ApprovalSchemaType(prismContext);
		for (ApprovalPolicyActionType action : approvalActions) {

		}
		return new ApprovalRequestImpl<>(newAssignment.getAssignmentType(), approvalSchema, prismContext);
	}


    private List<PcpChildWfTaskCreationInstruction> prepareTaskInstructions(ModelContext<?> modelContext, Task taskFromModel,
            OperationResult result, List<ApprovalRequest<AssignmentType>> approvalRequestList) throws SchemaException {

        List<PcpChildWfTaskCreationInstruction> instructions = new ArrayList<>();
		String assigneeOid = getFocusObjectOid(modelContext);
        String assigneeName = getFocusObjectName(modelContext);
        PrismObject<UserType> requester = baseModelInvocationProcessingHelper.getRequester(taskFromModel, result);

        for (ApprovalRequest<AssignmentType> approvalRequest : approvalRequestList) {

            assert approvalRequest.getPrismContext() != null;

            LOGGER.trace("Approval request = {}", approvalRequest);
            AssignmentType assignmentType = approvalRequest.getItemToApprove();
            T target = getAssignmentApprovalTarget(assignmentType, result);
            Validate.notNull(target, "No target in assignment to be approved");

            String targetName = target.getName() != null ? target.getName().getOrig() : "(unnamed)";
            String approvalTaskName = "Approve adding " + targetName + " to " + assigneeName;

            PcpChildWfTaskCreationInstruction<ItemApprovalSpecificContent> instruction =
                    PcpChildWfTaskCreationInstruction.createItemApprovalInstruction(getChangeProcessor(), approvalTaskName, approvalRequest);

            instruction.prepareCommonAttributes(this, modelContext, requester);

            ObjectDelta<? extends FocusType> delta = assignmentToDelta(modelContext, assignmentType, assigneeOid);
            instruction.setDeltasToProcess(delta);

            instruction.setObjectRef(modelContext, result);
            instruction.setTargetRef(createObjectRef(target), result);

            String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and execution " : "";
            instruction.setTaskName("Approval " + andExecuting + "of assigning " + targetName + " to " + assigneeName);
            instruction.setProcessInstanceName("Assigning " + targetName + " to " + assigneeName);

            itemApprovalProcessInterface.prepareStartInstruction(instruction);

            instructions.add(instruction);
        }
        return instructions;
    }

    // creates an ObjectDelta that will be executed after successful approval of the given assignment
	@SuppressWarnings("unchecked")
    private ObjectDelta<? extends FocusType> assignmentToDelta(ModelContext<?> modelContext, AssignmentType assignmentType, String objectOid) {
        PrismObject<FocusType> focus = (PrismObject<FocusType>) modelContext.getFocusContext().getObjectNew();
        PrismContainerDefinition<AssignmentType> prismContainerDefinition = focus.getDefinition().findContainerDefinition(FocusType.F_ASSIGNMENT);

        ItemDelta<PrismContainerValue<AssignmentType>,PrismContainerDefinition<AssignmentType>> addRoleDelta = new ContainerDelta<>(new ItemPath(), FocusType.F_ASSIGNMENT, prismContainerDefinition, prismContext);
        PrismContainerValue<AssignmentType> assignmentValue = assignmentType.asPrismContainerValue().clone();
        addRoleDelta.addValueToAdd(assignmentValue);

        Class focusClass = primaryChangeAspectHelper.getFocusClass(modelContext);
        return ObjectDelta.createModifyDelta(objectOid, addRoleDelta, focusClass, ((LensContext) modelContext).getPrismContext());
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