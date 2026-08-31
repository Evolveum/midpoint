/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.Set;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.policy.evaluator.ActivityCompositeConstraintEvaluator;
import com.evolveum.midpoint.schema.policy.PolicyRuleIdentifier;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.PolicyRuleConfigItem;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyThresholdType;

/**
 * Builder for {@link ActivityPolicyRule}.
 *
 * Main reason for this builder is to hide implementation details regarding policy rule (state)
 * construction, so that it can be used in upper layers that also process policies.
 */
public class ActivityPolicyRuleBuilder {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityPolicyRuleBuilder.class);

    private final PolicyRuleType policyRule;

    private final ActivityPath activityPath;

    private final ConfigurationItemOrigin origin;

    private final PolicyRuleIdentifier policyRuleIdentifier;

    public ActivityPolicyRuleBuilder(
            @NotNull PolicyRuleType policyRule,
            @NotNull ActivityPath activityPath,
            @NotNull PolicyRuleIdentifier policyRuleIdentifier,
            @NotNull ConfigurationItemOrigin origin) {
        this.policyRule = policyRule;
        this.activityPath = activityPath;
        this.policyRuleIdentifier = policyRuleIdentifier;
        this.origin = origin;
    }

    public ActivityPolicyRule build() {
        // The rule bean comes from the live (mutable) activity definition and is later read concurrently
        // by all worker threads of the activity run - by the activity-side rule evaluation as well as by
        // the per-item clockwork processing. We create a private frozen copy here, while still in the
        // single-threaded run initialization: concurrent lazy parsing of a shared mutable prism structure
        // is not thread-safe and was observed to leave parts of the rule (policy threshold water marks,
        // policy actions) unreadable for the rest of the run.
        PolicyRuleType frozenRule = policyRule.clone();
        frozenRule.asPrismContainerValue().freeze();

        var policyCI = ConfigurationItem.configItem(frozenRule, origin, PolicyRuleConfigItem.class);

        var rule = new ActivityPolicyRule(policyCI, activityPath, policyRuleIdentifier, getDataNeeds(frozenRule));

        // The "what will this run actually enforce" snapshot: any content lost on the way from the
        // definition (thresholds, actions) is visible right here, at the start of the run.
        if (LOGGER.isTraceEnabled()) {
            PolicyThresholdType threshold = frozenRule.getPolicyThreshold();
            LOGGER.trace("Built activity policy rule '{}' ({}) at '{}': lowWaterMark={}, highWaterMark={}, actions={}",
                    frozenRule.getName(), rule.getRuleIdentifier(), activityPath,
                    threshold != null && threshold.getLowWaterMark() != null ? threshold.getLowWaterMark().getCount() : null,
                    threshold != null && threshold.getHighWaterMark() != null ? threshold.getHighWaterMark().getCount() : null,
                    policyCI.getAllActions().stream().map(a -> a.getTypeName()).toList());
        }

        return rule;
    }

    private static Set<DataNeed> getDataNeeds(PolicyRuleType policyBean) {
        return ActivityCompositeConstraintEvaluator.get().getDataNeeds(createRootConstraintElement(policyBean));
    }

    private static JAXBElement<PolicyConstraintsType> createRootConstraintElement(PolicyRuleType policyBean) {
        return new JAXBElement<>(
                PolicyConstraintsType.F_AND,
                PolicyConstraintsType.class,
                policyBean.getPolicyConstraints());
    }
}
