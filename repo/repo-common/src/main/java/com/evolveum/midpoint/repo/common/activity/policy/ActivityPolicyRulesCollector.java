/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.policy.evaluator.ActivityCompositeConstraintEvaluator;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPoliciesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

public class ActivityPolicyRulesCollector {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityPolicyRulesCollector.class);

    @NotNull
    private final AbstractActivityRun<?, ?, ?> activityRun;

    public ActivityPolicyRulesCollector(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        this.activityRun = activityRun;
    }

    private ActivityPolicyRulesContext getPolicyRulesContext() {
        return activityRun.getActivityPolicyRulesContext();
    }

    /**
     * Collects all activity policy rules from the activity and its parent activities.
     * Collects also preexisting (initial) values for individual constraints.
     *
     * TODO Currently returns "doubled" policies when activity has embedded child activities (e.g. reconciliation).
     *  Reason is that embedded activities do inherit definition from parent activity (if there's no tailoring in place).
     */
    public void collectRulesAndPreexistingValues(OperationResult result) throws SchemaException, ObjectNotFoundException {
        List<ActivityPolicyRule> rules = collectRulesFromActivity(activityRun.getActivity());
        getPolicyRulesContext().setPolicyRules(rules);

        LOGGER.trace("Found {} activity policy rules for activity hierarchy, activity: '{}', rules: {}",
                rules.size(), activityRun.getActivityPath(), StringUtils.join(rules.stream().map(ActivityPolicyRule::getName).toArray(), ","));

        PreexistingValues preexistingValues = PreexistingValues.determine(activityRun, rules, result);
        getPolicyRulesContext().setPreexistingValues(preexistingValues);

        LOGGER.trace("Determined preexisting values for activity policy rules:\n{}", preexistingValues.debugDumpLazily(1));
    }

    /**
     * Collects all policy rules from the given activity and its parent activities recursively.
     *
     * Rules from parent activities are included because otherwise they would only be validated
     * in-between child activities, which might be too infrequent (e.g., for execution time policies).
     * By collecting rules from the entire activity hierarchy, we ensure that parent rules are
     * enforced as often as necessary.
     *
     * @param activity The activity from which to start collecting policy rules (null to stop).
     * @return List of evaluated activity policy rules, ordered by their defined order.
     */
    private List<ActivityPolicyRule> collectRulesFromActivity(@Nullable Activity<?, ?> activity) {
        if (activity == null) {
            return List.of();
        }

        var rules = new ArrayList<>(collectRulesFromActivity(activity.getParent()));

        ActivityPath activityPath = activity.getPath();
        ActivityPoliciesType activityPoliciesBean = activity.getDefinition().getPoliciesDefinition().getPolicies();
        List<PolicyRuleType> policyBeans = activityPoliciesBean.getPolicy();

        policyBeans.stream()
                .filter(policy -> BooleanUtils.isNotFalse(policy.isEnabled()))
                .map(policy -> new ActivityPolicyRule(policy, activityPath, getDataNeeds(policy)))
                .sorted(
                        Comparator.comparing(
                                ActivityPolicyRule::getOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(r -> rules.add(r));

        LOGGER.trace("Found {} activity policy rules for activity '{}' (including ancestors)", rules.size(), activityPath);

        return rules;
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
