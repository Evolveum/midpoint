/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.wf.impl.processes.common;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType.APPROVE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType.AUTO_COMPLETION_CONDITION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType.NO_ASSIGNEES_FOUND;

/**
 * Helps with computing things needed for stage approval (e.g. approvers, auto-approval result, ...)
 */
@Component
public class StageComputeHelper {

	private static final transient Trace LOGGER = TraceManager.getTrace(StageComputeHelper.class);

	@Autowired private ExpressionEvaluationHelper evaluationHelper;
	@Autowired private PrismContext prismContext;
	@Autowired private MiscHelper miscHelper;
	@Autowired private RepositoryService repositoryService;

	public ExpressionVariables getDefaultVariables(CaseType aCase,
			WfContextType wfContext, String requestChannel, OperationResult result)
			throws SchemaException {

		ExpressionVariables variables = new ExpressionVariables();
		variables.put(ExpressionConstants.VAR_REQUESTER, miscHelper.resolveTypedObjectReference(aCase.getRequestorRef(), result));
		variables.put(ExpressionConstants.VAR_OBJECT, miscHelper.resolveTypedObjectReference(aCase.getObjectRef(), result));
		// might be null
		variables.put(ExpressionConstants.VAR_TARGET, miscHelper.resolveTypedObjectReference(aCase.getTargetRef(), result));
		variables.put(ExpressionConstants.VAR_OBJECT_DELTA, getFocusPrimaryDelta(wfContext), ObjectDelta.class);
		variables.put(ExpressionConstants.VAR_CHANNEL, requestChannel, String.class);
		variables.put(ExpressionConstants.VAR_WORKFLOW_CONTEXT, wfContext, WfContextType.class);
		// todo other variables?

		return variables;
	}

	private ObjectDelta getFocusPrimaryDelta(WfContextType workflowContext) throws SchemaException {
		ObjectDeltaType objectDeltaType = getFocusPrimaryObjectDeltaType(workflowContext);
		return objectDeltaType != null ? DeltaConvertor.createObjectDelta(objectDeltaType, prismContext) : null;
	}

	// mayBeNull=false means that the corresponding variable must be present (not that focus must be non-null)
	// TODO: review/correct this!
	private ObjectDeltaType getFocusPrimaryObjectDeltaType(WfContextType workflowContext) {
		ObjectTreeDeltasType deltas = getObjectTreeDeltaType(workflowContext);
		return deltas != null ? deltas.getFocusPrimaryDelta() : null;
	}

	private ObjectTreeDeltasType getObjectTreeDeltaType(WfContextType workflowContext) {
		WfProcessorSpecificStateType state = workflowContext.getProcessorSpecificState();
		if (state == null) {
			return null;
		} else if (!(state instanceof WfPrimaryChangeProcessorStateType)) {
			throw new IllegalStateException("Expected WfPrimaryChangeProcessorStateType but got " + state);
		} else {
			return ((WfPrimaryChangeProcessorStateType) state).getDeltasToProcess();
		}
	}

	// TODO name
	public static class ComputationResult {
		private ApprovalLevelOutcomeType predeterminedOutcome;
		private AutomatedCompletionReasonType automatedCompletionReason;
		private Set<ObjectReferenceType> approverRefs;
		private boolean noApproversFound;   // computed but not found (i.e. not set when outcome is given by an auto-outcome expression)

		public ApprovalLevelOutcomeType getPredeterminedOutcome() {
			return predeterminedOutcome;
		}

		public AutomatedCompletionReasonType getAutomatedCompletionReason() {
			return automatedCompletionReason;
		}

		public Set<ObjectReferenceType> getApproverRefs() {
			return approverRefs;
		}

		public boolean noApproversFound() {
			return noApproversFound;
		}
	}

	@FunctionalInterface
	public interface VariablesProvider {
		ExpressionVariables get() throws SchemaException, ObjectNotFoundException;
	}

