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

import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil.toConstraintsList;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType.F_AND;

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
	@Autowired private PolicyStateRecorder policyStateRecorder;

	@Autowired private AssignmentModificationConstraintEvaluator assignmentConstraintEvaluator;
	@Autowired private HasAssignmentConstraintEvaluator hasAssignmentConstraintEvaluator;
	@Autowired private ExclusionConstraintEvaluator exclusionConstraintEvaluator;
	@Autowired private MultiplicityConstraintEvaluator multiplicityConstraintEvaluator;
	@Autowired private PolicySituationConstraintEvaluator policySituationConstraintEvaluator;
	@Autowired private ObjectModificationConstraintEvaluator modificationConstraintEvaluator;
	@Autowired private StateConstraintEvaluator stateConstraintEvaluator;
	@Autowired private CompositeConstraintEvaluator compositeConstraintEvaluator;
	@Autowired private TransitionConstraintEvaluator transitionConstraintEvaluator;
	@Autowired private ExpressionFactory expressionFactory;

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
			throws PolicyViolationException, SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

		for (EvaluatedAssignmentImpl<F> evaluatedAssignment : evaluatedAssignmentTriple.union()) {
			RulesEvaluationContext globalCtx = new RulesEvaluationContext();

			boolean inPlus = evaluatedAssignmentTriple.presentInPlusSet(evaluatedAssignment);
			boolean inMinus = evaluatedAssignmentTriple.presentInMinusSet(evaluatedAssignment);
			boolean inZero = evaluatedAssignmentTriple.presentInZeroSet(evaluatedAssignment);

			resolveReferences(evaluatedAssignment.getAllTargetsPolicyRules(), getAllGlobalRules(context));

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
					if (checkApplicabilityToAssignment(policyRule)) {
						evaluateRule(new AssignmentPolicyRuleEvaluationContext<>(policyRule,
								evaluatedAssignment, inPlus, inZero, inMinus, true, context,
								evaluatedAssignmentTriple, task, globalCtx), result);
					}
				}
			}
			for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getOtherTargetsPolicyRules()) {
				if (!hasSituationConstraint(policyRule)) {
					if (checkApplicabilityToAssignment(policyRule)) {
						evaluateRule(new AssignmentPolicyRuleEvaluationContext<>(policyRule,
								evaluatedAssignment, inPlus, inZero, inMinus, false, context,
								evaluatedAssignmentTriple, task, globalCtx), result);
					}
				}
			}
			for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getThisTargetPolicyRules()) {
				if (hasSituationConstraint(policyRule)) {
					if (checkApplicabilityToAssignment(policyRule)) {
						evaluateRule(new AssignmentPolicyRuleEvaluationContext<>(policyRule,
								evaluatedAssignment, inPlus, inZero, inMinus, true, context,
								evaluatedAssignmentTriple, task, globalCtx), result);
					}
				}
			}
			for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getOtherTargetsPolicyRules()) {
				if (hasSituationConstraint(policyRule)) {
					if (checkApplicabilityToAssignment(policyRule)) {
						evaluateRule(new AssignmentPolicyRuleEvaluationContext<>(policyRule,
								evaluatedAssignment, inPlus, inZero, inMinus, false, context,
								evaluatedAssignmentTriple, task, globalCtx), result);
					}
				}
			}
			// a bit of hack, but hopefully it will work
			PlusMinusZero mode = inMinus ? PlusMinusZero.MINUS : evaluatedAssignment.getMode();
			policyStateRecorder.applyAssignmentState(context, evaluatedAssignment, mode, globalCtx.rulesToRecord);
		}

		exclusionConstraintEvaluator.checkExclusionsLegacy(context, evaluatedAssignmentTriple.getPlusSet(),
				evaluatedAssignmentTriple.getNonNegativeValues());
	}

	private boolean checkApplicabilityToAssignment(EvaluatedPolicyRule policyRule) {
		if (isApplicableToAssignment(policyRule)) {
			return true;
		} else {
			LOGGER.trace("Skipping rule {} because it is not applicable to assignment: {}", policyRule.getName(), policyRule);
			return false;
		}
	}

	private void resolveReferences(Collection<EvaluatedPolicyRule> evaluatedRules, Collection<? extends PolicyRuleType> otherRules)
			throws SchemaException, ObjectNotFoundException {
		List<PolicyRuleType> rules = evaluatedRules.stream().map(er -> er.getPolicyRule()).collect(Collectors.toList());
		PolicyRuleTypeUtil.resolveReferences(rules, otherRules, prismContext);
	}

	//endregion

	//region ------------------------------------------------------------------ Focus policy rules
	public <F extends FocusType> void evaluateObjectPolicyRules(LensContext<F> context, String activityDescription,
			XMLGregorianCalendar now, Task task, OperationResult result)
			throws PolicyViolationException, SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null) {
			return;
		}

		RulesEvaluationContext globalCtx = new RulesEvaluationContext();

		List<EvaluatedPolicyRule> rules = new ArrayList<>();
		collectFocusRulesFromAssignments(rules, context);
		collectGlobalObjectRules(rules, context, task, result);

		resolveReferences(rules, getAllGlobalRules(context));

		List<EvaluatedPolicyRule> situationRules = new ArrayList<>();
		List<EvaluatedPolicyRule> nonSituationRules = new ArrayList<>();

		LOGGER.trace("Evaluating {} object policy rules", rules.size());
		focusContext.clearPolicyRules();
		for (EvaluatedPolicyRule rule : rules) {
			if (isApplicableToObject(rule)) {
				if (hasSituationConstraint(rule)) {
					situationRules.add(rule);
				} else {
					nonSituationRules.add(rule);
				}
				focusContext.addPolicyRule(rule);
			} else {
				LOGGER.trace("Rule {} is not applicable to an object, skipping: {}", rule.getName(), rule);
			}
		}

		for (EvaluatedPolicyRule rule : nonSituationRules) {
			evaluateFocusRule(rule, context, globalCtx, task, result);
		}
		for (EvaluatedPolicyRule rule : situationRules) {
			evaluateFocusRule(rule, context, globalCtx, task, result);
		}
		policyStateRecorder.applyObjectState(context, globalCtx.rulesToRecord);
	}

	private <F extends FocusType> Collection<? extends PolicyRuleType> getAllGlobalRules(LensContext<F> context) {
		return context.getSystemConfiguration() != null
				? context.getSystemConfiguration().asObjectable().getGlobalPolicyRule()
				: Collections.emptyList();
	}

	private <F extends FocusType> void evaluateFocusRule(EvaluatedPolicyRule rule, LensContext<F> context,
			RulesEvaluationContext globalCtx, Task task, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		evaluateRule(new ObjectPolicyRuleEvaluationContext<>(rule, globalCtx, context, task), result);
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

	private <F extends FocusType> void collectGlobalObjectRules(List<EvaluatedPolicyRule> rules, LensContext<F> context,
			Task task, OperationResult result)
			throws SchemaException, PolicyViolationException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException {
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
		List<GlobalPolicyRuleType> globalPolicyRuleList = systemConfiguration.asObjectable().getGlobalPolicyRule();
		LOGGER.trace("Checking {} global policy rules", globalPolicyRuleList.size());
		for (GlobalPolicyRuleType globalPolicyRule: globalPolicyRuleList) {
			ObjectSelectorType focusSelector = globalPolicyRule.getFocusSelector();
			if (repositoryService.selectorMatches(focusSelector, focus, null, LOGGER, "Global policy rule "+globalPolicyRule.getName()+": ")) {
				if (isRuleConditionTrue(globalPolicyRule, focus, null, context, task, result)) {
					rules.add(new EvaluatedPolicyRuleImpl(globalPolicyRule, null, prismContext));
				} else {
					LOGGER.trace("Skipping global policy rule {} because the condition evaluated to false: {}", globalPolicyRule.getName(), globalPolicyRule);
				}
			} else {
				LOGGER.trace("Skipping global policy rule {} because the selector did not match: {}", globalPolicyRule.getName(), globalPolicyRule);
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

	private boolean isApplicableToAssignment(EvaluatedPolicyRule rule) {
		return PolicyRuleTypeUtil.isApplicableToAssignment(rule.getPolicyRule());
	}

	private boolean isApplicableToObject(EvaluatedPolicyRule rule) {
		return PolicyRuleTypeUtil.isApplicableToObject(rule.getPolicyRule());
	}

	/**
	 * Evaluates given policy rule in a given context.
	 */
	private <F extends FocusType> void evaluateRule(PolicyRuleEvaluationContext<F> ctx, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Evaluating policy rule {} in {}", ctx.policyRule.toShortString(), ctx.getShortDescription());
		}
		PolicyConstraintsType constraints = ctx.policyRule.getPolicyConstraints();
		JAXBElement<PolicyConstraintsType> conjunction = new JAXBElement<>(F_AND, PolicyConstraintsType.class, constraints);
		EvaluatedCompositeTrigger trigger = compositeConstraintEvaluator.evaluate(conjunction, ctx, result);
		if (trigger != null && !trigger.getInnerTriggers().isEmpty()) {
			List<EvaluatedPolicyRuleTrigger<?>> triggers;
			// TODO reconsider this
			if (constraints.getName() == null && constraints.getPresentation() == null) {
				triggers = new ArrayList<>(trigger.getInnerTriggers());
			} else {
				triggers = Collections.singletonList(trigger);
			}
			ctx.triggerRule(triggers);
		}
		if (ctx.policyRule.isTriggered()) {
			((EvaluatedPolicyRuleImpl) ctx.policyRule).computeEnabledActions(ctx, ctx.getObject(), expressionFactory, prismContext, ctx.task, result);
			if (ctx.policyRule.containsEnabledAction(RecordPolicyActionType.class)) {
				ctx.record();
			}
		}
		traceRuleEvaluationResult(ctx.policyRule, ctx);
	}

	// returns non-empty list if the constraints evaluated to true (if allMustApply, all of the constraints must apply; otherwise, at least one must apply)
	@SuppressWarnings("unchecked")
	@NotNull
	public <F extends FocusType> List<EvaluatedPolicyRuleTrigger<?>> evaluateConstraints(PolicyConstraintsType constraints,
			boolean allMustApply, PolicyRuleEvaluationContext<F> ctx, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		if (constraints == null) {
			return Collections.emptyList();
		}
		List<EvaluatedPolicyRuleTrigger<?>> triggers = new ArrayList<>();
		for (JAXBElement<AbstractPolicyConstraintType> constraint : toConstraintsList(constraints, false, false)) {
			PolicyConstraintEvaluator<AbstractPolicyConstraintType> evaluator =
					(PolicyConstraintEvaluator<AbstractPolicyConstraintType>) getConstraintEvaluator(constraint);
			EvaluatedPolicyRuleTrigger<?> trigger = evaluator.evaluate(constraint, ctx, result);
			traceConstraintEvaluationResult(constraint, ctx, trigger);
			if (trigger != null) {
				triggers.add(trigger);
			} else {
				if (allMustApply) {
					return Collections.emptyList(); // constraint that does not apply => skip this rule
				}
			}
		}
		return triggers;
	}

	private <F extends FocusType> void traceConstraintEvaluationResult(JAXBElement<AbstractPolicyConstraintType> constraintElement,
			PolicyRuleEvaluationContext<F> ctx, EvaluatedPolicyRuleTrigger<?> trigger) throws SchemaException {
		if (!LOGGER.isTraceEnabled()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("\n---[ POLICY CONSTRAINT ");
		if (trigger != null) {
			sb.append("# ");
		}
		AbstractPolicyConstraintType constraint = constraintElement.getValue();
		if (constraint.getName() != null) {
			sb.append("'").append(constraint.getName()).append("'");
		}
		sb.append(" (").append(constraintElement.getName().getLocalPart()).append(")");
		sb.append(" for ");
		sb.append(ctx.getShortDescription());
		sb.append(" (").append(ctx.lensContext.getState()).append(")");
		sb.append("]---------------------------");
		sb.append("\nConstraint:\n");
		sb.append(prismContext.serializerFor(DebugUtil.getPrettyPrintBeansAs(PrismContext.LANG_XML)).serialize(constraintElement));
		//sb.append("\nContext: ").append(ctx.debugDump());
		sb.append("\nRule: ").append(ctx.policyRule.toShortString());
		//sb.append("\nResult: ").append(trigger != null ? DebugUtil.debugDump(trigger) : null);  // distinction is here because debugDump(null) returns "  null"
		sb.append("\nResult: ").append(DebugUtil.debugDump(trigger));
		LOGGER.trace("{}", sb.toString());
	}

	private <F extends FocusType> void traceRuleEvaluationResult(EvaluatedPolicyRule rule, PolicyRuleEvaluationContext<F> ctx)
			throws SchemaException {
		if (!LOGGER.isTraceEnabled()) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("\n---[ POLICY RULE ");
		if (rule.isTriggered()) {
			sb.append("# ");
		}
		sb.append(rule.toShortString());
		sb.append(" for ");
		sb.append(ctx.getShortDescription());
		sb.append(" (").append(ctx.lensContext.getState()).append(")");
		sb.append("]---------------------------");
		sb.append("\n");
		sb.append(rule.debugDump());
		//sb.append("\nContext: ").append(ctx.debugDump());
		LOGGER.trace("{}", sb.toString());
	}

	private PolicyConstraintEvaluator<?> getConstraintEvaluator(JAXBElement<AbstractPolicyConstraintType> constraint) {
		if (constraint.getValue() instanceof AssignmentModificationPolicyConstraintType) {
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
		} else if (constraint.getValue() instanceof PolicyConstraintsType) {
			return compositeConstraintEvaluator;
		} else if (constraint.getValue() instanceof TransitionPolicyConstraintType) {
			return transitionConstraintEvaluator;
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
					if (!targetPolicyRule.containsEnabledAction(PrunePolicyActionType.class)) {
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
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException {

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

		List<GlobalPolicyRuleType> globalPolicyRuleList = systemConfiguration.asObjectable().getGlobalPolicyRule();
		LOGGER.trace("Checking {} global policy rules for selection to assignments", globalPolicyRuleList.size());
		for (GlobalPolicyRuleType globalPolicyRule: systemConfiguration.asObjectable().getGlobalPolicyRule()) {
			ObjectSelectorType focusSelector = globalPolicyRule.getFocusSelector();
			if (!repositoryService.selectorMatches(focusSelector, focus, null, LOGGER,
					"Global policy rule "+globalPolicyRule.getName()+" focus selector: ")) {
				LOGGER.trace("Skipping global policy rule {} because focus selector did not match: {}", globalPolicyRule.getName(), globalPolicyRule);
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
							target.getTarget(), null, LOGGER, "Global policy rule "+globalPolicyRule.getName()+" target selector: ")) {
						LOGGER.trace("Skipping global policy rule {} because target selector did not match: {}", globalPolicyRule.getName(), globalPolicyRule);
						continue;
					}
					if (!isRuleConditionTrue(globalPolicyRule, focus, evaluatedAssignment, context, task, result)) {
						LOGGER.trace("Skipping global policy rule {} because the condition evaluated to false: {}", globalPolicyRule.getName(), globalPolicyRule);
						continue;
					}
					EvaluatedPolicyRule evaluatedRule = new EvaluatedPolicyRuleImpl(globalPolicyRule, target.getAssignmentPath().clone(), prismContext);
					boolean direct = target.isDirectlyAssigned();
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
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
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
				.addVariableDefinition(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, evaluatedAssignment)
				.addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, evaluatedAssignment != null ? evaluatedAssignment.getAssignmentType() : null)
				.rootNode(focusOdo);

		Mapping<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

		mappingEvaluator.evaluateMapping(mapping, context, task, result);

		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTriple = mapping.getOutputTriple();
		return conditionTriple != null && ExpressionUtil.computeConditionResult(conditionTriple.getNonNegativeValues());	// TODO: null -> true (in the method) - ok?
	}
	//endregion

}
