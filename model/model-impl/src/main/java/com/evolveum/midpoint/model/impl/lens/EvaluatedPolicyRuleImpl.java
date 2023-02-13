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
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
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

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.constants.ExpressionConstants.VAR_RULE_EVALUATION_CONTEXT;
import static com.evolveum.midpoint.util.DebugUtil.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType.FULL;

/**
 * @author semancik
 *
 */
public class EvaluatedPolicyRuleImpl implements EvaluatedPolicyRule {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(EvaluatedPolicyRuleImpl.class);

    @NotNull private final PolicyRuleType policyRuleBean;
    @NotNull private final Collection<EvaluatedPolicyRuleTrigger<?>> triggers = new ArrayList<>();
    @NotNull private final Collection<PolicyExceptionType> policyExceptions = new ArrayList<>();

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
     * It can null for artificially-created policy rules e.g. in task validity cases. To be reviewed.
     */
    @Nullable private final AssignmentPath assignmentPath;
    @Nullable private final ObjectType directOwner;

    /**
     * Evaluated assignment that brought this policy rule to the focus or target.
     * May be missing for artificially-crafted policy rules (to be reviewed!)
     */
    private final EvaluatedAssignmentImpl<?> evaluatedAssignment;

    /** Tries to uniquely identify the policy rule. Used e.g. for threshold counters. */
    @NotNull private final String ruleId;

    private int count;

    private boolean enabledActionsComputed;

    // computed only when necessary (typically when triggered)
    @NotNull private final List<PolicyActionType> enabledActions = new ArrayList<>();

    public EvaluatedPolicyRuleImpl(
            @NotNull PolicyRuleType policyRuleBean,
            @NotNull String ruleId,
            @Nullable AssignmentPath assignmentPath,
            @Nullable EvaluatedAssignmentImpl<?> evaluatedAssignment) {
        this.policyRuleBean = policyRuleBean;
        this.ruleId = ruleId;
        this.assignmentPath = assignmentPath;
        this.evaluatedAssignment = evaluatedAssignment;
        this.directOwner = computeDirectOwner();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public EvaluatedPolicyRuleImpl clone() {
        return new EvaluatedPolicyRuleImpl(
                CloneUtil.clone(policyRuleBean),
                ruleId,
                CloneUtil.clone(assignmentPath),
                evaluatedAssignment);
    }

    private ObjectType computeDirectOwner() {
        if (assignmentPath == null) {
            return null;
        }
        List<ObjectType> roots = assignmentPath.getFirstOrderChain();
        return roots.isEmpty() ? null : roots.get(roots.size()-1);
    }

    @Override
    public String getName() {
        return policyRuleBean.getName();
    }

    @Override
    public @NotNull PolicyRuleType getPolicyRule() {
        return policyRuleBean;
    }

    @Nullable
    @Override
    public AssignmentPath getAssignmentPath() {
        return assignmentPath;
    }

    public EvaluatedAssignmentImpl<?> getEvaluatedAssignment() {
        return evaluatedAssignment;
    }

    @Nullable
    @Override
    public ObjectType getDirectOwner() {
        return directOwner;
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
            if (trigger instanceof EvaluatedCompositeTrigger) {
                EvaluatedCompositeTrigger compositeTrigger = (EvaluatedCompositeTrigger) trigger;
                if (compositeTrigger.getConstraintKind() != PolicyConstraintKindType.NOT) {
                    collectTriggers(collected, compositeTrigger.getInnerTriggers(), type);
                } else {
                    // there is no use in collecting "negated" triggers
                }
            }
        }
    }

    void addTriggers(Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        this.triggers.addAll(triggers);
    }

    @Override
    public void addTrigger(@NotNull EvaluatedPolicyRuleTrigger<?> trigger) {
        triggers.add(trigger);
    }

    @NotNull
    @Override
    public Collection<PolicyExceptionType> getPolicyExceptions() {
        return policyExceptions;
    }

