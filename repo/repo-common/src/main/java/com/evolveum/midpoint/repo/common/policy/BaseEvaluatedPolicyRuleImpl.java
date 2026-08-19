/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.PolicyActionConfigItem;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.TreeNode;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TransitionPolicyConstraintType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.config.AbstractPolicyRuleConfigItem;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.repo.common.policy.TriggerPresentationUtil.extractMessages;
import static com.evolveum.midpoint.repo.common.policy.TriggerPresentationUtil.MessageKind;

/**
 * Implementation of the most generic features in {@link EvaluatedPolicyRule} interface.
 */
public abstract class BaseEvaluatedPolicyRuleImpl implements EvaluatedPolicyRule {

    private static final Trace LOGGER = TraceManager.getTrace(BaseEvaluatedPolicyRuleImpl.class);

    /** {@link ConfigurationItem} for the policy rule in question. */
    @NotNull private final AbstractPolicyRuleConfigItem<?> policyRuleCI;

    /** Identifier of the policy rule (there are multiple implementations). */
    @NotNull private final PolicyRuleIdentifier ruleIdentifier;

    /** Triggers that resulted from the rule evaluation. */
    @NotNull private final Collection<EvaluatedPolicyRuleTrigger<?>> triggers = new ArrayList<>();

    /**
     * The count computed for *this* evaluation, i.e. for the item currently being processed; see
     * {@link #setCount(Integer, Integer)}. It belongs to a single evaluation, so it must not be kept in any object
     * shared by the whole activity run - all worker threads of an activity evaluate the very same rule, each with its
     * own count.
     */
    private Integer localCount;

    /** Total count for this evaluation - the value the threshold is checked against. @see #setCount(Integer, Integer) */
    private Integer totalCount;

    public BaseEvaluatedPolicyRuleImpl(
            @NotNull AbstractPolicyRuleConfigItem<?> policyRuleCI,
            @NotNull PolicyRuleIdentifier ruleIdentifier) {
        this.policyRuleCI = policyRuleCI;
        this.ruleIdentifier = ruleIdentifier;
    }

    @Override
    public @NotNull AbstractPolicyRuleConfigItem<?> getPolicyRuleConfigItem() {
        return policyRuleCI;
    }

    @Override
    public @NotNull PolicyRuleIdentifier getRuleIdentifier() {
        return ruleIdentifier;
    }

    @Override
    public Collection<? extends PolicyActionConfigItem<?>> getEnabledActions() {
        return List.of();
    }

    @Override
    public Integer getCount() {
        return totalCount;
    }

    /**
     * Local value is the count computed for the current activity, total value is that one plus the counts contributed
     * by the other activities of the tree; see {@code PolicyRuleCounterUpdater}. The threshold is checked against the
     * total one.
     */
    @Override
    public void setCount(Integer localValue, Integer totalValue) {
        this.localCount = localValue;
        this.totalCount = totalValue;
    }

    /** The part of {@link #getCount()} that was computed for the current activity only. */
    public Integer getLocalCount() {
        return localCount;
    }

    @Override
    public String debugDump(int indent) {
        return "";
    }

    public boolean isTriggered() {
        return !triggers.isEmpty();
    }

    @Override
    public @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<EvaluatedPolicyRuleTrigger<?>> triggers) {
        this.triggers.clear();

        if (triggers != null) {
            this.triggers.addAll(triggers);
        }
    }

    public void trigger(Collection<EvaluatedPolicyRuleTrigger<?>> triggers) {
        String ruleName = getName();
        LOGGER.debug("Policy rule {} triggered: {}", ruleName, triggers);
        LOGGER.trace("Policy rule {} triggered:\n{}", ruleName, DebugUtil.debugDumpLazily(triggers, 1));
        this.triggers.addAll(triggers);
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

    @Override
    public @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> getAllTriggers() {
        List<EvaluatedPolicyRuleTrigger<?>> rv = new ArrayList<>();
        for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
            if (trigger instanceof EvaluatedSituationTrigger situationTrigger) {
                rv.addAll(situationTrigger.getAllTriggers());
            } else {
                rv.add(trigger);
            }
        }
        return rv;
    }

    @Override
    public List<TreeNode<LocalizableMessage>> extractMessages() {
        return TriggerPresentationUtil.extractMessages(triggers, MessageKind.NORMAL);
    }

    @Override
    public List<TreeNode<LocalizableMessage>> extractShortMessages() {
        return TriggerPresentationUtil.extractMessages(triggers, MessageKind.SHORT);
    }

    // TODO rewrite this method
    @Override
    public @Nullable String getPolicySituation() {
        // TODO default situations depending on getTriggeredConstraintKinds
        String explicitSituation = getPolicyRuleBean().getPolicySituation();
        if (explicitSituation != null) {
            return explicitSituation;
        }

        if (isTriggered()) {
            EvaluatedPolicyRuleTrigger<?> firstTrigger = getTriggers().iterator().next();
            if (firstTrigger instanceof EvaluatedSituationTrigger situationTrigger) {
                Collection<EvaluatedPolicyRule> sourceRules = situationTrigger.getSourceRules();
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
    public boolean equals(Object o) {
        if (!(o instanceof BaseEvaluatedPolicyRuleImpl that)) {
            return false;
        }
        return Objects.equals(policyRuleCI, that.policyRuleCI)
                && Objects.equals(ruleIdentifier, that.ruleIdentifier)
                && Objects.equals(triggers, that.triggers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyRuleCI, ruleIdentifier, triggers);
    }
}
