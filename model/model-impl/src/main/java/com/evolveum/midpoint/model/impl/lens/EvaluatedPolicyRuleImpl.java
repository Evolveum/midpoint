/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.api.util.EvaluatedPolicyRuleUtil;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectState;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.schema.config.AbstractPolicyRuleConfigItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.ExpressionConfigItem;
import com.evolveum.midpoint.schema.config.PolicyActionConfigItem;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.constants.ExpressionConstants.VAR_RULE_EVALUATION_CONTEXT;
import static com.evolveum.midpoint.util.DebugUtil.*;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * @author semancik
 *
 */
public class EvaluatedPolicyRuleImpl implements EvaluatedPolicyRule, AssociatedPolicyRule {
    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(EvaluatedPolicyRuleImpl.class);

    @NotNull private final AbstractPolicyRuleConfigItem<?> policyRuleCI;
    @NotNull private final PolicyRuleType policyRuleBean;

    @NotNull private final Collection<EvaluatedPolicyRuleTrigger<?>> triggers = new ArrayList<>();

    /**
     * Information about exact place where the rule was found. This can be important for rules that are
     * indirectly attached to an assignment.
     *
     * An example: Let Engineer induce Employee which conflicts with Contractor. The SoD rule is attached
     * to Employee. But let the user have assignments for Engineer and Contractor only. When evaluating
     * Engineer assignment, we find a (indirectly attached) SoD rule. But we need to know it came from Employee.
     * This is what assignmentPath (Engineer->Employee->(maybe some metarole)->rule) and directOwner (Employee) are for.
     *
     * For global policy rules, assignmentPath is the path to the target object that matched global policy rule.
     *
     * See also {@link #targetType}.
     *
     * It can null for artificially-created policy rules e.g. in task validity cases. To be reviewed.
     */
    @Nullable private final AssignmentPath assignmentPath;

    /**
     * See {@link EvaluatedPolicyRule#getEvaluatedAssignment()}.
     */
    private final EvaluatedAssignmentImpl<?> evaluatedAssignment;

    /** See {@link EvaluatedPolicyRule#getTargetType()}. */
    @NotNull private final TargetType targetType;

    /** Tries to uniquely identify the policy rule. Used e.g. for threshold counters. */
    @NotNull private final String ruleId;

    private int count;

    /**
     * Set to `true` after {@link #enabledActions} are computed.
     * See {@link #computeEnabledActions(PolicyRuleEvaluationContext, PrismObject, Task, OperationResult)}.
     */
    private boolean enabledActionsComputed;

    /** True if evaluated. TODO deduplicate with {@link #enabledActionsComputed}. */
    private boolean evaluated;

    /**
     * Filled in only if {@link #enabledActionsComputed} is `true`.
     * Therefore, please use {@link #getEnabledActions()} to make sure the value returned is valid.
     */
    @NotNull private final List<PolicyActionConfigItem<?>> enabledActions = new ArrayList<>();

    public EvaluatedPolicyRuleImpl(
            @NotNull AbstractPolicyRuleConfigItem<?> policyRuleCI,
            @NotNull String ruleId,
            @Nullable AssignmentPath assignmentPath,
            @NotNull TargetType targetType) {
        this(policyRuleCI, ruleId, assignmentPath, null, targetType);
    }

