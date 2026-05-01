/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.schema.constants.ExpressionConstants.VAR_RULE_EVALUATION_CONTEXT;
import static com.evolveum.midpoint.util.DebugUtil.*;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.io.Serial;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.DirectlyEvaluatedClockworkPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedExclusionTrigger;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectState;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.repo.common.activity.policy.ActivityPolicyRule;
import com.evolveum.midpoint.repo.common.policy.*;
import com.evolveum.midpoint.schema.config.AbstractPolicyRuleConfigItem;
import com.evolveum.midpoint.schema.config.ExpressionConfigItem;
import com.evolveum.midpoint.schema.config.PolicyActionConfigItem;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.policy.PolicyRuleDumpUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * A (single) implementation of {@link DirectlyEvaluatedClockworkPolicyRule}.
 */
public class DirectlyEvaluatedClockworkPolicyRuleImpl
        extends BaseEvaluatedPolicyRuleImpl
        implements DirectlyEvaluatedClockworkPolicyRule {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(DirectlyEvaluatedClockworkPolicyRuleImpl.class);

    /** See {@link DirectlyEvaluatedClockworkPolicyRule#getRuleAssignmentPath()}. */
    @Nullable private final AssignmentPath ruleAssignmentPath;

    /** See {@link DirectlyEvaluatedClockworkPolicyRule#getOriginatingAssignment()}. */
    private final EvaluatedAssignmentImpl<?> originatingAssignment;

    /** See {@link DirectlyEvaluatedClockworkPolicyRule#getTargetType()}. */
    @NotNull private final TargetType targetType;

    // TODO what is this? [pavol]
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

    /**
     * Filled in only if rule was loaded from specific activity.
     * E.g. not for global policy rules or rules obtained from task assignments.
     * *
     * TODO vysvetlit preco to tu teraz je (data needs, counters sa handluju inak ked je rule z aktivity a nie so sysconfigu
     */
    private final ActivityPolicyRule activityPolicyRule;

    public DirectlyEvaluatedClockworkPolicyRuleImpl(
            @NotNull AbstractPolicyRuleConfigItem<?> policyRuleCI,
            @NotNull String ruleId,
            @Nullable AssignmentPath ruleAssignmentPath,
            @NotNull TargetType targetType) {
        this(policyRuleCI, ruleId, ruleAssignmentPath, null, targetType, null);
    }

    public DirectlyEvaluatedClockworkPolicyRuleImpl(
            @NotNull AbstractPolicyRuleConfigItem<?> policyRuleCI,
            @NotNull String ruleId,
            @Nullable AssignmentPath ruleAssignmentPath,
            @NotNull TargetType targetType,
            ActivityPolicyRule activityPolicyRule) {
        this(policyRuleCI, ruleId, ruleAssignmentPath, null, targetType, activityPolicyRule);
    }

    public DirectlyEvaluatedClockworkPolicyRuleImpl(
            @NotNull AbstractPolicyRuleConfigItem<?> policyRuleCI,
            @NotNull String ruleId,
            @Nullable AssignmentPath ruleAssignmentPath,
            @Nullable EvaluatedAssignmentImpl<?> originatingAssignment,
            @NotNull TargetType targetType) {
        this(policyRuleCI, ruleId, ruleAssignmentPath, originatingAssignment, targetType, null);
    }

    public DirectlyEvaluatedClockworkPolicyRuleImpl(
            @NotNull AbstractPolicyRuleConfigItem<?> policyRuleCI,
            @NotNull String ruleId,
            @Nullable AssignmentPath ruleAssignmentPath,
            @Nullable EvaluatedAssignmentImpl<?> originatingAssignment,
            @NotNull TargetType targetType,
            ActivityPolicyRule activityPolicyRule) {
        super(policyRuleCI, PlainPolicyRuleIdentifier.of(ruleId));
        this.ruleAssignmentPath = ruleAssignmentPath;
        this.originatingAssignment = originatingAssignment;
        this.targetType = targetType;
        this.activityPolicyRule = activityPolicyRule;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public DirectlyEvaluatedClockworkPolicyRuleImpl clone() {
        return new DirectlyEvaluatedClockworkPolicyRuleImpl(
                CloneUtil.cloneCloneable(getPolicyRuleConfigItem()),
                getRuleIdentifier().asString(),
                CloneUtil.cloneCloneable(ruleAssignmentPath),
                originatingAssignment,
                targetType);
    }

    @Override
    public ActivityPath getActivityPath() {
        return activityPolicyRule != null ? activityPolicyRule.getPath() : null;
    }

    @Override
    public void setCount(Integer localValue, Integer totalValue) {
        if (activityPolicyRule != null) {
            activityPolicyRule.setCount(localValue, totalValue);
        }

        count = totalValue;
    }

    @Override
    public boolean isGlobal() {
        // in the future we might employ special flag for this (if needed)
        return getPolicyRuleBean() instanceof GlobalPolicyRuleType;
    }

    @Nullable
    @Override
    public AssignmentPath getRuleAssignmentPath() {
        return ruleAssignmentPath;
    }

    public @NotNull AssignmentPath getRuleAssignmentPathRequired() {
        return MiscUtil.requireNonNull(
                ruleAssignmentPath,
                () -> new IllegalStateException("No assignment path in " + this));
    }

    @Nullable
    @Override
    public EvaluatedAssignmentImpl<?> getOriginatingAssignment() {
        return originatingAssignment;
    }

    @Override
    public @NotNull Collection<EvaluatedExclusionTrigger> getRelevantExclusionTriggers() {
        // All exclusion triggers here are relevant
        return getAllTriggers(EvaluatedExclusionTrigger.class);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        debugDumpLabelLn(sb, getClass().getSimpleName() + " " + (getName() != null ? getName() + " " : "") + "(triggers: " + getTriggers().size() + ")", indent);
        debugDumpWithLabelLn(sb, "name", getName(), indent + 1);
        debugDumpLabelLn(sb, "policyRuleType", indent + 1);
        indentDebugDump(sb, indent + 2);
        PrismPrettyPrinter.debugDumpValue(sb, indent + 2, getPolicyRuleConfigItem().value(), PolicyRuleType.COMPLEX_TYPE, PrismContext.LANG_XML);
        sb.append('\n');
        debugDumpWithLabelLn(sb, "ruleAssignmentPath", ruleAssignmentPath, indent + 1);
        debugDumpWithLabelLn(sb, "triggers", getTriggers(), indent + 1);
        debugDumpWithLabel(sb, "rootObjects", ruleAssignmentPath != null ? String.valueOf(ruleAssignmentPath.getFirstOrderChain()) : null, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getName() + ")";
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
                .append(PolicyRuleDumpUtil.toShortString(getPolicyConstraints()))
                .append(")");
        sb.append("->");
        sb.append("(")
                .append(PolicyRuleDumpUtil.toShortString(getRawActions(), enabledActionsComputed ? enabledActions : null))
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
    public @NotNull Collection<EvaluatedPolicyRuleType> toEvaluatedPolicyRuleBeans(
            @NotNull PolicyRuleExternalizationOptions options,
            @Nullable Predicate<EvaluatedPolicyRuleTrigger<?>> triggerSelector) {
        if (!isTriggered()) {
            return List.of();
        }
        var ruleBeans = new ArrayList<EvaluatedPolicyRuleType>();
        var ruleBean = new EvaluatedPolicyRuleType();
        ruleBean.setRuleName(getName());
        if (options.isFullStorageStrategy()) {
            if (ruleAssignmentPath != null) {
                ruleBean.setAssignmentPath(
                        ruleAssignmentPath.toAssignmentPathBean(options.isIncludeAssignmentsContent()));
            }
            ObjectType directOwner = computeDirectOwner();
            if (directOwner != null) {
                ruleBean.setDirectOwnerRef(ObjectTypeUtil.createObjectRef(directOwner));
                ruleBean.setDirectOwnerDisplayName(ObjectTypeUtil.getDisplayName(directOwner));
            }
        }
        for (EvaluatedPolicyRuleTrigger<?> trigger : getTriggers()) {
            if (triggerSelector != null && !triggerSelector.test(trigger)) {
                continue;
            }
            if (!options.matchesSelector(trigger)) {
                continue;
            }
            if (trigger instanceof EvaluatedSituationTrigger situationTrigger && trigger.isHidden()) {
                // We don't want the trigger, but we want inner rules
                for (var sourceRule : situationTrigger.getSourceRules()) {
                    ruleBeans.addAll(
                            sourceRule.toEvaluatedPolicyRuleBeans(options, triggerSelector));
                }
            } else {
                // For non-final situation triggers, this will add inner policy rules by default
                ruleBean.getTrigger().add(
                        trigger.toEvaluatedPolicyRuleTriggerBean(options));
            }
        }
        if (ruleBean.getTrigger().isEmpty()) {
            // We don't serialize rules without relevant triggers
        } else {
            ruleBeans.add(ruleBean);
        }
        return ruleBeans;
    }

    private ObjectType computeDirectOwner() {
        if (ruleAssignmentPath == null) {
            return null;
        }
        List<ObjectType> roots = ruleAssignmentPath.getFirstOrderChain();
        return roots.isEmpty() ? null : roots.get(roots.size() - 1);
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

        List<PolicyActionConfigItem<?>> allActions = getPolicyRuleConfigItem().getAllActions();
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
    public Integer getCount() {
        if (activityPolicyRule != null) {
            return activityPolicyRule.getTotalCount();
        }

        return count;
    }

    @Override
    public @NotNull DirectlyEvaluatedClockworkPolicyRule.TargetType getTargetType() {
        return targetType;
    }

    @NotNull Collection<String> getTriggeredEventMarksOids() {
        if (isTriggered()) {
            return getAllEventMarksOids();
        } else {
            return Set.of();
        }
    }

    public void registerAsForeignRuleIfNeeded() {
        for (EvaluatedExclusionTrigger exclusionTrigger : getAllTriggers(EvaluatedExclusionTrigger.class)) {
            ((EvaluatedAssignmentImpl<?>) exclusionTrigger.getConflictingAssignment())
                    .registerAsForeignRule(this);
        }
    }

    public void setEvaluated() {
        stateCheck(!evaluated, "Already evaluated: %s", this);
        evaluated = true;
    }

    @Override
    public boolean isEvaluated() {
        return evaluated;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DirectlyEvaluatedClockworkPolicyRuleImpl that)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return Objects.equals(ruleAssignmentPath, that.ruleAssignmentPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ruleAssignmentPath);
    }

    /**
     * Comparing only rule identifier and assignment path.
     *
     * While rule identifier is obviously needed, the assignment path is needed to distinguish
     * whether two rule instances were triggered by the same assignment (assignment path is equivalent).
     */
    // TODO think about whether we should use this in equals() as well
    boolean isTheSameAs(DirectlyEvaluatedClockworkPolicyRuleImpl other) {
        if (other == null) {
            return false;
        }

        if (!Objects.equals(getRuleIdentifier(), other.getRuleIdentifier())) {
            return false;
        }

        AssignmentPath thisPath = getRuleAssignmentPath();
        AssignmentPath otherPath = other.getRuleAssignmentPath();

        if (thisPath == null && otherPath == null) {
            return true;
        }

        return thisPath != null && otherPath != null && thisPath.equivalent(otherPath);
    }
}
