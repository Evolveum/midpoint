/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.policy;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.config.AbstractPolicyRuleConfigItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;

import com.evolveum.midpoint.schema.config.PolicyActionConfigItem;

import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.TreeNode;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

/**
 * Common interface for policy rules obtained from different places and evaluated at different places.
 * E.g. policy rules from task activities, task assignments or global rules from system configuration.
 *
 * TODO sort the methods in some logical order, document them, etc.
 */
public interface EvaluatedPolicyRule extends DebugDumpable, Serializable, Cloneable {

    /** Automatically generated identifier that - we hope - uniquely identifies the policy rule. */
    @NotNull PolicyRuleIdentifier getRuleIdentifier();

    /**
     * Returns all triggers of given type, stepping down to situation policy rules and composite triggers.
     * An exception are composite "not" triggers: it is usually of no use to collect negated triggers.
     */
    <T extends EvaluatedPolicyRuleTrigger<?>> Collection<T> getAllTriggers(Class<T> type);

    default PolicyConstraintsType getPolicyConstraints() {
        return getPolicyRuleBean().getPolicyConstraints();
    }

    default PolicyThresholdType getPolicyThreshold() {
        return getPolicyRuleBean().getPolicyThreshold();
    }

    // returns statically defined actions; consider using getEnabledActions() instead
    default PolicyActionsType getRawActions() {
        return getPolicyRuleBean().getPolicyActions();
    }

    @NotNull AbstractPolicyRuleConfigItem<?> getPolicyRuleConfigItem();

    default @NotNull Collection<String> getAllEventMarksOids() {
        return getPolicyRuleConfigItem().getEventMarksOids();
    }

    default @NotNull PolicyRuleType getPolicyRuleBean() {
        return getPolicyRuleConfigItem().value();
    }

    default @NotNull ConfigurationItemOrigin getRuleOrigin() {
        return getPolicyRuleConfigItem().origin();
    }

    /** Name of the policy rule, as configured. */
    default String getName() {
        return getPolicyRuleConfigItem().getName();
    }

    default Integer getOrder() {
        return getPolicyRuleBean().getOrder();
    }

    //region Actions

    // BEWARE: enabled actions can be queried only after computeEnabledActions has been called
    // todo think again about this

    default boolean containsEnabledAction() {
        return !getEnabledActions().isEmpty();
    }

    /** Returns all enabled actions. Fails if they were not computed yet. TODO clockwork vs other policy rules */
    Collection<? extends PolicyActionConfigItem<?>> getEnabledActions();

    /** Returns enabled action of given type, if there's any. Throws an exception if there are more of them. */
    default <T extends PolicyActionType> @Nullable PolicyActionConfigItem<T> getEnabledAction(Class<T> type) {
        var actions = getEnabledActions(type);
        return MiscUtil.extractSingleton(
                actions,
                () -> new IllegalStateException("More than one enabled policy action of class " + type + ": " + actions));
    }

    /** Returns all enabled actions of given type. */
    default <T extends PolicyActionType> @NotNull List<? extends PolicyActionConfigItem<T>> getEnabledActions(Class<T> type) {
        return PolicyRuleTypeUtil.filterActions(
                getEnabledActions(),
                type);
    }

    /**
     * Returns actions (beans) defined for the policy rule - in the form of a list, not the structured {@link PolicyActionsType}.
     */
    default @NotNull List<PolicyActionType> getActions() {
        return getPolicyRuleConfigItem().getAllActions().stream()
                .map(actionCI -> actionCI.value())
                .collect(Collectors.toUnmodifiableList());
    }

    /** Are there any enabled actions of given type? */
    default boolean containsEnabledAction(Class<? extends PolicyActionType> type) {
        return !getEnabledActions(type).isEmpty();
    }
    //endregion

    //region Triggers
    /** Returns triggers for this policy rule, except for ones triggered by related situation policy rules. */
    @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> getTriggers();

    /** Returns all triggers, even those that were indirectly collected via situation policy rules. */
    @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> getAllTriggers();

    void trigger(Collection<EvaluatedPolicyRuleTrigger<?>> triggers);

    default void trigger(EvaluatedPolicyRuleTrigger<?> trigger) {
        trigger(List.of(trigger));
    }

    /**
     * Was this rule triggered, i.e. are there any triggers? Note that for activity-based rules this may include triggers
     * that occurred in previous activity runs (and so stored in the activity state).
     */
    boolean isTriggered();

    List<TreeNode<LocalizableMessage>> extractMessages();

    List<TreeNode<LocalizableMessage>> extractShortMessages();
    //endregion

    default boolean hasSituationConstraint() {
        return hasSituationConstraint(getPolicyConstraints());
    }

    private boolean hasSituationConstraint(PolicyConstraintsType constraints) {
        return constraints != null &&
                (!constraints.getSituation().isEmpty() ||
                        hasSituationConstraint(constraints.getAnd()) ||
                        hasSituationConstraint(constraints.getOr()) ||
                        hasSituationConstraint(constraints.getNot()));
    }

    private boolean hasSituationConstraint(Collection<PolicyConstraintsType> constraints) {
        return constraints.stream().anyMatch(this::hasSituationConstraint);
    }

    //region Thresholds and counters
    default boolean hasThreshold() {
        return getPolicyRuleBean().getPolicyThreshold() != null; // refine this if needed
    }

    Integer getCount();

    void setCount(Integer localValue, Integer totalValue);

    /**
     * Evaluates whether the current count is over the threshold defined in the policy rule.
     * If there is no threshold, it returns true.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean isOverThreshold() {
        if (!hasThreshold()) {
            return true;
        }

        Integer count = getCount();
        if (count == null) {
            count = 0;
        }

        PolicyThresholdType policyThreshold = getPolicyRuleBean().getPolicyThreshold();
        Integer low = getWaterMarkValue(policyThreshold.getLowWaterMark());
        Integer high = getWaterMarkValue(policyThreshold.getHighWaterMark());

        if (low != null && count < low) {
            // below low water-mark
            return false;
        }

        if (high != null && count > high) {
            // above high water-mark
            return false;
        }

        // either marks are not set, or the count is within the range
        return true;
    }

    private Integer getWaterMarkValue(WaterMarkType waterMark) {
        if (waterMark == null) {
            return null;
        }

        return waterMark.getCount();
    }
    //endregion

    /** Returns the policy situation connected to this rule. Will be replaced by object marks. */
    @Nullable String getPolicySituation();

    default @NotNull List<ObjectReferenceType> getPolicyMarkRef() {
        return getPolicyRuleBean().getMarkRef(); //TODO
    }

    /**
     * Serializes the policy rule into bean form ({@link EvaluatedPolicyRuleType}).
     *
     * - Serializes only triggered rules.
     * - For sitation rules with are both hidden and final, serializes the inner rules. (TODO why should that be?)
     *
     * TODO consider if we cannot merge trigger selector with the options
     *
     * @param options Options - how the serialization should take place.
     * @param triggerSelector Which triggers should be processed?
     */
    @NotNull Collection<EvaluatedPolicyRuleType> toEvaluatedPolicyRuleBeans(
            @NotNull PolicyRuleExternalizationOptions options,
            @Nullable Predicate<EvaluatedPolicyRuleTrigger<?>> triggerSelector);

    /** Returns {@code true} if this rule is present in the provided collection of rules. Checking by id. */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    default boolean isContainedIn(Collection<? extends EvaluatedPolicyRule> rules) {
        var myRuleIdentifier = getRuleIdentifier();
        return rules.stream()
                .anyMatch(r -> r.getRuleIdentifier().equals(myRuleIdentifier));
    }
}
