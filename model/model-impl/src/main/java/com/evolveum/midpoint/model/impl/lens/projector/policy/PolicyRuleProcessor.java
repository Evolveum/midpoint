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
package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedExclusionTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil.toPrimitiveConstraintsList;

/**
 * @author semancik
 * @author mederly
 */
@Component
public class PolicyRuleProcessor {
	
	private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleProcessor.class);
	
	@Autowired private PrismContext prismContext;
	@Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
	@Autowired private MappingFactory mappingFactory;
	@Autowired private MappingEvaluator mappingEvaluator;
	@Autowired private MatchingRuleRegistry matchingRuleRegistry;

	@Autowired private AssignmentConstraintEvaluator assignmentConstraintEvaluator;
	@Autowired private HasAssignmentConstraintEvaluator hasAssignmentConstraintEvaluator;
	@Autowired private ExclusionConstraintEvaluator exclusionConstraintEvaluator;
	@Autowired private MultiplicityConstraintEvaluator multiplicityConstraintEvaluator;
	@Autowired private PolicySituationConstraintEvaluator policySituationConstraintEvaluator;
	@Autowired private ModificationConstraintEvaluator modificationConstraintEvaluator;
	@Autowired private StateConstraintEvaluator stateConstraintEvaluator;

	private static final QName CONDITION_OUTPUT_NAME = new QName(SchemaConstants.NS_C, "condition");

	//region ------------------------------------------------------------------ Assignment policy rules
	/**
	 * Evaluates the policies (policy rules, but also legacy policies). Triggers the rules.
	 * But does not enforce anything and does not make any context changes. TODO really? also for legacy policies?
	 *
	 * Takes into account all policy rules related to assignments in the given evaluatedAssignmentTriple.
	 * Focus policy rules are not processed here, even though they might come through these assignments.
	 */
	public <F extends FocusType> void evaluateAssignmentPolicyRules(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
			Task task, OperationResult result)
			throws PolicyViolationException, SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		for (EvaluatedAssignmentImpl<F> evaluatedAssignment : evaluatedAssignmentTriple.union()) {
			boolean inPlus = evaluatedAssignmentTriple.presentInPlusSet(evaluatedAssignment);
			boolean inMinus = evaluatedAssignmentTriple.presentInMinusSet(evaluatedAssignment);
			boolean inZero = evaluatedAssignmentTriple.presentInZeroSet(evaluatedAssignment);

			/*
			 *  Situation-related rules are to be evaluated last, because they refer to triggering of other rules.
			 *
			 *  Note: It is the responsibility of the administrator to avoid two situation-related rules
			 *  referring to each other, i.e.
			 *    Situation(URL1) -> Action1 [URL2]
			 *    Situation(URL2) -> Action2 [URL1]
			 */
			for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getThisTargetPolicyRules()) {
				if (!hasSituationConstraint(policyRule)) {
					evaluateRule(new AssignmentPolicyRuleEvaluationContext<>(policyRule,
							evaluatedAssignment, inPlus, inZero, inMinus, true, context, evaluatedAssignmentTriple, task), result);
				}
			}
			for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getOtherTargetsPolicyRules()) {
				if (!hasSituationConstraint(policyRule)) {
					evaluateRule(new AssignmentPolicyRuleEvaluationContext<>(policyRule,
							evaluatedAssignment, inPlus, inZero, inMinus, false, context, evaluatedAssignmentTriple, task), result);
				}
			}
			for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getThisTargetPolicyRules()) {
				if (hasSituationConstraint(policyRule)) {
					evaluateRule(new AssignmentPolicyRuleEvaluationContext<>(policyRule,
							evaluatedAssignment, inPlus, inZero, inMinus, true, context, evaluatedAssignmentTriple, task), result);
				}
			}
			for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getOtherTargetsPolicyRules()) {
				if (hasSituationConstraint(policyRule)) {
					evaluateRule(new AssignmentPolicyRuleEvaluationContext<>(policyRule,
							evaluatedAssignment, inPlus, inZero, inMinus, false, context, evaluatedAssignmentTriple, task), result);
				}
			}
		}

		exclusionConstraintEvaluator.checkExclusionsLegacy(context, evaluatedAssignmentTriple.getPlusSet(),
				evaluatedAssignmentTriple.getNonNegativeValues());
	}

	//endregion

	//region ------------------------------------------------------------------ Focus policy rules
	public <F extends FocusType> void evaluateFocusPolicyRules(LensContext<F> context, String activityDescription,
			XMLGregorianCalendar now, Task task, OperationResult result)
			throws PolicyViolationException, SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return;
		}

		List<EvaluatedPolicyRule> rules = new ArrayList<>();
		collectFocusRulesFromAssignments(rules, context);
		collectGlobalFocusRules(rules, context, task, result);

		for (EvaluatedPolicyRule rule : rules) {
			if (!hasSituationConstraint(rule)) {
				evaluateFocusRule(rule, context, task, result);
			}
		}
		for (EvaluatedPolicyRule rule : rules) {
			if (hasSituationConstraint(rule)) {
				evaluateFocusRule(rule, context, task, result);
			}
		}
	}

	private <F extends FocusType> void evaluateFocusRule(EvaluatedPolicyRule rule, LensContext<F> context, Task task, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		context.getFocusContext().addPolicyRule(rule);
		evaluateRule(new FocusPolicyRuleEvaluationContext<>(rule, context, task), result);
	}

	private <F extends FocusType> void collectFocusRulesFromAssignments(List<EvaluatedPolicyRule> rules, LensContext<F> context) {
		DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
		if (evaluatedAssignmentTriple == null) {
			return;
		}
		for (EvaluatedAssignmentImpl<?> evaluatedAssignment : evaluatedAssignmentTriple.getNonNegativeValues()) {
			rules.addAll(evaluatedAssignment.getFocusPolicyRules());
		}
	}

	private <F extends FocusType> void collectGlobalFocusRules(List<EvaluatedPolicyRule> rules, LensContext<F> context,
			Task task, OperationResult result)
			throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException {
		PrismObject<SystemConfigurationType> systemConfiguration = context.getSystemConfiguration();
		if (systemConfiguration == null) {
			return;
		}
		LensFocusContext<F> focusContext = context.getFocusContext();

		// We select rules based on current object state, not the new one.
		// (e.g. because some of the constraints might be modification constraints)
		PrismObject<F> focus = focusContext.getObjectCurrent();
		if (focus == null) {
			focus = focusContext.getObjectNew();
		}
		for (GlobalPolicyRuleType globalPolicyRule: systemConfiguration.asObjectable().getGlobalPolicyRule()) {
			ObjectSelectorType focusSelector = globalPolicyRule.getFocusSelector();
			if (repositoryService.selectorMatches(focusSelector, focus, LOGGER, "Global policy rule "+globalPolicyRule.getName()+": ")) {
				if (!isRuleConditionTrue(globalPolicyRule, focus, null, context, task, result)) {
					LOGGER.trace("Skipping global policy rule because the condition evaluated to false: {}", globalPolicyRule);
					continue;
				}
				rules.add(new EvaluatedPolicyRuleImpl(globalPolicyRule, null));
			}
		}
	}

	//endregion

	//region ------------------------------------------------------------------ All policy rules

	private boolean hasSituationConstraint(EvaluatedPolicyRule policyRule) {
		return hasSituationConstraint(policyRule.getPolicyConstraints());
	}

	private boolean hasSituationConstraint(Collection<PolicyConstraintsType> constraints) {
		return constraints.stream().anyMatch(this::hasSituationConstraint);
	}

	private boolean hasSituationConstraint(PolicyConstraintsType constraints) {
		return constraints != null &&
				(!constraints.getSituation().isEmpty() ||
						hasSituationConstraint(constraints.getAnd()) ||
						hasSituationConstraint(constraints.getOr()) ||
						hasSituationConstraint(constraints.getNot()));
	}

	/**
	 * Evaluates given policy rule in a given context.
	 */
	private <F extends FocusType> void evaluateRule(PolicyRuleEvaluationContext<F> ctx, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		if (evaluateConstraints(ctx.policyRule.getPolicyConstraints(), true, ctx, result)) {
			ctx.triggerRule();
		}
	}

	// returns true if the constraints evaluated to true (if allMustApply, all of the constraints must apply; otherwise, at least one must apply)
	@SuppressWarnings("unchecked")
	private <F extends FocusType> boolean evaluateConstraints(PolicyConstraintsType constraints,
			boolean allMustApply, PolicyRuleEvaluationContext<F> ctx, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		if (constraints == null) {
			return false;
		}
		boolean atLeastOne = false;
		for (JAXBElement<AbstractPolicyConstraintType> primitiveConstraint : toPrimitiveConstraintsList(constraints)) {
			PolicyConstraintEvaluator<AbstractPolicyConstraintType> evaluator =
					(PolicyConstraintEvaluator<AbstractPolicyConstraintType>) getConstraintEvaluator(primitiveConstraint);
			EvaluatedPolicyRuleTrigger<?> trigger = evaluator.evaluate(primitiveConstraint, ctx, result);
			if (trigger != null) {
				ctx.triggers.add(trigger);
				atLeastOne = true;
			} else {
				if (allMustApply) {
					return false; // constraint that does not apply => skip this rule
				}
			}
		}
		for (PolicyConstraintsType andConstraint : constraints.getAnd()) {
			boolean val = evaluateConstraints(andConstraint, true, ctx, result);
			if (val) {
				atLeastOne = true;
			} else {
				if (allMustApply) {
					return false;
				}
			}
		}
		for (PolicyConstraintsType orConstraint : constraints.getOr()) {
			boolean val = evaluateConstraints(orConstraint, false, ctx, result);
			if (val) {
				atLeastOne = true;
			} else {
				if (allMustApply) {
					return false;
				}
			}
		}
		for (PolicyConstraintsType notConstraint : constraints.getNot()) {
			boolean val = evaluateConstraints(notConstraint, true, ctx, result);
			if (!val) {
				atLeastOne = true;
			} else {
				if (allMustApply) {
					return false;
				}
			}
		}
		return atLeastOne;      // no constraints => never returns true, even in 'all must apply' mode
	}

	private PolicyConstraintEvaluator<?> getConstraintEvaluator(JAXBElement<AbstractPolicyConstraintType> constraint) {
		if (constraint.getValue() instanceof AssignmentPolicyConstraintType) {
			return assignmentConstraintEvaluator;
		} else if (constraint.getValue() instanceof HasAssignmentPolicyConstraintType) {
			return hasAssignmentConstraintEvaluator;
		} else if (constraint.getValue() instanceof ExclusionPolicyConstraintType) {
			return exclusionConstraintEvaluator;
		} else if (constraint.getValue() instanceof MultiplicityPolicyConstraintType) {
			return multiplicityConstraintEvaluator;
		} else if (constraint.getValue() instanceof PolicySituationPolicyConstraintType) {
			return policySituationConstraintEvaluator;
		} else if (constraint.getValue() instanceof ModificationPolicyConstraintType) {
			return modificationConstraintEvaluator;
		} else if (constraint.getValue() instanceof StatePolicyConstraintType) {
			return stateConstraintEvaluator;
		} else {
			throw new IllegalArgumentException("Unknown policy constraint: " + constraint.getName() + "/" + constraint.getValue().getClass());
		}
	}
	//endregion

	//region ------------------------------------------------------------------ Pruning
	public <F extends FocusType> boolean processPruning(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
			OperationResult result) throws PolicyViolationException, SchemaException {
		Collection<EvaluatedAssignmentImpl<F>> plusSet = evaluatedAssignmentTriple.getPlusSet();
		boolean needToReevaluateAssignments = false;
		for (EvaluatedAssignmentImpl<F> plusAssignment: plusSet) {
			for (EvaluatedPolicyRule targetPolicyRule: plusAssignment.getAllTargetsPolicyRules()) {
				for (EvaluatedPolicyRuleTrigger trigger: targetPolicyRule.getTriggers()) {
					if (!(trigger instanceof EvaluatedExclusionTrigger)) {
						continue;
					}
					EvaluatedExclusionTrigger exclTrigger = (EvaluatedExclusionTrigger) trigger;
					PolicyActionsType actions = targetPolicyRule.getActions();
					if (actions == null || actions.getPrune() == null) {
						continue;
					}
					EvaluatedAssignment<FocusType> conflictingAssignment = exclTrigger.getConflictingAssignment();
					if (conflictingAssignment == null) {
						throw new SystemException("Added assignment "+plusAssignment
								+", the exclusion prune rule was triggered but there is no conflicting assignment in the trigger");
					}
					LOGGER.debug("Pruning assignment {} because it conflicts with added assignment {}", conflictingAssignment, plusAssignment);
					
					PrismContainerValue<AssignmentType> assignmentValueToRemove = conflictingAssignment.getAssignmentType().asPrismContainerValue().clone();
					PrismObjectDefinition<F> focusDef = context.getFocusContext().getObjectDefinition();
					ContainerDelta<AssignmentType> assignmentDelta = ContainerDelta.createDelta(FocusType.F_ASSIGNMENT, focusDef);
					assignmentDelta.addValuesToDelete(assignmentValueToRemove);
					context.getFocusContext().swallowToSecondaryDelta(assignmentDelta);
					
					needToReevaluateAssignments = true;
				}
			}
		}
		
		return needToReevaluateAssignments;
	}
	//endregion

	//region ------------------------------------------------------------------ Global policy rules

	public <F extends FocusType> void addGlobalPolicyRulesToAssignments(LensContext<F> context,
			DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple, Task task, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		PrismObject<SystemConfigurationType> systemConfiguration = context.getSystemConfiguration();
		if (systemConfiguration == null) {
			return;
		}
		// We need to consider object before modification here.
		LensFocusContext<F> focusContext = context.getFocusContext();
		PrismObject<F> focus = focusContext.getObjectCurrent();
		if (focus == null) {
			focus = focusContext.getObjectNew();		// only if it does not exist, let's try the new one
		}

		for (GlobalPolicyRuleType globalPolicyRule: systemConfiguration.asObjectable().getGlobalPolicyRule()) {
			ObjectSelectorType focusSelector = globalPolicyRule.getFocusSelector();
			if (!repositoryService.selectorMatches(focusSelector, focus, LOGGER,
					"Global policy rule "+globalPolicyRule.getName()+" focus selector: ")) {
				continue;
			}
			for (EvaluatedAssignmentImpl<F> evaluatedAssignment : evaluatedAssignmentTriple.getAllValues()) {
				for (EvaluatedAssignmentTargetImpl target : evaluatedAssignment.getRoles().getNonNegativeValues()) {
					if (!target.getAssignmentPath().last().isMatchingOrder() && !target.isDirectlyAssigned()) {
						// This is to be thought out well. It is of no use to include global policy rules
						// attached to meta-roles assigned to the role being assigned to the focus. But we certainly need to include rules
						// attached to all directly assigned roles (because they might be considered for assignment) as
						// well as all indirectly assigned roles but of the matching order (because of exclusion violation).
						continue;
					}
					if (!repositoryService.selectorMatches(globalPolicyRule.getTargetSelector(),
							target.getTarget(), LOGGER, "Global policy rule "+globalPolicyRule.getName()+" target selector: ")) {
						continue;
					}
					if (!isRuleConditionTrue(globalPolicyRule, focus, evaluatedAssignment, context, task, result)) {
						LOGGER.trace("Skipping global policy rule because the condition evaluated to false: {}", globalPolicyRule);
						continue;
					}
					EvaluatedPolicyRule evaluatedRule = new EvaluatedPolicyRuleImpl(globalPolicyRule, target.getAssignmentPath().clone());
					boolean direct = target.isDirectlyAssigned() || target.getAssignmentPath().getFirstOrderChain().size() == 1;
					if (direct) {
						evaluatedAssignment.addThisTargetPolicyRule(evaluatedRule);
					} else {
						evaluatedAssignment.addOtherTargetPolicyRule(evaluatedRule);
					}
				}
			}
		}
	}

	private <F extends FocusType> boolean isRuleConditionTrue(GlobalPolicyRuleType globalPolicyRule, PrismObject<F> focus,
			EvaluatedAssignmentImpl<F> evaluatedAssignment, LensContext<F> context, Task task, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		MappingType condition = globalPolicyRule.getCondition();
		if (condition == null) {
			return true;
		}

		Mapping.Builder<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> builder = mappingFactory
				.createMappingBuilder();
		ObjectDeltaObject<F> focusOdo = new ObjectDeltaObject<>(focus, null, focus);
		builder = builder.mappingType(condition)
				.contextDescription("condition in global policy rule " + globalPolicyRule.getName())
				.sourceContext(focusOdo)
				.defaultTargetDefinition(
						new PrismPropertyDefinitionImpl<>(CONDITION_OUTPUT_NAME, DOMUtil.XSD_BOOLEAN, prismContext))
				.addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo)
				.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo)
				.addVariableDefinition(ExpressionConstants.VAR_TARGET, evaluatedAssignment != null ? evaluatedAssignment.getTarget() : null)
				.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, evaluatedAssignment)                // TODO: ok?
				.rootNode(focusOdo);

		Mapping<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

		mappingEvaluator.evaluateMapping(mapping, context, task, result);

		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTriple = mapping.getOutputTriple();
		return conditionTriple != null && ExpressionUtil.computeConditionResult(conditionTriple.getNonNegativeValues());	// TODO: null -> true (in the method) - ok?
	}
	//endregion

}