    void addPolicyException(PolicyExceptionType exception) {
        policyExceptions.add(exception);
    }

    @Override
    public PolicyActionsType getActions() {
        return policyRuleBean.getPolicyActions();
    }

    // TODO rewrite this method
    @Override
    public String getPolicySituation() {
        // TODO default situations depending on getTriggeredConstraintKinds
        if (policyRuleBean.getPolicySituation() != null) {
            return policyRuleBean.getPolicySituation();
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
        PrismPrettyPrinter.debugDumpValue(sb, indent + 2, policyRuleBean, PrismContext.get(),
                PolicyRuleType.COMPLEX_TYPE, PrismContext.LANG_XML);
        sb.append('\n');
        debugDumpWithLabelLn(sb, "assignmentPath", assignmentPath, indent + 1);
        debugDumpWithLabelLn(sb, "triggers", triggers, indent + 1);
        debugDumpWithLabelLn(sb, "directOwner", ObjectTypeUtil.toShortString(directOwner), indent + 1);
        debugDumpWithLabel(sb, "rootObjects", assignmentPath != null ? String.valueOf(assignmentPath.getFirstOrderChain()) : null, indent + 1);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof EvaluatedPolicyRuleImpl))
            return false;
        EvaluatedPolicyRuleImpl that = (EvaluatedPolicyRuleImpl) o;
        return java.util.Objects.equals(policyRuleBean, that.policyRuleBean) &&
                Objects.equals(assignmentPath, that.assignmentPath) &&
                Objects.equals(triggers, that.triggers) &&
                Objects.equals(policyExceptions, that.policyExceptions) &&
                Objects.equals(directOwner, that.directOwner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyRuleBean, assignmentPath, triggers, policyExceptions, directOwner);
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
    public void addToEvaluatedPolicyRuleBeans(Collection<EvaluatedPolicyRuleType> ruleBeans,
            PolicyRuleExternalizationOptions options, Predicate<EvaluatedPolicyRuleTrigger<?>> triggerSelector) {
        EvaluatedPolicyRuleType bean = new EvaluatedPolicyRuleType();
        bean.setRuleName(getName());
        boolean isFull = options.getTriggeredRulesStorageStrategy() == FULL;
        if (isFull && assignmentPath != null) {
            bean.setAssignmentPath(assignmentPath.toAssignmentPathType(options.isIncludeAssignmentsContent()));
        }
        if (isFull && directOwner != null) {
            bean.setDirectOwnerRef(ObjectTypeUtil.createObjectRef(directOwner));
            bean.setDirectOwnerDisplayName(ObjectTypeUtil.getDisplayName(directOwner));
        }
        for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
            if (triggerSelector != null && !triggerSelector.test(trigger)) {
                continue;
            }
            if (trigger instanceof EvaluatedSituationTrigger && trigger.isHidden()) {
                for (EvaluatedPolicyRule sourceRule : ((EvaluatedSituationTrigger) trigger).getSourceRules()) {
                    sourceRule.addToEvaluatedPolicyRuleBeans(ruleBeans, options, null);
                }
            } else {
                bean.getTrigger().add(trigger.toEvaluatedPolicyRuleTriggerBean(options));
            }
        }
        if (bean.getTrigger().isEmpty()) {
            // skip empty situation rule
        } else {
            ruleBeans.add(bean);
        }
    }

    @NotNull
    public List<PolicyActionType> getEnabledActions() {
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
        if (rctx instanceof AssignmentPolicyRuleEvaluationContext) {
            AssignmentPolicyRuleEvaluationContext<?> actx = (AssignmentPolicyRuleEvaluationContext<?>) rctx;
            PrismObject<?> target = actx.evaluatedAssignment.getTarget();
            var.put(ExpressionConstants.VAR_TARGET, target, target != null ? target.getDefinition() : getObjectDefinition());
            var.put(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, actx.evaluatedAssignment, EvaluatedAssignment.class);
            AssignmentType assignment = actx.evaluatedAssignment.getAssignment(actx.state == ObjectState.BEFORE);
            var.put(ExpressionConstants.VAR_ASSIGNMENT, assignment, getAssignmentDefinition(assignment, prismContext));
        } else if (rctx instanceof ObjectPolicyRuleEvaluationContext) {
            var.put(ExpressionConstants.VAR_TARGET, null, getObjectDefinition());
            var.put(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, null, EvaluatedAssignment.class);
            var.put(ExpressionConstants.VAR_ASSIGNMENT, null, getAssignmentDefinition(null, prismContext));
        } else if (rctx != null) {
            throw new AssertionError(rctx);
        }
        var.put(VAR_RULE_EVALUATION_CONTEXT, rctx, PolicyRuleEvaluationContext.class);
        return var;
    }

    private PrismObjectDefinition<ObjectType> getObjectDefinition() {
        return PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ObjectType.class);
    }

    private static PrismContainerDefinition<?> getAssignmentDefinition(AssignmentType assignment, PrismContext prismContext) {
        if (assignment != null) {
            PrismContainerDefinition<?> definition = assignment.asPrismContainerValue().getDefinition();
            if (definition != null) {
                return definition;
            }
        }

        return prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(AssignmentHolderType.class)
                .findContainerDefinition(AssignmentHolderType.F_ASSIGNMENT);
    }

    @Override
    public boolean containsEnabledAction() {
        return !enabledActions.isEmpty();
    }

    @Override
    public boolean containsEnabledAction(Class<? extends PolicyActionType> clazz) {
        return !getEnabledActions(clazz).isEmpty();
    }

    @Override
    public <T extends PolicyActionType> List<T> getEnabledActions(Class<T> clazz) {
        return PolicyRuleTypeUtil.filterActions(enabledActions, clazz);
    }

    @Override
    public <T extends PolicyActionType> T getEnabledAction(Class<T> clazz) {
        List<T> actions = getEnabledActions(clazz);
        if (actions.isEmpty()) {
            return null;
        } else if (actions.size() == 1) {
            return actions.get(0);
        } else {
            throw new IllegalStateException("More than one enabled policy action of class " + clazz + ": " + actions);
        }
    }

    public void computeEnabledActions(
            @Nullable PolicyRuleEvaluationContext<?> rctx, PrismObject<?> object, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LOGGER.trace("Computation of enabled actions starting");
        List<PolicyActionType> allActions = PolicyRuleTypeUtil.getAllActions(policyRuleBean.getPolicyActions());
        LOGGER.trace("Actions defined for policy rule: {}", allActions);
        for (PolicyActionType action : allActions) {
            if (action.getCondition() != null) {
                VariablesMap variables = createVariablesMap(rctx, object);
                if (!LensExpressionUtil.evaluateBoolean(
                        action.getCondition(),
                        variables,
                        rctx != null ? rctx.elementContext : null,
                        "condition in action " + action.getName() + " (" + action.getClass().getSimpleName() + ")",
                        task,
                        result)) {
                    LOGGER.trace("Skipping action {} ({}) because the condition evaluated to false", action.getName(), action.getClass().getSimpleName());
                    continue;
                } else {
                    LOGGER.trace("Accepting action {} ({}) because the condition evaluated to true", action.getName(), action.getClass().getSimpleName());
                }
            }
            LOGGER.trace("Adding action {} ({}) into the enabled action list.", action, action.getClass().getSimpleName());
            enabledActions.add(action);
        }
        enabledActionsComputed = true;
    }

    //experimental
    @Override
    public String getPolicyRuleIdentifier() {
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

    @NotNull Collection<String> getTriggeredEventMarks() {
        if (isTriggered()) {
            return getAllEventMarks();
        } else {
            return Set.of();
        }
    }

    @NotNull Collection<String> getAllEventMarks() {
        return policyRuleBean.getMarkRef().stream()
                .map(AbstractReferencable::getOid)
                .collect(Collectors.toSet());
    }
}
