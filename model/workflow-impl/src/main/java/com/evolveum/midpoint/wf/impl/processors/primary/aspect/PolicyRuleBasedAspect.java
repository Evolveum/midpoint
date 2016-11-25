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
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.*;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.apache.velocity.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.wf.impl.util.MiscDataUtil.getFocusObjectName;
import static com.evolveum.midpoint.wf.impl.util.MiscDataUtil.getFocusObjectOid;

/**
 *
 * @author mederly
 */
@Component
public class PolicyRuleBasedAspect extends BasePrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleBasedAspect.class);

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected ItemApprovalProcessInterface itemApprovalProcessInterface;

	@Autowired
	protected ApprovalSchemaHelper approvalSchemaHelper;

    //region ------------------------------------------------------------ Things that execute on request arrival

	@Override
	public boolean isEnabledByDefault() {
		return true;
	}

	@Override
	protected boolean isFirst() {
		return true;
	}

	@NotNull
	@Override
    public List<PcpChildWfTaskCreationInstruction> prepareTasks(@NotNull ModelContext<?> modelContext,
            WfConfigurationType wfConfigurationType, @NotNull ObjectTreeDeltas objectTreeDeltas,
            @NotNull Task taskFromModel, @NotNull OperationResult result) throws SchemaException {

		List<PcpChildWfTaskCreationInstruction> instructions = new ArrayList<>();
		PrismObject<UserType> requester = baseModelInvocationProcessingHelper.getRequester(taskFromModel, result);

		if (objectTreeDeltas.getFocusChange() != null) {
			extractAssignmentBasedInstructions(modelContext, objectTreeDeltas, requester, instructions, wfConfigurationType, taskFromModel, result);
			extractObjectBasedInstructions((LensContext<?>) modelContext, objectTreeDeltas, requester, instructions, taskFromModel, result);
		}
        return instructions;
    }

	private void extractAssignmentBasedInstructions(@NotNull ModelContext<?> modelContext,
			@NotNull ObjectTreeDeltas objectTreeDeltas, PrismObject<UserType> requester,
			List<PcpChildWfTaskCreationInstruction> instructions, WfConfigurationType wfConfigurationType,
			@NotNull Task taskFromModel, @NotNull OperationResult result)
			throws SchemaException {

		ObjectDelta<? extends ObjectType> focusDelta = objectTreeDeltas.getFocusChange();

		LegacyApproversSpecificationUsageType configuredUseLegacyApprovers = baseConfigurationHelper.getUseLegacyApproversSpecification(wfConfigurationType);

		DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = ((LensContext<?>) modelContext).getEvaluatedAssignmentTriple();
		LOGGER.trace("Processing evaluatedAssignmentTriple:\n{}", DebugUtil.debugDumpLazily(evaluatedAssignmentTriple));
		if (evaluatedAssignmentTriple == null || evaluatedAssignmentTriple.getPlusSet() == null) {
			return;
		}

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
			if (newAssignment.getTarget() == null) {
				if (!approvalActions.isEmpty()) {
					throw new IllegalStateException("No target in " + newAssignment + ", but with "
							+ approvalActions.size() + " approval action(s)");
				} else {
					continue;
				}
			}
			boolean noExplicitApprovalAction = approvalActions.isEmpty();
			if (noExplicitApprovalAction) {
				ApprovalPolicyActionType defaultPolicyAction = new ApprovalPolicyActionType(prismContext);
				defaultPolicyAction.getApproverRelation().add(SchemaConstants.ORG_APPROVER);
				approvalActions.add(defaultPolicyAction);
				LOGGER.trace("Added default approval action, as no explicit one was found");
			}
			boolean useLegacy = configuredUseLegacyApprovers == LegacyApproversSpecificationUsageType.ALWAYS
					|| configuredUseLegacyApprovers == LegacyApproversSpecificationUsageType.IF_NO_EXPLICIT_APPROVAL_POLICY_ACTION
							&& noExplicitApprovalAction;
			ApprovalRequest<?> request = createAssignmentApprovalRequest(newAssignment, approvalActions, useLegacy, result);
			if (!request.getApprovalSchema().isEmpty()) {
				PrismContainerValue<AssignmentType> assignmentValue = newAssignment.getAssignmentType().asPrismContainerValue();
				boolean removed = objectTreeDeltas.subtractFromFocusDelta(new ItemPath(FocusType.F_ASSIGNMENT), assignmentValue);
				if (!removed) {
					String message = "Assignment with a value of " + assignmentValue.debugDump() + " was not found in deltas: "
							+ objectTreeDeltas.debugDump();
					assert false : message;				// fail in test mode; just log an error otherwise
					LOGGER.error("{}", message);
					return;
				}
				if (focusDelta.isAdd()) {
					miscDataUtil.generateFocusOidIfNeeded(modelContext, focusDelta);
				}
				instructions.add(
						prepareAssignmentRelatedTaskInstruction(request, newAssignment, modelContext, taskFromModel, requester,
								result));
			}
		}
	}

	private void extractObjectBasedInstructions(@NotNull LensContext<?> modelContext,
			@NotNull ObjectTreeDeltas objectTreeDeltas, PrismObject<UserType> requester,
			List<PcpChildWfTaskCreationInstruction> instructions, @NotNull Task taskFromModel, @NotNull OperationResult result)
			throws SchemaException {

		ObjectDelta<?> focusDelta = objectTreeDeltas.getFocusChange();
		LensFocusContext<?> focusContext = modelContext.getFocusContext();
		PrismObject<?> object = focusContext.getObjectOld() != null ?
				focusContext.getObjectOld() : focusContext.getObjectNew();
		Map<Set<ItemPath>, ApprovalSchemaType> approvalSchemas = new HashMap<>();

		Collection<EvaluatedPolicyRule> policyRules = focusContext.getPolicyRules();
		for (EvaluatedPolicyRule rule : policyRules) {
			LOGGER.trace("Processing object-level policy rule:\n{}", DebugUtil.debugDumpLazily(rule));
			if (rule.getTriggers().isEmpty()) {
				LOGGER.trace("Skipping the rule because it is not triggered", rule.getName());
				continue;
			}
			ApprovalPolicyActionType approvalAction = rule.getActions() != null ? rule.getActions().getApproval() : null;
			if (approvalAction == null) {
				LOGGER.trace("Skipping the rule because it doesn't contain an approval action", rule.getName());
				continue;
			}
			Set<ItemPath> key;
			if (focusDelta.isAdd() || focusDelta.isDelete()) {
				key = Collections.emptySet();
			} else {
				Set<ItemPath> items = getAffectedItems(rule.getTriggers());
				Set<ItemPath> deltaItems = new HashSet<>(focusDelta.getModifiedItems());
				Set<ItemPath> affectedItems;
				if (items.isEmpty()) {
					affectedItems = deltaItems;        // whole object
				} else {
					affectedItems = new HashSet<>(CollectionUtils.intersection(items, deltaItems));
				}
				key = affectedItems;
			}
			approvalSchemas.put(key,
					addApprovalActionIntoApprovalSchema(
							approvalSchemas.get(key),
							approvalAction,
							findApproversByReference(object, approvalAction, result)));
		}
		// default rule
		if (approvalSchemas.isEmpty()) {
			ApprovalPolicyActionType defaultPolicyAction = new ApprovalPolicyActionType(prismContext);
			defaultPolicyAction.getApproverRelation().add(SchemaConstants.ORG_OWNER);
			approvalSchemas.put(Collections.emptySet(),
					addApprovalActionIntoApprovalSchema(null, defaultPolicyAction,
							findApproversByReference(object, defaultPolicyAction, result)));
			LOGGER.trace("Added default approval action, as no explicit one was found");
		}
		// create approval requests; also test for overlaps
		Set<ItemPath> itemsProcessed = null;
		for (Map.Entry<Set<ItemPath>, ApprovalSchemaType> entry : approvalSchemas.entrySet()) {
			Set<ItemPath> items = entry.getKey();
			if (itemsProcessed != null) {
				if (items.isEmpty() || itemsProcessed.isEmpty() || CollectionUtils.containsAny(itemsProcessed, items)) {
					throw new IllegalStateException("Overlapping modification-related policy rules. "
							+ "Items processed = " + itemsProcessed + ", current items = " + items);
				}
				itemsProcessed.addAll(items);
			} else {
				itemsProcessed = items;
			}
			ApprovalRequest<?> request = new ApprovalRequestImpl<>("dummy", entry.getValue(), prismContext);
			if (!request.getApprovalSchema().isEmpty()) {
				instructions.add(
						prepareObjectRelatedTaskInstruction(request, focusDelta, items, modelContext, taskFromModel, requester, result));
			}
		}
	}

	private Set<ItemPath> getAffectedItems(Collection<EvaluatedPolicyRuleTrigger> triggers) {
		Set<ItemPath> rv = new HashSet<>();
		for (EvaluatedPolicyRuleTrigger trigger : triggers) {
			if (trigger.getConstraint() instanceof ModificationPolicyConstraintType) {
				ModificationPolicyConstraintType modConstraint = (ModificationPolicyConstraintType) trigger.getConstraint();
				if (modConstraint.getItem().isEmpty()) {
					return Collections.emptySet();			// all items
				} else {
					modConstraint.getItem().forEach(
							itemPathType -> rv.add(itemPathType.getItemPath()));
				}
			}
		}
		return rv;
	}

	private ApprovalRequest<AssignmentType> createAssignmentApprovalRequest(EvaluatedAssignment<?> newAssignment,
			List<ApprovalPolicyActionType> approvalActions, boolean useLegacyApprovers,
			OperationResult result) throws SchemaException {
		PrismObject<?> target = newAssignment.getTarget();
		if (target == null) {
			throw new IllegalStateException("No target in " + newAssignment);
		}
		ApprovalSchemaType approvalSchema = null;
		if (useLegacyApprovers && target.asObjectable() instanceof AbstractRoleType) {
			AbstractRoleType abstractRole = (AbstractRoleType) target.asObjectable();
			if (abstractRole.getApprovalSchema() != null) {
				approvalSchema = abstractRole.getApprovalSchema().clone();
			} else if (!abstractRole.getApproverRef().isEmpty() || !abstractRole.getApproverExpression().isEmpty()) {
				approvalSchema = new ApprovalSchemaType(prismContext);
				ApprovalLevelType level = new ApprovalLevelType(prismContext);
				level.getApproverRef().addAll(CloneUtil.cloneCollectionMembers(abstractRole.getApproverRef()));
				level.getApproverExpression().addAll(CloneUtil.cloneCollectionMembers(abstractRole.getApproverExpression()));
				level.setAutomaticallyApproved(abstractRole.getAutomaticallyApproved());
				level.setOrder(1);
				approvalSchema.getLevel().add(level);
			}
		}
		for (ApprovalPolicyActionType action : approvalActions) {
			approvalSchema = addApprovalActionIntoApprovalSchema(approvalSchema, action, findApproversByReference(target, action, result));
		}
		assert approvalSchema != null;
		return new ApprovalRequestImpl<>(newAssignment.getAssignmentType(), approvalSchema, prismContext);
	}

	private List<ObjectReferenceType> findApproversByReference(PrismObject<?> target, ApprovalPolicyActionType action,
			OperationResult result) throws SchemaException {
		if (target == null || action.getApproverRelation().isEmpty()) {
			return Collections.emptyList();
		}
		S_AtomicFilterExit q = QueryBuilder.queryFor(FocusType.class, prismContext).none();
		for (QName approverRelation : action.getApproverRelation()) {
			PrismReferenceValue approverReference = new PrismReferenceValue(target.getOid());
			approverReference.setRelation(QNameUtil.qualifyIfNeeded(approverRelation, SchemaConstants.NS_ORG));
			q = q.or().item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(approverReference);
		}
		ObjectQuery query = q.build();
		LOGGER.trace("Looking for approvers for {} using query:\n{}", target, DebugUtil.debugDumpLazily(query));
		List<PrismObject<FocusType>> objects = repositoryService.searchObjects(FocusType.class, query, null, result);
		Set<PrismObject<FocusType>> distinctObjects = new HashSet<>(objects);
		LOGGER.trace("Found {} approver(s): {}", distinctObjects.size(), DebugUtil.toStringLazily(distinctObjects));
		return distinctObjects.stream()
				.map(ObjectTypeUtil::createObjectRef)
				.collect(Collectors.toList());
	}

	private ApprovalSchemaType addApprovalActionIntoApprovalSchema(ApprovalSchemaType approvalSchema, ApprovalPolicyActionType action,
			@Nullable List<ObjectReferenceType> additionalReviewers) {
		if (action.getApprovalSchema() != null) {
			approvalSchema = approvalSchemaHelper.mergeIntoSchema(approvalSchema, action.getApprovalSchema());
		} else {
			approvalSchema = approvalSchemaHelper
					.mergeIntoSchema(approvalSchema, action.getApproverExpression(), action.getAutomaticallyApproved(), additionalReviewers);
		}
		return approvalSchema;
	}

	private PcpChildWfTaskCreationInstruction prepareAssignmentRelatedTaskInstruction(ApprovalRequest<?> approvalRequest,
			EvaluatedAssignment<?> evaluatedAssignment, ModelContext<?> modelContext, Task taskFromModel,
			PrismObject<UserType> requester, OperationResult result) throws SchemaException {

		String objectOid = getFocusObjectOid(modelContext);
		String objectName = getFocusObjectName(modelContext);

		assert approvalRequest.getPrismContext() != null;

		LOGGER.trace("Approval request = {}", approvalRequest);

		PrismObject<? extends ObjectType> target = (PrismObject<? extends ObjectType>) evaluatedAssignment.getTarget();
		Validate.notNull(target, "assignment target is null");

		String targetName = target.getName() != null ? target.getName().getOrig() : "(unnamed)";
		String approvalTaskName = "Approve adding " + targetName + " to " + objectName;				// TODO adding?

		PcpChildWfTaskCreationInstruction<ItemApprovalSpecificContent> instruction =
				PcpChildWfTaskCreationInstruction.createItemApprovalInstruction(getChangeProcessor(), approvalTaskName, approvalRequest);

		instruction.prepareCommonAttributes(this, modelContext, requester);

		ObjectDelta<? extends FocusType> delta = assignmentToDelta(modelContext.getFocusClass(), evaluatedAssignment.getAssignmentType(), objectOid);
		instruction.setDeltasToProcess(delta);

		instruction.setObjectRef(modelContext, result);
		instruction.setTargetRef(createObjectRef(target), result);

		String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and execution " : "";
		instruction.setTaskName("Approval " + andExecuting + "of assigning " + targetName + " to " + objectName);
		instruction.setProcessInstanceName("Assigning " + targetName + " to " + objectName);

		itemApprovalProcessInterface.prepareStartInstruction(instruction);

		return instruction;
    }

	private PcpChildWfTaskCreationInstruction prepareObjectRelatedTaskInstruction(ApprovalRequest<?> approvalRequest,
			ObjectDelta<?> focusDelta, Set<ItemPath> paths, ModelContext<?> modelContext, Task taskFromModel,
			PrismObject<UserType> requester, OperationResult result) throws SchemaException {

		//String objectOid = getFocusObjectOid(modelContext);
		String objectName = getFocusObjectName(modelContext);

		assert approvalRequest.getPrismContext() != null;

		LOGGER.trace("Approval request = {}", approvalRequest);

		String opName;
		if (focusDelta.isAdd()) {
			opName = "addition";
		} else if (focusDelta.isDelete()) {
			opName = "deletion";
		} else {
			opName = "modification";
		}

		String approvalTaskName = "Approve " + opName + " of " + objectName;

		PcpChildWfTaskCreationInstruction<ItemApprovalSpecificContent> instruction =
				PcpChildWfTaskCreationInstruction.createItemApprovalInstruction(getChangeProcessor(), approvalTaskName, approvalRequest);

		instruction.prepareCommonAttributes(this, modelContext, requester);

		ObjectDelta<? extends FocusType> delta = (ObjectDelta<? extends FocusType>) subtractModifications(focusDelta, paths);
		instruction.setDeltasToProcess(delta);

		instruction.setObjectRef(modelContext, result);

		String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and execution " : "";
		instruction.setTaskName("Approval " + andExecuting + "of " + opName + " of " + objectName);
		instruction.setProcessInstanceName(StringUtils.capitalizeFirstLetter(opName) + " of " + objectName);

		itemApprovalProcessInterface.prepareStartInstruction(instruction);

		return instruction;
	}

	private ObjectDelta<?> subtractModifications(@NotNull ObjectDelta<?> focusDelta, @NotNull Set<ItemPath> itemPaths) {
		if (itemPaths.isEmpty()) {
			ObjectDelta<?> originalDelta = focusDelta.clone();
			if (focusDelta.isAdd()) {
				focusDelta.setObjectToAdd(null);
			} else if (focusDelta.isModify()) {
				focusDelta.getModifications().clear();
			} else if (focusDelta.isDelete()) {
				// hack: convert to empty ADD delta
				focusDelta.setChangeType(ChangeType.ADD);
				focusDelta.setObjectToAdd(null);
				focusDelta.setOid(null);
			} else {
				throw new IllegalStateException("Unsupported delta type: " + focusDelta.getChangeType());
			}
			return originalDelta;
		}
		if (!focusDelta.isModify()) {
			throw new IllegalStateException("Not a MODIFY delta; delta = " + focusDelta);
		}
		return focusDelta.subtract(itemPaths);
	}

	// creates an ObjectDelta that will be executed after successful approval of the given assignment
	@SuppressWarnings("unchecked")
    private ObjectDelta<? extends FocusType> assignmentToDelta(Class<? extends Objectable> focusClass, AssignmentType assignmentType,
			String objectOid) throws SchemaException {
		return (ObjectDelta<? extends FocusType>) DeltaBuilder.deltaFor(focusClass, prismContext)
				.item(FocusType.F_ASSIGNMENT).add(assignmentType.clone().asPrismContainerValue())
				.asObjectDelta(objectOid);
    }

    //endregion

}