    public EvaluatedPolicyRuleImpl(
            @NotNull AbstractPolicyRuleConfigItem<?> policyRuleCI,
            @NotNull String ruleId,
            @Nullable AssignmentPath assignmentPath,
            @Nullable EvaluatedAssignmentImpl<?> evaluatedAssignment,
            @NotNull TargetType targetType) {
        this.policyRuleCI = policyRuleCI;
        this.policyRuleBean = policyRuleCI.value();
        this.ruleId = ruleId;
        this.assignmentPath = assignmentPath;
        this.evaluatedAssignment = evaluatedAssignment;
        this.targetType = targetType;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public EvaluatedPolicyRuleImpl clone() {
        return new EvaluatedPolicyRuleImpl(
                CloneUtil.cloneCloneable(policyRuleCI),
                ruleId,
                CloneUtil.cloneCloneable(assignmentPath),
                evaluatedAssignment,
                targetType);
    }

    @Override
    public String getName() {
        return policyRuleCI.getName();
    }

    @Override
    public @NotNull PolicyRuleType getPolicyRule() {
        return policyRuleCI.value();
    }

    public @NotNull ConfigurationItemOrigin getRuleOrigin() {
        return policyRuleCI.origin();
    }

    @Nullable
    @Override
    public AssignmentPath getAssignmentPath() {
        return assignmentPath;
    }

    public @NotNull AssignmentPath getAssignmentPathRequired() {
        return MiscUtil.requireNonNull(
                assignmentPath,
                () -> new IllegalStateException("No assignment path in " + this));
    }

    @Nullable
    @Override
    public EvaluatedAssignmentImpl<?> getEvaluatedAssignment() {
        return evaluatedAssignment;
    }

    @Override
    public PolicyConstraintsType getPolicyConstraints() {
        return policyRuleBean.getPolicyConstraints();
    }

    @Override
    public PolicyThresholdType getPolicyThreshold() {
        return policyRuleBean.getPolicyThreshold();
    }

    @NotNull
    @Override
    public Collection<EvaluatedPolicyRuleTrigger<?>> getTriggers() {
        return triggers;
    }

    @Override
    public boolean isTriggered() {
        return !getTriggers().isEmpty();
    }

    @NotNull
    @Override
    public Collection<EvaluatedPolicyRuleTrigger<?>> getAllTriggers() {
        List<EvaluatedPolicyRuleTrigger<?>> rv = new ArrayList<>();
        for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
            if (trigger instanceof EvaluatedSituationTrigger) {
                rv.addAll(((EvaluatedSituationTrigger) trigger).getAllTriggers());
            } else {
                rv.add(trigger);
            }
        }
        return rv;
    }

    @Override
    public @NotNull Collection<EvaluatedExclusionTrigger> getRelevantExclusionTriggers() {
        // All triggers here are relevant
        return getAllTriggers(EvaluatedExclusionTrigger.class);
    }

    @Override
    public <T extends EvaluatedPolicyRuleTrigger<?>> Collection<T> getAllTriggers(Class<T> type) {
        List<T> selectedTriggers = new ArrayList<>();
        collectTriggers(selectedTriggers, getAllTriggers(), type);
        return selectedTriggers;
    }

    private <T extends EvaluatedPolicyRuleTrigger<?>> void collectTriggers(Collection<T> collected,
            Collection<EvaluatedPolicyRuleTrigger<?>> all, Class<T> type) {
        for (EvaluatedPolicyRuleTrigger<?> trigger : all) {
            if (type.isAssignableFrom(trigger.getClass())) {
                //noinspection unchecked
                collected.add((T) trigger);
            }
            if (trigger instanceof EvaluatedCompositeTrigger compositeTrigger) {
                if (compositeTrigger.getConstraintKind() != PolicyConstraintKindType.NOT) {
                    collectTriggers(collected, compositeTrigger.getInnerTriggers(), type);
                } else {
                    // there is no use in collecting "negated" triggers
                }
            }
        }
    }

    public void trigger(Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        String ruleName = getName();
        LOGGER.debug("Policy rule {} triggered: {}", ruleName, triggers);
        LOGGER.trace("Policy rule {} triggered:\n{}", ruleName, DebugUtil.debugDumpLazily(triggers, 1));
        this.triggers.addAll(triggers);
    }

    @Override
    public void addTrigger(@NotNull EvaluatedPolicyRuleTrigger<?> trigger) {
        triggers.add(trigger);
    }

    @Override
    public PolicyActionsType getActions() {
        return policyRuleBean.getPolicyActions();
    }

    // TODO rewrite this method
    @Override
    public @Nullable String getPolicySituation() {
        // TODO default situations depending on getTriggeredConstraintKinds
        String explicitSituation = policyRuleBean.getPolicySituation();
        if (explicitSituation != null) {
            return explicitSituation;
        }

        if (!triggers.isEmpty()) {
            EvaluatedPolicyRuleTrigger<?> firstTrigger = triggers.iterator().next();
            if (firstTrigger instanceof EvaluatedSituationTrigger) {
                Collection<EvaluatedPolicyRule> sourceRules = ((EvaluatedSituationTrigger) firstTrigger).getSourceRules();
                if (!sourceRules.isEmpty()) {    // should be always the case
                    return sourceRules.iterator().next().getPolicySituation();
                }
            }
            PolicyConstraintKindType constraintKind = firstTrigger.getConstraintKind();
            PredefinedPolicySituation predefinedSituation = PredefinedPolicySituation.get(constraintKind);
            if (predefinedSituation != null) {
                return predefinedSituation.getUrl();
            }
        }

        PolicyConstraintsType policyConstraints = getPolicyConstraints();
        return getSituationFromConstraints(policyConstraints);
    }

