/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentTargetImpl;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators.*;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.task.api.Task;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil.toConstraintsList;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType.F_AND;

/**
 * @author semancik
 */
@Component
@ProcessorExecution(focusRequired = true, focusType = AssignmentHolderType.class)
public class PolicyRuleProcessor implements ProjectorProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleProcessor.class);

    private static final String OP_EVALUATE_RULE = PolicyRuleProcessor.class.getName() + ".evaluateRule";

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
    @Autowired private AlwaysTrueConstraintEvaluator alwaysTrueConstraintEvaluator;
    @Autowired private OrphanedConstraintEvaluator orphanedConstraintEvaluator;
    @Autowired private CompositeConstraintEvaluator compositeConstraintEvaluator;
    @Autowired private TransitionConstraintEvaluator transitionConstraintEvaluator;
    @Autowired private ExpressionFactory expressionFactory;

    //region ------------------------------------------------------------------ Assignment policy rules
    /**
     * Evaluates the policies (policy rules, but also legacy policies). Triggers the rules.
     * But does not enforce anything and does not make any context changes. TODO really? also for legacy policies?
     *
     * Takes into account all policy rules related to assignments in the given evaluatedAssignmentTriple.
     * Focus policy rules are not processed here, even though they might come through these assignments.
     */
    public <F extends AssignmentHolderType> void evaluateAssignmentPolicyRules(LensContext<F> context,
            DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple,
            Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        for (EvaluatedAssignmentImpl<F> evaluatedAssignment : evaluatedAssignmentTriple.union()) {
            RulesEvaluationContext globalCtx = new RulesEvaluationContext();

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
                                evaluatedAssignment, true, context,
                                evaluatedAssignmentTriple, task, globalCtx), result);
                    }
                }
            }
            for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getOtherTargetsPolicyRules()) {
                if (!hasSituationConstraint(policyRule)) {
                    if (checkApplicabilityToAssignment(policyRule)) {
                        evaluateRule(new AssignmentPolicyRuleEvaluationContext<>(policyRule,
                                evaluatedAssignment, false, context,
                                evaluatedAssignmentTriple, task, globalCtx), result);
                    }
                }
            }
            for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getThisTargetPolicyRules()) {
                if (hasSituationConstraint(policyRule)) {
                    if (checkApplicabilityToAssignment(policyRule)) {
                        evaluateRule(new AssignmentPolicyRuleEvaluationContext<>(policyRule,
                                evaluatedAssignment, true, context,
                                evaluatedAssignmentTriple, task, globalCtx), result);
                    }
                }
            }
            for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getOtherTargetsPolicyRules()) {
                if (hasSituationConstraint(policyRule)) {
                    if (checkApplicabilityToAssignment(policyRule)) {
                        evaluateRule(new AssignmentPolicyRuleEvaluationContext<>(policyRule,
                                evaluatedAssignment, false, context,
                                evaluatedAssignmentTriple, task, globalCtx), result);
                    }
                }
            }
            // a bit of hack, but hopefully it will work
            PlusMinusZero mode = evaluatedAssignment.getOrigin().isBeingDeleted() ? PlusMinusZero.MINUS : evaluatedAssignment.getMode();

            // FIXME: What if the assignments are reevaluated because of pruning? The state changes are already recorded
            //  into the focus context. See MID-7123.
            policyStateRecorder.applyAssignmentState(context, evaluatedAssignment, mode, globalCtx.rulesToRecord);
        }
    }

    private boolean checkApplicabilityToAssignment(EvaluatedPolicyRule policyRule) {
        if (isApplicableToAssignment(policyRule)) {
            return true;
        } else {
            LOGGER.trace("Skipping rule {} because it is not applicable to assignment: {}", policyRule.getName(), policyRule);
            return false;
        }
    }

    private void resolveReferences(Collection<? extends EvaluatedPolicyRule> evaluatedRules,
            Collection<? extends PolicyRuleType> otherRules) {
        List<PolicyRuleType> rules = evaluatedRules.stream()
                .map(EvaluatedPolicyRule::getPolicyRule)
                .collect(Collectors.toList());
        PolicyRuleTypeUtil.resolveReferences(rules, otherRules, prismContext);
    }

    //endregion

    //region ------------------------------------------------------------------ Focus policy rules
    @ProcessorMethod
    public <AH extends AssignmentHolderType> void evaluateObjectPolicyRules(LensContext<AH> context,
            @SuppressWarnings("unused") XMLGregorianCalendar now, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException {
        RulesEvaluationContext globalCtx = new RulesEvaluationContext();

        List<EvaluatedPolicyRuleImpl> rules = new ArrayList<>();
        collectFocusRulesFromAssignments(rules, context);
        collectGlobalObjectRules(rules, context, task, result);

        resolveReferences(rules, getAllGlobalRules(context));

        List<EvaluatedPolicyRule> situationRules = new ArrayList<>();
        List<EvaluatedPolicyRule> nonSituationRules = new ArrayList<>();

        LOGGER.trace("Evaluating {} object policy rules", rules.size());
        LensFocusContext<AH> focusContext = context.getFocusContext();
        focusContext.clearObjectPolicyRules();
        for (EvaluatedPolicyRuleImpl rule : rules) {
            if (isApplicableToObject(rule)) {
                if (hasSituationConstraint(rule)) {
                    situationRules.add(rule);
                } else {
                    nonSituationRules.add(rule);
                }
                focusContext.addObjectPolicyRule(rule);
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

    private <AH extends AssignmentHolderType> Collection<? extends PolicyRuleType> getAllGlobalRules(LensContext<AH> context) {
        return context.getSystemConfiguration() != null
                ? CloneUtil.clone(context.getSystemConfiguration().asObjectable().getGlobalPolicyRule())
                : Collections.emptyList();
    }

    private <AH extends AssignmentHolderType> void evaluateFocusRule(EvaluatedPolicyRule rule, LensContext<AH> context,
            RulesEvaluationContext globalCtx, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        evaluateRule(new ObjectPolicyRuleEvaluationContext<>(rule, globalCtx, context, task), result);
    }

    private <AH extends AssignmentHolderType> void collectFocusRulesFromAssignments(List<EvaluatedPolicyRuleImpl> rules, LensContext<AH> context) {
        // We intentionally evaluate rules also from negative (deleted) assignments.
        for (EvaluatedAssignmentImpl<?> evaluatedAssignment : context.getAllEvaluatedAssignments()) {
            rules.addAll(evaluatedAssignment.getFocusPolicyRules());
        }
    }

    private <AH extends AssignmentHolderType> void collectGlobalObjectRules(List<EvaluatedPolicyRuleImpl> rules, LensContext<AH> context,
            Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException {
        PrismObject<SystemConfigurationType> systemConfiguration = context.getSystemConfiguration();
        if (systemConfiguration == null) {
            return;
        }
        LensFocusContext<AH> focusContext = context.getFocusContext();

        // We select rules based on current object state, not the new one.
        // (e.g. because some of the constraints might be modification constraints)
        PrismObject<AH> focus = focusContext.getObjectCurrent();
        if (focus == null) {
            focus = focusContext.getObjectNew();
        } if (focus == null && focusContext.isDeleted()) {
            // This is a very special case, where the focus was deleted.
            focus = focusContext.getObjectOld();
        }
        List<GlobalPolicyRuleType> globalPolicyRuleList = systemConfiguration.asObjectable().getGlobalPolicyRule();
        LOGGER.trace("Checking {} global policy rules", globalPolicyRuleList.size());
        int globalRulesFound = 0;
        for (GlobalPolicyRuleType globalPolicyRule: globalPolicyRuleList) {
            ObjectSelectorType focusSelector = globalPolicyRule.getFocusSelector();
            if (repositoryService.selectorMatches(focusSelector, focus, null, LOGGER, "Global policy rule "+globalPolicyRule.getName()+": ")) {
                if (isRuleConditionTrue(globalPolicyRule, focus, null, context, task, result)) {
                    String ruleId = PolicyRuleTypeUtil.createId(systemConfiguration.getOid(), globalPolicyRule.getId());
                    rules.add(new EvaluatedPolicyRuleImpl(globalPolicyRule.clone(), ruleId, null, null));
                    globalRulesFound++;
                } else {
                    LOGGER.trace("Skipping global policy rule {} because the condition evaluated to false: {}", globalPolicyRule.getName(), globalPolicyRule);
                }
            } else {
                LOGGER.trace("Skipping global policy rule {} because the selector did not match: {}", globalPolicyRule.getName(), globalPolicyRule);
            }
        }
        LOGGER.trace("Selected {} global policy rules for further evaluation", globalRulesFound);
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
    private <AH extends AssignmentHolderType> void evaluateRule(@NotNull PolicyRuleEvaluationContext<AH> ctx,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        String ruleShortString = ctx.policyRule.toShortString();
        String ctxShortDescription = ctx.getShortDescription();
        OperationResult result = parentResult.subresult(OP_EVALUATE_RULE)
                .addParam("policyRule", ruleShortString)
                .addContext("context", ctxShortDescription)
                .build();
        try {
            LOGGER.trace("Evaluating policy rule {} in {}", ruleShortString, ctxShortDescription);
            PolicyConstraintsType constraints = ctx.policyRule.getPolicyConstraints();
            JAXBElement<PolicyConstraintsType> conjunction = new JAXBElement<>(F_AND, PolicyConstraintsType.class, constraints);
            EvaluatedCompositeTrigger trigger = compositeConstraintEvaluator.evaluate(conjunction, ctx, result);
            LOGGER.trace("Evaluated composite trigger {} for ctx {}", trigger, ctx);
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
            boolean triggered = ctx.policyRule.isTriggered();
            LOGGER.trace("Policy rule triggered: {}", triggered);
            result.addReturn("triggered", triggered);
            if (triggered) {
                LOGGER.trace("Start to compute actions");
                ((EvaluatedPolicyRuleImpl) ctx.policyRule)
                        .computeEnabledActions(ctx, ctx.getObject(), expressionFactory, prismContext, ctx.task, result);
                if (ctx.policyRule.containsEnabledAction(RecordPolicyActionType.class)) {
                    ctx.record(); // TODO postpone this (e.g. because of thresholds and also MID-7123)
                }
                result.addArbitraryObjectCollectionAsReturn("enabledActions", ctx.policyRule.getEnabledActions());
            }
            traceRuleEvaluationResult(ctx.policyRule, ctx);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    // returns non-empty list if the constraints evaluated to true (if allMustApply, all of the constraints must apply; otherwise, at least one must apply)
    @SuppressWarnings("unchecked")
    @NotNull
    public <AH extends AssignmentHolderType> List<EvaluatedPolicyRuleTrigger<?>> evaluateConstraints(PolicyConstraintsType constraints,
            boolean allMustApply, PolicyRuleEvaluationContext<AH> ctx, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (constraints == null) {
            return Collections.emptyList();
        }
        List<EvaluatedPolicyRuleTrigger<?>> triggers = new ArrayList<>();
        for (JAXBElement<AbstractPolicyConstraintType> constraint : toConstraintsList(constraints, false, false)) {
            PolicyConstraintEvaluator<AbstractPolicyConstraintType> evaluator =
                    (PolicyConstraintEvaluator<AbstractPolicyConstraintType>) getConstraintEvaluator(constraint);
            EvaluatedPolicyRuleTrigger<?> trigger = evaluator.evaluate(constraint, ctx, result);
            LOGGER.trace("Evaluated policy rule trigger: {}", trigger);
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

    private <AH extends AssignmentHolderType> void traceConstraintEvaluationResult(JAXBElement<AbstractPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<AH> ctx, EvaluatedPolicyRuleTrigger<?> trigger) throws SchemaException {
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
        LOGGER.trace("{}", sb);
    }

    private <AH extends AssignmentHolderType> void traceRuleEvaluationResult(EvaluatedPolicyRule rule, PolicyRuleEvaluationContext<AH> ctx) {
        if (LOGGER.isTraceEnabled()) {
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
            LOGGER.trace("{}", sb);
        }
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
        } else if (constraint.getValue() instanceof AlwaysTruePolicyConstraintType) {
            return alwaysTrueConstraintEvaluator;
        } else if (constraint.getValue() instanceof OrphanedPolicyConstraintType) {
            return orphanedConstraintEvaluator;
        } else {
            throw new IllegalArgumentException("Unknown policy constraint: " + constraint.getName() + "/" + constraint.getValue().getClass());
        }
    }
    //endregion

    //region ------------------------------------------------------------------ Global policy rules

    public <F extends AssignmentHolderType> void addGlobalPolicyRulesToAssignments(LensContext<F> context,
            DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException,
            ConfigurationException, CommunicationException {

        PrismObject<SystemConfigurationType> systemConfiguration = context.getSystemConfiguration();
        if (systemConfiguration == null) {
            return;
        }
        // We need to consider object before modification here.
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext.isDelete()) {
            // TODO ok?
            LOGGER.trace("Focus is being deleted => assignments-related global policy rules are ignored");
            return;
        }

        PrismObject<F> focus = Objects.requireNonNull(
                focusContext.getObjectCurrentOrNew(),
                "no current nor new focus while operation is not DELETE");

        List<GlobalPolicyRuleType> globalPolicyRuleList = systemConfiguration.asObjectable().getGlobalPolicyRule();
        LOGGER.trace("Checking {} global policy rules for selection to assignments", globalPolicyRuleList.size());
        int globalRulesInstantiated = 0;
        for (GlobalPolicyRuleType globalPolicyRule: globalPolicyRuleList) {
            ObjectSelectorType focusSelector = globalPolicyRule.getFocusSelector();
            if (!repositoryService.selectorMatches(focusSelector, focus, null, LOGGER,
                    "Global policy rule "+globalPolicyRule.getName()+" focus selector: ")) {
                LOGGER.trace("Skipping global policy rule {} because focus selector did not match: {}", globalPolicyRule.getName(), globalPolicyRule);
                continue;
            }
            for (EvaluatedAssignmentImpl<F> evaluatedAssignment : evaluatedAssignmentTriple.getAllValues()) {
                for (EvaluatedAssignmentTargetImpl target : evaluatedAssignment.getRoles().getNonNegativeValues()) { // MID-6403
                    if (!target.getAssignmentPath().last().isMatchingOrder() && !target.isDirectlyAssigned()) {
                        // This is to be thought out well. It is of no use to include global policy rules
                        // attached to meta-roles assigned to the role being assigned to the focus. But we certainly need to include rules
                        // attached to all directly assigned roles (because they might be considered for assignment) as
                        // well as all indirectly assigned roles but of the matching order (because of exclusion violation).
                        continue;
                    }
                    if (!repositoryService.selectorMatches(globalPolicyRule.getTargetSelector(),
                            target.getTarget(), null, LOGGER,
                            "Global policy rule "+globalPolicyRule.getName()+" target selector: ")) {
                        LOGGER.trace("Skipping global policy rule {} because target selector did not match: {}",
                                globalPolicyRule.getName(), globalPolicyRule);
                        continue;
                    }
                    if (!isRuleConditionTrue(globalPolicyRule, focus, evaluatedAssignment, context, task, result)) {
                        LOGGER.trace("Skipping global policy rule {} because the condition evaluated to false: {}",
                                globalPolicyRule.getName(), globalPolicyRule);
                        continue;
                    }
                    String ruleId = PolicyRuleTypeUtil.createId(systemConfiguration.getOid(), globalPolicyRule.getId());
                    EvaluatedPolicyRuleImpl evaluatedRule = new EvaluatedPolicyRuleImpl(globalPolicyRule.clone(), ruleId,
                            target.getAssignmentPath().clone(), evaluatedAssignment);
                    boolean direct = target.isDirectlyAssigned();
                    if (direct) {
                        evaluatedAssignment.addThisTargetPolicyRule(evaluatedRule);
                    } else {
                        evaluatedAssignment.addOtherTargetPolicyRule(evaluatedRule);
                    }
                    globalRulesInstantiated++;
                }
            }
        }
        LOGGER.trace("Global policy rules instantiated {} times for further evaluation", globalRulesInstantiated);
    }

    private <AH extends AssignmentHolderType> boolean isRuleConditionTrue(
            GlobalPolicyRuleType globalPolicyRule, PrismObject<AH> focus,
            EvaluatedAssignmentImpl<AH> evaluatedAssignment, LensContext<AH> context, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        MappingType condition = globalPolicyRule.getCondition();
        if (condition == null) {
            return true;
        }
        if (!task.canSee(condition)) {
            LOGGER.trace("Condition is not visible for the current task");
            return true;
        }

        MappingBuilder<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> builder = mappingFactory
                .createMappingBuilder();
        ObjectDeltaObject<AH> focusOdo = new ObjectDeltaObject<>(focus, null, focus, focus.getDefinition());
        PrismObject<?> target = evaluatedAssignment != null ? evaluatedAssignment.getTarget() : null;

        builder = builder.mappingBean(condition)
                .mappingKind(MappingKindType.POLICY_RULE_CONDITION)
                .contextDescription("condition in global policy rule " + globalPolicyRule.getName())
                .sourceContext(focusOdo)
                .defaultTargetDefinition(LensUtil.createConditionDefinition(prismContext))
                .addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo)
                .addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo)
                .addAliasRegistration(ExpressionConstants.VAR_USER, null)
                .addAliasRegistration(ExpressionConstants.VAR_FOCUS, null)
                .addVariableDefinition(ExpressionConstants.VAR_TARGET,
                        target, target != null ? target.getDefinition() : getObjectDefinition())
                .addVariableDefinition(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, evaluatedAssignment, EvaluatedAssignment.class)
                .addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT,
                        evaluatedAssignment != null ? evaluatedAssignment.getAssignment() : null, AssignmentType.class)
                .rootNode(focusOdo);

        MappingImpl<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

        mappingEvaluator.evaluateMapping(mapping, context, task, result);

        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTriple = mapping.getOutputTriple();
        return conditionTriple != null && ExpressionUtil.computeConditionResult(conditionTriple.getNonNegativeValues()); // TODO: null -> true (in the method) - ok?
    }

    private PrismObjectDefinition<?> getObjectDefinition() {
        return PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ObjectType.class);
    }

    //endregion
}
