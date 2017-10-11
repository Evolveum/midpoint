/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.MidpointUtil;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.function.Function;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder.getMiscDataUtil;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType.APPROVE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType.AUTO_COMPLETION_CONDITION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType.NO_ASSIGNEES_FOUND;

/**
 * @author mederly
 */
@Component
public class WfStageComputeHelper {

	private static final transient Trace LOGGER = TraceManager.getTrace(WfStageComputeHelper.class);

	@Autowired private WfExpressionEvaluationHelper evaluationHelper;

	public ExpressionVariables getDefaultVariables(@Nullable DelegateExecution execution, Task wfTask, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		ExpressionVariables variables = getDefaultVariables(wfTask.getWorkflowContext(), wfTask.getChannel(), result);
		// Activiti process instance variables (use with care)
		if (execution != null) {
			execution.getVariables().forEach((key, value) -> variables.addVariableDefinition(new QName("_" + key), value));
		}
		return variables;
	}

	public ExpressionVariables getDefaultVariables(WfContextType wfContext, String requestChannel, OperationResult result)
			throws SchemaException, ObjectNotFoundException {

		MiscDataUtil miscDataUtil = getMiscDataUtil();

		ExpressionVariables variables = new ExpressionVariables();

		variables.addVariableDefinition(C_REQUESTER,
				miscDataUtil.resolveObjectReference(wfContext.getRequesterRef(), result));

		variables.addVariableDefinition(C_OBJECT,
				miscDataUtil.resolveObjectReference(wfContext.getObjectRef(), result));

		// might be null
		variables.addVariableDefinition(C_TARGET,
				miscDataUtil.resolveObjectReference(wfContext.getTargetRef(), result));

		ObjectDelta objectDelta;
		try {
			objectDelta = miscDataUtil.getFocusPrimaryDelta(wfContext, true);
		} catch (JAXBException e) {
			throw new SchemaException("Couldn't get object delta: " + e.getMessage(), e);
		}
		variables.addVariableDefinition(SchemaConstants.T_OBJECT_DELTA, objectDelta);

		variables.addVariableDefinition(ExpressionConstants.VAR_CHANNEL, requestChannel);

		variables.addVariableDefinition(ExpressionConstants.VAR_WORKFLOW_CONTEXT, wfContext);
		// todo other variables?

		return variables;
	}

	// TODO name
	public class ComputationResult {
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
			variables.addVariableDefinition(ExpressionConstants.VAR_STAGE_DEFINITION, stageDef);
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
				} catch (ExpressionEvaluationException | ObjectNotFoundException | SchemaException | RuntimeException e) {
					throw new SystemException("Couldn't evaluate approvers expressions", e);
				}
			}

			LOGGER.trace("Approvers at the stage {} (before potential group expansion) are: {}", stageDef, rv.approverRefs);
			if (stageDef.getGroupExpansion() == GroupExpansionType.ON_WORK_ITEM_CREATION) {
				rv.approverRefs = MidpointUtil.expandGroups(rv.approverRefs);       // see MID-4105
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
			Task opTask, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		List<String> outcomes = evaluationHelper.evaluateExpression(stageDef.getAutomaticallyCompleted(), variables,
				"automatic completion expression", String.class,
				DOMUtil.XSD_STRING, createOutcomeConvertor(), opTask, result);
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
}

