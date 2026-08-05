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
import com.evolveum.midpoint.repo.common.policy.PolicyRuleIdentifier;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.PolicyRuleConfigItem;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

/**
 * Builder for {@link ActivityPolicyRule}.
 *
 * Main reason for this builder is to hide implementation details regarding policy rule (state)
 * construction, so that it can be used in upper layers that also process policies.
 */
public class ActivityPolicyRuleBuilder {

    private final PolicyRuleType policyRule;

    private final ActivityPath activityPath;

    private final ConfigurationItemOrigin origin;

    private PolicyRuleIdentifier customPolicyRuleIdentifier;

    public ActivityPolicyRuleBuilder(@NotNull PolicyRuleType policyRule, ActivityPath activityPath, ConfigurationItemOrigin origin) {
        this.policyRule = policyRule;
        this.activityPath = activityPath;
        this.origin = origin;
    }

    public ActivityPolicyRuleBuilder customPolicyRuleIdentifier(PolicyRuleIdentifier customPolicyRuleIdentifier) {
        this.customPolicyRuleIdentifier = customPolicyRuleIdentifier;
        return this;
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

        return new ActivityPolicyRule(policyCI, activityPath, customPolicyRuleIdentifier, getDataNeeds(frozenRule));
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
