/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.policy;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;
import com.evolveum.midpoint.schema.policy.PolicyRuleDumpUtil;

import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedPolicyRuleTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedStateTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LinkTargetObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MultiplicityPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StatePolicyConstraintType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintPresentationType;

/**
 * A policy rule is said to be _triggered_ if its policy constraint matches the object state, its transition, or any other
 * condition.
 *
 * _Trigger_ is the cause of the rule being triggered. Typical examples are:
 *
 * - "assignment is going to be added" (for {@link PolicyConstraintKind#ASSIGNMENT_MODIFICATION} transitional constraint)
 * - "object is in illegal state" (for {@link PolicyConstraintKind#OBJECT_STATE} constraint)
 * - "resource object was modified" (currently represented by {@link PolicyConstraintKind#CUSTOM} constraint)
 * - "activity took too long to execute (for {@link PolicyConstraintKind#EXECUTION_TIME} constraint)
 *
 * This class represents such a trigger.
 *
 * Each its instance is connected to a specific policy constraint.
 * Currently, there is a rough correspondence between:
 *
 * - POJO version of the trigger (base: {@link EvaluatedPolicyRuleTrigger}, specific e.g. {@link `EvaluatedStateTrigger`})
 * - XML/JSON/YAML (bean) version of the trigger (base: {@link EvaluatedPolicyRuleTriggerType}, specific e.g.
 * {@link EvaluatedStateTriggerType})
 * - constraint that caused the trigger (base: {@link AbstractPolicyConstraintType}, specific e.g. {@link StatePolicyConstraintType})
 * - evaluator that evaluates the constraint (multiple bases - activities, clockwork, specific e.g. `StateConstraintEvaluator`)
 *
 * But the match may not be 100%. For example, many triggers (both POJO and bean versions) carry no additional information
 * w.r.t. the base triggers. We may remove them in the future.
 *
 * ---
 *
 * When constraints form trees, their corresponding triggers do that as well. So, e.g. `C1 and C2` constraint will produce
 * a root-level `EvaluatedCompositeTrigger` with two children, one for `C1` and one for `C2`.
 *
 * These triggers should be immutable, although it is not enforced (by freezing the constraint and other content) for now.
 * But at least all fields should be `final`.
 */
public abstract class EvaluatedPolicyRuleTrigger<CT extends AbstractPolicyConstraintType> implements DebugDumpable, Serializable {

    /**
     * What is the specific constraint kind for constraint that produced this trigger?
     *
     * It is stored here because the type of {@link #constraint} may be shared for multiple related constraints
     * (like {@link PolicyConstraintKind#MIN_ASSIGNEES} and {@link PolicyConstraintKind#MAX_ASSIGNEES}
     * for {@link MultiplicityPolicyConstraintType}).
     */
    @NotNull private final PolicyConstraintKind constraintKind;

    /** The actual value (parameters) of the constraint that produced this trigger. */
    @NotNull private final CT constraint;

    /** @see #getMessage()  */
    private final LocalizableMessage message;

    /** @see #getShortMessage()  */
    private final LocalizableMessage shortMessage;

    /** @see #isEnforcementOverride() */
    private final boolean enforcementOverride;