	// TODO method name
	public ComputationResult computeStageApprovers(ApprovalStageDefinitionType stageDef, VariablesProvider variablesProvider,
			Task opTask, OperationResult opResult) {
		ComputationResult rv = new ComputationResult();
		ExpressionVariables expressionVariables = null;
		VariablesProvider enhancedVariablesProvider = () -> {
			ExpressionVariables variables = variablesProvider.get();
			variables.put(ExpressionConstants.VAR_STAGE_DEFINITION, stageDef, ApprovalStageDefinitionType.class);
			return variables;
		};

		if (stageDef.getAutomaticallyApproved() != null) {
			try {
				expressionVariables = enhancedVariablesProvider.get();
				boolean preApproved = evaluationHelper.evaluateBooleanExpression(stageDef.getAutomaticallyApproved(), expressionVariables,
						"automatic approval expression", opTask, opResult);
				LOGGER.trace("Pre-approved = {} for stage {}", preApproved, stageDef);
				if (preApproved) {
					rv.predeterminedOutcome = APPROVE;
					rv.automatedCompletionReason = AUTO_COMPLETION_CONDITION;
				}
			} catch (Exception e) {     // todo
				throw new SystemException("Couldn't evaluate auto-approval expression", e);
			}
		}

		if (rv.predeterminedOutcome == null && stageDef.getAutomaticallyCompleted() != null) {
			try {
				if (expressionVariables == null) {
					expressionVariables = enhancedVariablesProvider.get();
				}
				String outcome = evaluateAutoCompleteExpression(stageDef, expressionVariables, opTask, opResult);
				if (outcome != null) {
					rv.predeterminedOutcome = ApprovalUtils.approvalLevelOutcomeFromUri(outcome);
					rv.automatedCompletionReason = AUTO_COMPLETION_CONDITION;
				}
			} catch (Exception e) {     // todo
				throw new SystemException("Couldn't evaluate auto-approval expression", e);
			}
		}

		rv.approverRefs = new HashSet<>();

		if (rv.predeterminedOutcome == null) {
			rv.approverRefs.addAll(CloneUtil.cloneCollectionMembers(stageDef.getApproverRef()));

			if (!stageDef.getApproverExpression().isEmpty()) {
				try {
					if (expressionVariables == null) {
						expressionVariables = enhancedVariablesProvider.get();
					}
					rv.approverRefs.addAll(evaluationHelper.evaluateRefExpressions(stageDef.getApproverExpression(), expressionVariables,
							"resolving approver expression", opTask, opResult));
				} catch (ExpressionEvaluationException | ObjectNotFoundException | SchemaException | RuntimeException | CommunicationException | ConfigurationException | SecurityViolationException e) {
					throw new SystemException("Couldn't evaluate approvers expressions", e);
				}
			}

			LOGGER.trace("Approvers at the stage {} (before potential group expansion) are: {}", stageDef, rv.approverRefs);
			if (stageDef.getGroupExpansion() == GroupExpansionType.ON_WORK_ITEM_CREATION) {
				rv.approverRefs = expandGroups(rv.approverRefs);       // see MID-4105
				LOGGER.trace("Approvers at the stage {} (after group expansion) are: {}", stageDef, rv.approverRefs);
			}

			if (rv.approverRefs.isEmpty()) {
				rv.noApproversFound = true;
				if (stageDef.getOutcomeIfNoApprovers() != null) {       // should be always the case (default is REJECT)
					rv.predeterminedOutcome = stageDef.getOutcomeIfNoApprovers();
					rv.automatedCompletionReason = NO_ASSIGNEES_FOUND;
				}
			}
		}
		return rv;
	}

	public String evaluateAutoCompleteExpression(ApprovalStageDefinitionType stageDef, ExpressionVariables variables,
			Task opTask, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
		List<String> outcomes = evaluationHelper.evaluateExpression(stageDef.getAutomaticallyCompleted(), variables,
				"automatic completion expression", String.class,
				DOMUtil.XSD_STRING, false, createOutcomeConvertor(), opTask, result);
		LOGGER.trace("Pre-completed = {} for stage {}", outcomes, stageDef);
		Set<String> distinctOutcomes = new HashSet<>(outcomes);
		if (distinctOutcomes.isEmpty()) {
			return null;
		} else if (distinctOutcomes.size() == 1) {
			return distinctOutcomes.iterator().next();
		} else {
			throw new IllegalStateException("Ambiguous result from 'automatically completed' expression: " + distinctOutcomes);
		}
	}

	private Function<Object, Object> createOutcomeConvertor() {
		return (o) -> {
			if (o == null || o instanceof String) {
				return o;
			} else if (o instanceof ApprovalLevelOutcomeType) {
				return ApprovalUtils.toUri((ApprovalLevelOutcomeType) o);
			} else if (o instanceof QName) {
				return QNameUtil.qNameToUri((QName) o);
			} else {
				//throw new IllegalArgumentException("Couldn't create an URI from " + o);
				return o;		// let someone else complain about this
			}
		};
	}

	private Set<ObjectReferenceType> expandGroups(Set<ObjectReferenceType> approverRefs) {
		Set<ObjectReferenceType> rv = new HashSet<>();
		for (ObjectReferenceType approverRef : approverRefs) {
			@SuppressWarnings({ "unchecked", "raw" })
			Class<? extends Containerable> clazz = (Class<? extends Containerable>)
					prismContext.getSchemaRegistry().getCompileTimeClassForObjectType(approverRef.getType());
			if (clazz == null) {
				throw new IllegalStateException("Unknown object type " + approverRef.getType());
			}
			if (UserType.class.isAssignableFrom(clazz)) {
				rv.add(approverRef.clone());
			} else if (AbstractRoleType.class.isAssignableFrom(clazz)) {
				rv.addAll(expandAbstractRole(approverRef));
			} else {
				LOGGER.warn("Unexpected type {} for approver: {}", clazz, approverRef);
				rv.add(approverRef.clone());
			}
		}
		return rv;
	}

	private Collection<ObjectReferenceType> expandAbstractRole(ObjectReferenceType approverRef) {
		ObjectQuery query = prismContext.queryFor(UserType.class)
				.item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(approverRef.asReferenceValue())
				.build();
		try {
			return repositoryService
					.searchObjects(UserType.class, query, null, new OperationResult("dummy"))
					.stream()
					.map(o -> ObjectTypeUtil.createObjectRef(o, prismContext))
					.collect(Collectors.toList());
		} catch (SchemaException e) {
			throw new SystemException("Couldn't resolve " + approverRef + ": " + e.getMessage(), e);
		}
	}


}

