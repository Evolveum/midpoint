/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 *
 */
public interface EvaluatedPolicyRule extends DebugDumpable, Serializable, Cloneable {

    @NotNull
    Collection<EvaluatedPolicyRuleTrigger<?>> getTriggers();

    default boolean isTriggered() {
        return !getTriggers().isEmpty();
    }

    /**
     * Returns all triggers, even those that were indirectly collected via situation policy rules.
     */
    @NotNull
    Collection<EvaluatedPolicyRuleTrigger<?>> getAllTriggers();

    /**
     * Returns all triggers of given type, stepping down to situation policy rules and composite triggers.
     * An exception are composite "not" triggers: it is usually of no use to collect negated triggers.
     */
    <T extends EvaluatedPolicyRuleTrigger<?>> Collection<T> getAllTriggers(Class<T> type);

    String getName();

    @NotNull PolicyRuleType getPolicyRule();

    PolicyConstraintsType getPolicyConstraints();

    PolicyThresholdType getPolicyThreshold();

    // returns statically defined actions; consider using getEnabledActions() instead
    PolicyActionsType getActions();

    AssignmentPath getAssignmentPath();

    // TODO consider removing
    String getPolicySituation();

    Collection<PolicyExceptionType> getPolicyExceptions();

    void addToEvaluatedPolicyRuleBeans(
            Collection<EvaluatedPolicyRuleType> ruleBeans,
            PolicyRuleExternalizationOptions options,
            Predicate<EvaluatedPolicyRuleTrigger<?>> triggerSelector);

    boolean isGlobal();

    String toShortString();

    List<TreeNode<LocalizableMessage>> extractMessages();

    List<TreeNode<LocalizableMessage>> extractShortMessages();

    // BEWARE: enabled actions can be queried only after computeEnabledActions has been called
    // todo think again about this

    boolean containsEnabledAction();

    boolean containsEnabledAction(Class<? extends PolicyActionType> clazz);

    Collection<PolicyActionType> getEnabledActions();

    <T extends PolicyActionType> List<T> getEnabledActions(Class<T> clazz);

    <T extends PolicyActionType> T getEnabledAction(Class<T> clazz);

    // use only if you know what you're doing
    void addTrigger(@NotNull EvaluatedPolicyRuleTrigger<?> trigger);

    //experimental
    String getPolicyRuleIdentifier();

    default boolean hasThreshold() {
        return getPolicyRule().getPolicyThreshold() != null; // refine this if needed
    }

    int getCount();

    void setCount(int value);

    boolean isOverThreshold() throws ConfigurationException;

    boolean hasSituationConstraint();

    default boolean isApplicableToFocusObject() {
        return PolicyRuleTypeUtil.isApplicableToObject(getPolicyRule());
    }

    default boolean isApplicableToProjection() {
        return PolicyRuleTypeUtil.isApplicableToProjection(getPolicyRule());
    }

    default boolean isApplicableToAssignment() {
        return PolicyRuleTypeUtil.isApplicableToAssignment(getPolicyRule());
    }

    /** To which object is the policy rule targeted and how. */
    @NotNull TargetType getTargetType();

    /**
     * To which object is the policy rule targeted, from the point of assignment mechanisms - and how?
     * For example, if it's assigned to the focus (to be applied either to the focus or the projections),
     * then it's {@link #OBJECT}. If it's assigned directly to the assignment target, it's {@link #DIRECT_ASSIGNMENT_TARGET}.
     */
    enum TargetType {

        /** Focus or projection */
        OBJECT,

        /** The rule applies directly to the target of the current evaluated assignment (attached to this rule!). */
        DIRECT_ASSIGNMENT_TARGET,

        /**
         * The rule applies to a different target (induced to focus), stemming from the current evaluated assignment
         * (attached to this rule).
         *
         * An example: Let `Engineer` induce `Employee` which conflicts with `Contractor`. An SoD rule is attached
         * to `Employee`. But let the user have assignments for `Engineer` and `Contractor` only. So the target type
         * for such rule is this one.
         */
        INDIRECT_ASSIGNMENT_TARGET
    }
}