    public EvaluatedPolicyRuleTrigger(
            @NotNull PolicyConstraintKind constraintKind,
            @NotNull CT constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {
        this.constraintKind = constraintKind;
        this.constraint = constraint;
        this.message = message;
        this.shortMessage = shortMessage;
        this.enforcementOverride = false;
    }

    public EvaluatedPolicyRuleTrigger(
            @NotNull PolicyConstraintKind constraintKind,
            @NotNull CT constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage,
            boolean enforcementOverride) {
        this.constraintKind = constraintKind;
        this.constraint = constraint;
        this.message = message;
        this.shortMessage = shortMessage;
        this.enforcementOverride = enforcementOverride;
    }

    /**
     * The kind of constraint that caused the trigger.
     *
     * For backwards compatibility, we return the "serializable version" (i.e. {@link PolicyConstraintKindType}).
     */
    public @NotNull PolicyConstraintKindType getConstraintKind() {
        return constraintKind.getSerializableVersion();
    }

    public @NotNull CT getConstraint() {
        return constraint;
    }

    /**
     * Human-readable message associated with this trigger. The message explain why the rule was triggered. It can be used
     * in the logs, as an error message, in the audit trail and so on.
     *
     * An example: "Assignment ... is going to be added".
     */
    public LocalizableMessage getMessage() {
        return message;
    }

    /**
     * Short form of {@link #getMessage()}.
     */
    public LocalizableMessage getShortMessage() {
        return shortMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluatedPolicyRuleTrigger<?> that)) {
            return false;
        }
        return constraintKind == that.constraintKind
                && Objects.equals(constraint, that.constraint)
                && Objects.equals(message, that.message)
                && Objects.equals(shortMessage, that.shortMessage)
                && enforcementOverride == that.enforcementOverride;
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraintKind, constraint, message);
    }

    @Override
    public String toString() {
        return "EvaluatedPolicyRuleTrigger(" +
                (constraint.getName() != null ? "[" + constraint.getName() + "] " : "") +
                constraintKind + ": " + message + ")";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        debugDumpCommon(sb, indent + 1);
        debugDumpSpecific(sb, indent + 1);
        return sb.toString();
    }

    private void debugDumpCommon(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelToStringLn(sb, "constraintName", constraint.getName(), indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "constraintKind", constraintKind, indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "constraint", constraint, indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "message", message, indent);
        if (isFinal()) {
            DebugUtil.debugDumpWithLabelToStringLn(sb, "final", true, indent);
        }
        if (isHidden()) {
            DebugUtil.debugDumpWithLabelToStringLn(sb, "hidden", true, indent);
        }
        var targetObjects = getTargetObjects();
        if (!targetObjects.isEmpty()) {
            DebugUtil.debugDumpWithLabelToStringLn(sb, "targetObjects", targetObjects, indent + 1);
        }
    }

    /**
     * If returns {@code true}, this trigger will not be presented.
     *
     * Useful for hiding constraints that are too technical to be shown to a user.
     *
     * @see PolicyConstraintPresentationType#F_HIDDEN
     */
    public boolean isHidden() {
        PolicyConstraintPresentationType presentation = constraint.getPresentation();
        return presentation != null && Boolean.TRUE.equals(presentation.isHidden());
    }

    /**
     * If returns {@code true}, no children of this trigger will be presented.
     *
     * Useful for hiding constraints that are too technical to be shown to a user.
     *
     * @see PolicyConstraintPresentationType#F_FINAL
     */
    public boolean isFinal() {
        PolicyConstraintPresentationType presentation = constraint.getPresentation();
        return presentation != null && Boolean.TRUE.equals(presentation.isFinal());
    }

    protected void debugDumpSpecific(StringBuilder sb, int indent) {
    }

    public String toDiagShortcut() {
        return PolicyRuleDumpUtil.toDiagShortcut(constraintKind);
    }

    public Collection<EvaluatedPolicyRuleTrigger<?>> getInnerTriggers() {
        return Collections.emptySet();
    }

    /**
     * Used to create XML bean which is stored in focus, assignment, or in the activity state.
     */
    public EvaluatedPolicyRuleTriggerType toEvaluatedPolicyRuleTriggerBean() {
        return toEvaluatedPolicyRuleTriggerBean(PolicyRuleExternalizationOptions.empty());
    }

    /**
     * Converts this trigger to externalized (bean) form, i.e., {@link EvaluatedPolicyRuleTriggerType} or its subtype.
     */
    public EvaluatedPolicyRuleTriggerType toEvaluatedPolicyRuleTriggerBean(@NotNull PolicyRuleExternalizationOptions options) {
        return toEvaluatedPolicyRuleTriggerBean(options, EvaluatedPolicyRuleTriggerType::new);
    }

    protected <T extends EvaluatedPolicyRuleTriggerType> T toEvaluatedPolicyRuleTriggerBean(
            @NotNull PolicyRuleExternalizationOptions options,
            @NotNull Supplier<T> beanConstructor) {
        var bean = beanConstructor.get();
        bean.setConstraintKind(getConstraintKind());
        bean.setMessage(LocalizationUtil.createLocalizableMessageType(getMessage()));
        bean.setShortMessage(LocalizationUtil.createLocalizableMessageType(getShortMessage()));
        PolicyConstraintPresentationType presentation = getConstraint().getPresentation();
        if (presentation != null) {
            bean.setFinal(Boolean.TRUE.equals(presentation.isFinal()));
            bean.setHidden(Boolean.TRUE.equals(presentation.isHidden())); // null would mean it would be overridden by isHiddenByDefault
            bean.setPresentationOrder(presentation.getDisplayOrder());
        }
        return bean;
    }

    /**
     * Returns target object(s) that were matched by constraint that produced this trigger.
     * For example: target of the assignment that was added (and that matched "assignment" constraint).
     *
     * Used for the "linked objects" feature, see {@link LinkTargetObjectSelectorType#F_MATCHES_CONSTRAINT}.
     *
     * Applicable only to a very specific triggers. TODO reconsider whether it should be here
     */
    public Collection<? extends PrismObject<?>> getTargetObjects() {
        return List.of();
    }

    /**
     * If true, this trigger is to be reported as {@link PolicyViolationException} regardless of specified policy rule action.
     * Used e.g. for disallowing assignment of two pruned roles (MID-4766).
     *
     * TODO reconsider this mechanism, as it doesn't seem fitting for the trigger as such: it is more related to the context
     *  in which the rule was triggered
     */
    public boolean isEnforcementOverride() {
        return enforcementOverride;
    }
}