    @Override
    public @NotNull List<ObjectReferenceType> getPolicyMarkRef() {
        return policyRuleBean.getMarkRef(); //TODO
    }

    @Nullable
    private String getSituationFromConstraints(PolicyConstraintsType policyConstraints) {
        if (!policyConstraints.getExclusion().isEmpty()) {
            return PredefinedPolicySituation.EXCLUSION_VIOLATION.getUrl();
        } else if (!policyConstraints.getMinAssignees().isEmpty()) {
            return PredefinedPolicySituation.UNDERASSIGNED.getUrl();
        } else if (!policyConstraints.getMaxAssignees().isEmpty()) {
            return PredefinedPolicySituation.OVERASSIGNED.getUrl();
        } else if (!policyConstraints.getModification().isEmpty()) {
            return PredefinedPolicySituation.MODIFIED.getUrl();
        } else if (!policyConstraints.getAssignment().isEmpty()) {
            return PredefinedPolicySituation.ASSIGNMENT_MODIFIED.getUrl();
        } else if (!policyConstraints.getObjectTimeValidity().isEmpty()) {
            return PredefinedPolicySituation.OBJECT_TIME_VALIDITY.getUrl();
        } else if (!policyConstraints.getAssignmentTimeValidity().isEmpty()) {
            return PredefinedPolicySituation.ASSIGNMENT_TIME_VALIDITY.getUrl();
        } else if (!policyConstraints.getHasAssignment().isEmpty()) {
            return PredefinedPolicySituation.HAS_ASSIGNMENT.getUrl();
        } else if (!policyConstraints.getHasNoAssignment().isEmpty()) {
            return PredefinedPolicySituation.HAS_NO_ASSIGNMENT.getUrl();
        } else if (!policyConstraints.getObjectState().isEmpty()) {
            return PredefinedPolicySituation.OBJECT_STATE.getUrl();
        } else if (!policyConstraints.getAssignmentState().isEmpty()) {
            return PredefinedPolicySituation.ASSIGNMENT_STATE.getUrl();
        }
        for (TransitionPolicyConstraintType tc : policyConstraints.getTransition()) {
            String s = getSituationFromConstraints(tc.getConstraints());
            if (s != null) {
                return s;
            }
        }
        for (PolicyConstraintsType subconstraints : policyConstraints.getAnd()) {
            String s = getSituationFromConstraints(subconstraints);
            if (s != null) {
                return s;
            }
        }
        // desperate attempt (might be altogether wrong)
        for (PolicyConstraintsType subconstraints : policyConstraints.getOr()) {
            String s = getSituationFromConstraints(subconstraints);
            if (s != null) {
                return s;
            }
        }
        // "not" will not be used
        return null;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        debugDumpLabelLn(sb, "EvaluatedPolicyRule " + (getName() != null ? getName() + " " : "") + "(triggers: " + triggers.size() + ")", indent);
        debugDumpWithLabelLn(sb, "name", getName(), indent + 1);
        debugDumpLabelLn(sb, "policyRuleType", indent + 1);
        indentDebugDump(sb, indent + 2);
        PrismPrettyPrinter.debugDumpValue(sb, indent + 2, policyRuleCI, PolicyRuleType.COMPLEX_TYPE, PrismContext.LANG_XML);
        sb.append('\n');
        debugDumpWithLabelLn(sb, "assignmentPath", assignmentPath, indent + 1);
        debugDumpWithLabelLn(sb, "triggers", triggers, indent + 1);
        debugDumpWithLabel(sb, "rootObjects", assignmentPath != null ? String.valueOf(assignmentPath.getFirstOrderChain()) : null, indent + 1);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof EvaluatedPolicyRuleImpl that))
            return false;
        return java.util.Objects.equals(policyRuleCI, that.policyRuleCI)
                && Objects.equals(assignmentPath, that.assignmentPath)
                && Objects.equals(triggers, that.triggers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyRuleCI, assignmentPath, triggers);
    }

    @Override
    public String toString() {
        return "EvaluatedPolicyRuleImpl(" + getName() + ")";
    }

    @Override
    public boolean isGlobal() {
        // in the future we might employ special flag for this (if needed)
        return policyRuleBean instanceof GlobalPolicyRuleType;
    }

    @Override
    public String toShortString() {
        StringBuilder sb = new StringBuilder();
        if (isGlobal()) {
            sb.append("G:");
        }
        if (getName() != null) {
            sb.append(getName()).append(":");
        }
        sb.append("(")
                .append(PolicyRuleTypeUtil.toShortString(getPolicyConstraints()))
                .append(")");
        sb.append("->");
        sb.append("(")
                .append(PolicyRuleTypeUtil.toShortString(getActions(), enabledActionsComputed ? enabledActions : null))
                .append(")");
        if (!getTriggers().isEmpty()) {
            sb.append(" # {T:");
            sb.append(
                    getTriggers().stream()
                            .map(EvaluatedPolicyRuleTrigger::toDiagShortcut)
                            .collect(Collectors.joining(", ")));
            sb.append("}");
        }
        return sb.toString();
    }

    @Override
    public List<TreeNode<LocalizableMessage>> extractMessages() {
        return EvaluatedPolicyRuleUtil.extractMessages(triggers, EvaluatedPolicyRuleUtil.MessageKind.NORMAL);
    }

    @Override
    public List<TreeNode<LocalizableMessage>> extractShortMessages() {
        return EvaluatedPolicyRuleUtil.extractMessages(triggers, EvaluatedPolicyRuleUtil.MessageKind.SHORT);
    }

    /**
     * Honors "final" but not "hidden" flag.
     */
    @Override
    public void addToEvaluatedPolicyRuleBeans(
            @NotNull Collection<EvaluatedPolicyRuleType> ruleBeans,
            @NotNull PolicyRuleExternalizationOptions options,
            @Nullable Predicate<EvaluatedPolicyRuleTrigger<?>> triggerSelector,
            @Nullable EvaluatedAssignment newOwner) {
        addToEvaluatedPolicyRuleBeansInternal(ruleBeans, options, triggerSelector, newOwner);
    }

    public void addToEvaluatedPolicyRuleBeansInternal(
            @NotNull Collection<EvaluatedPolicyRuleType> ruleBeans,
            @NotNull PolicyRuleExternalizationOptions options,
            @Nullable Predicate<EvaluatedPolicyRuleTrigger<?>> triggerSelector,
            @Nullable EvaluatedAssignment newOwner) {
        EvaluatedPolicyRuleType bean = new EvaluatedPolicyRuleType();
        bean.setRuleName(getName());
        if (options.isFullStorageStrategy()) {
            if (assignmentPath != null) {
                bean.setAssignmentPath(
                        assignmentPath.toAssignmentPathBean(options.isIncludeAssignmentsContent()));
            }
            ObjectType directOwner = computeDirectOwner();
            if (directOwner != null) {
                bean.setDirectOwnerRef(ObjectTypeUtil.createObjectRef(directOwner));
                bean.setDirectOwnerDisplayName(ObjectTypeUtil.getDisplayName(directOwner));
            }
        }
        for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
            if (triggerSelector != null && !triggerSelector.test(trigger)) {
                continue;
            }
            if (!trigger.isRelevantForNewOwner(newOwner)) {
                continue;
            }
            if (trigger instanceof EvaluatedSituationTrigger && trigger.isHidden()) {
                for (EvaluatedPolicyRule sourceRule : ((EvaluatedSituationTrigger) trigger).getSourceRules()) {
                    ((EvaluatedPolicyRuleImpl) sourceRule).addToEvaluatedPolicyRuleBeansInternal(
                            ruleBeans, options, null, newOwner);
                }
            } else {
                bean.getTrigger().add(
                        trigger.toEvaluatedPolicyRuleTriggerBean(options, newOwner));
            }
        }
        if (bean.getTrigger().isEmpty()) {
            // skip empty situation rule
        } else {
            ruleBeans.add(bean);
        }
    }

    private ObjectType computeDirectOwner() {
        if (assignmentPath == null) {
            return null;
        }
        List<ObjectType> roots = assignmentPath.getFirstOrderChain();
        return roots.isEmpty() ? null : roots.get(roots.size()-1);
    }

    @NotNull
    public List<? extends PolicyActionConfigItem<?>> getEnabledActions() {
        stateCheck(enabledActionsComputed, "Enabled actions are not yet computed in %s", this);
        return enabledActions;
    }

    private VariablesMap createVariablesMap(PolicyRuleEvaluationContext<?> rctx, PrismObject<?> object) {
        VariablesMap var = new VariablesMap();
        PrismContext prismContext = PrismContext.get();
        PrismObjectDefinition<?> definition = rctx != null ? rctx.getObjectDefinition() :
                prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AssignmentHolderType.class);
        var.put(ExpressionConstants.VAR_USER, object, definition);
        var.put(ExpressionConstants.VAR_FOCUS, object, definition);
        var.put(ExpressionConstants.VAR_OBJECT, object, definition);
        if (rctx instanceof AssignmentPolicyRuleEvaluationContext<?> actx) {
            PrismObject<?> target = actx.evaluatedAssignment.getTarget();
            var.put(ExpressionConstants.VAR_TARGET, target, target != null ? target.getDefinition() : getObjectDefinition());
            var.put(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, actx.evaluatedAssignment, EvaluatedAssignment.class);
            AssignmentType assignment = actx.evaluatedAssignment.getAssignment(actx.state == ObjectState.BEFORE);
            var.put(ExpressionConstants.VAR_ASSIGNMENT, assignment, getAssignmentDefinition(assignment));
        } else if (rctx instanceof ObjectPolicyRuleEvaluationContext) {
            var.put(ExpressionConstants.VAR_TARGET, null, getObjectDefinition());
            var.put(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, null, EvaluatedAssignment.class);
            var.put(ExpressionConstants.VAR_ASSIGNMENT, null, getAssignmentDefinition(null));
        } else if (rctx != null) {
            throw new AssertionError(rctx);
        }
        var.put(VAR_RULE_EVALUATION_CONTEXT, rctx, PolicyRuleEvaluationContext.class);
        return var;
    }

    private PrismObjectDefinition<ObjectType> getObjectDefinition() {
        return PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ObjectType.class);
    }

    private static PrismContainerDefinition<?> getAssignmentDefinition(AssignmentType assignment) {
        if (assignment != null) {
            PrismContainerDefinition<?> definition = assignment.asPrismContainerValue().getDefinition();
            if (definition != null) {
                return definition;
            }
        }

        return PrismContext.get().getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(AssignmentHolderType.class)
                .findContainerDefinition(AssignmentHolderType.F_ASSIGNMENT);
    }

    @Override
    public boolean containsEnabledAction() {
        return !getEnabledActions().isEmpty();
    }

    @Override
    public boolean containsEnabledAction(Class<? extends PolicyActionType> type) {
        return !getEnabledActions(type).isEmpty();
    }

    @Override
    public @NotNull <T extends PolicyActionType> List<? extends PolicyActionConfigItem<T>> getEnabledActions(Class<T> type) {
        return PolicyRuleTypeUtil.filterActions(
                getEnabledActions(),
                type);
    }

    @Override
    public <T extends PolicyActionType> PolicyActionConfigItem<T> getEnabledAction(Class<T> type) {
        var actions = getEnabledActions(type);
        return MiscUtil.extractSingleton(
                actions,
                () -> new IllegalStateException("More than one enabled policy action of class " + type + ": " + actions));
    }

    /** Call only after "triggered" status was determined. */
    public void computeEnabledActions(
            @Nullable PolicyRuleEvaluationContext<?> rctx, PrismObject<?> object, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        stateCheck(!enabledActionsComputed, "Enabled actions already computed in %s", this);
        assert enabledActions.isEmpty();

        stateCheck(evaluated, "Rule was not evaluated in %s", this);

        if (!isTriggered()) {
            LOGGER.trace("Skipping computing enabled actions because the rule is not triggered");
            enabledActionsComputed = true;
            return;
        }

        List<PolicyActionConfigItem<?>> allActions = policyRuleCI.getAllActions();
        LOGGER.trace("Computing enabled actions for {}; actions defined: {}", this, allActions);
        for (PolicyActionConfigItem<?> action : allActions) {
            ExpressionConfigItem condition = action.getCondition();
            String actionName = action.getName();
            String actionTypeName = action.getTypeName();
            if (condition != null) {
                VariablesMap variables = createVariablesMap(rctx, object);
                if (!LensExpressionUtil.evaluateBoolean(
                        condition.value(), // TODO full config item
                        variables,
                        rctx != null ? rctx.elementContext : null,
                        "condition in action " + actionName + " (" + actionTypeName + ")",
                        task,
                        result)) {
                    LOGGER.trace("Skipping action {} ({}) because the condition evaluated to false", actionName, actionTypeName);
                    continue;
                } else {
                    LOGGER.trace("Accepting action {} ({}) because the condition evaluated to true", actionName, actionTypeName);
                }
            }
            LOGGER.trace("Adding action {} ({}) into the enabled action list.", action, actionTypeName);
            enabledActions.add(action);
        }
        enabledActionsComputed = true;
    }

    //experimental
    @Override
    public @NotNull String getPolicyRuleIdentifier() {
        return ruleId;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public void setCount(int value) {
        count = value;
    }

    @Override
    public boolean isOverThreshold() throws ConfigurationException {
        // TODO: better implementation that takes high water mark into account
        PolicyThresholdType thresholdSettings = getPolicyThreshold();
        WaterMarkType lowWaterMark = thresholdSettings != null ? thresholdSettings.getLowWaterMark() : null;
        if (lowWaterMark == null) {
            LOGGER.trace("No low water mark defined.");
            return true;
        }
        Integer lowWaterCount = lowWaterMark.getCount();
        if (lowWaterCount == null) {
            throw new ConfigurationException("No count in low water mark in a policy rule");
        }
        return count >= lowWaterCount;
    }

    @Override
    public boolean hasSituationConstraint() {
        return hasSituationConstraint(getPolicyConstraints());
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

    @Override
    public @NotNull EvaluatedPolicyRule.TargetType getTargetType() {
        return targetType;
    }

    @NotNull Collection<String> getTriggeredEventMarksOids() {
        if (isTriggered()) {
            return getAllEventMarksOids();
        } else {
            return Set.of();
        }
    }

    @NotNull Collection<String> getAllEventMarksOids() {
        return policyRuleCI.getEventMarksOids();
    }

    public void registerAsForeignRuleIfNeeded() {
        for (EvaluatedExclusionTrigger exclusionTrigger : getAllTriggers(EvaluatedExclusionTrigger.class)) {
            ((EvaluatedAssignmentImpl<?>) exclusionTrigger.getConflictingAssignment())
                    .registerAsForeignRule(this);
        }
    }

    @Override
    public @Nullable EvaluatedAssignment getNewOwner() {
        return null;
    }

    @Override
    public @NotNull EvaluatedPolicyRule getEvaluatedPolicyRule() {
        return this;
    }

    public void setEvaluated() {
        stateCheck(!evaluated, "Already evaluated: %s", this);
        evaluated = true;
    }

    @Override
    public boolean isEvaluated() {
        return evaluated;
    }

    /**
     * Comparing only rule identifier and assignment path.
     *
     * While rule identifier is obviously needed, the assignment path is needed to distinguish
     * whether two rule instances were triggered by the same assignment (assignment path is equivalent).
     */
    // TODO think about whether we should use this in equals() as well
    public boolean isTheSameAs(EvaluatedPolicyRuleImpl other) {
        if (other == null) {
            return false;
        }

        if (!Objects.equals(getPolicyRuleIdentifier(), other.getPolicyRuleIdentifier())) {
            return false;
        }

        AssignmentPath thisPath = getAssignmentPath();
        AssignmentPath otherPath = other.getAssignmentPath();

        if (thisPath == null && otherPath == null) {
            return true;
        }

        return thisPath != null && otherPath != null && thisPath.equivalent(otherPath);
    }
}
