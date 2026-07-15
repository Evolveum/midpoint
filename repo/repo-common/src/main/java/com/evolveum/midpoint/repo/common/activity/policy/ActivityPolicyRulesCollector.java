/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.policy.PlainPolicyRuleIdentifier;
import com.evolveum.midpoint.repo.common.policy.PolicyRuleIdentifier;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ActivityPolicyRulesCollector {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityPolicyRulesCollector.class);

    @NotNull
    private final AbstractActivityRun<?, ?, ?> activityRun;
    @NotNull
    private final ObjectResolver objectResolver;

    public ActivityPolicyRulesCollector(
            @NotNull AbstractActivityRun<?, ?, ?> activityRun,
            @NotNull ObjectResolver objectResolver) {

        this.activityRun = activityRun;
        this.objectResolver = objectResolver;
    }

    private ActivityPolicyRulesContext getPolicyRulesContext() {
        return activityRun.getActivityPolicyRulesContext();
    }

    /**
     * Collects all activity policy rules from the activity and its parent activities.
     * Collects also preexisting (initial) values for individual constraints.
     */
    public void collectRulesAndPreexistingValues(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {

        List<ActivityPolicyRule> rules =
                collectRules(activityRun.getActivity(), activityRun.getRunningTask(), objectResolver, result);
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
     * Note that each rule is returned exactly once, under the path of the activity that declares it. Embedded child
     * activities (e.g. of reconciliation) must not inherit the policies of their parent into their own definition,
     * otherwise the parent rules would be returned twice; see
     * {@link com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerUtils#cloneWithoutIdForChildActivity}.
     *
     * Only the activity and the task are needed here; the activity run is not, which is what makes this callable
     * (and testable) without running the task.
     *
     * @param activity The activity from which to start collecting policy rules (null to stop).
     * @return List of evaluated activity policy rules, ordered by their defined order.
     */
    public static List<ActivityPolicyRule> collectRules(
            @Nullable Activity<?, ?> activity, @NotNull Task task,
            @NotNull ObjectResolver objectResolver, @NotNull OperationResult result) throws ConfigurationException {

        if (activity == null) {
            return List.of();
        }

        var rules = new ArrayList<>(collectRules(activity.getParent(), task, objectResolver, result));

        ActivityPath activityPath = activity.getPath();
        ActivityPoliciesType activityPoliciesBean = activity.getDefinition().getPoliciesDefinition().getPolicies();

        List<ActivityPolicyRule> activityRules = new ArrayList<>();

        collectRulesFromActivityPolicies(activityPoliciesBean, activityPath, activityRules, task);

        collectRulesFromActivityPolicyRefs(
                activityPoliciesBean, activityPath, activityRules, objectResolver, task, result);

        activityRules.sort(
                Comparator.comparing(
                        ActivityPolicyRule::getOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        rules.addAll(activityRules);

        LOGGER.trace("Found {} activity policy rules for activity '{}' (including ancestors)", rules.size(), activityPath);

        return rules;
    }

    private static void collectRulesFromActivityPolicies(
            ActivityPoliciesType activityPoliciesBean, ActivityPath activityPath, List<ActivityPolicyRule> rules,
            Task task) {

        ConfigurationItemOrigin origin = ConfigurationItemOrigin.inObjectApproximate(
                task.getRawTaskObjectClonedIfNecessary().asObjectable(),
                TaskType.F_ACTIVITY);

        for (PolicyRuleType rule : activityPoliciesBean.getPolicy()) {
            if (BooleanUtils.isFalse(rule.isEnabled())) {
                continue;
            }

            addActivityPolicyRule(rule, activityPath, origin, null, rules);
        }
    }

    private static void collectRulesFromActivityPolicyRefs(
            ActivityPoliciesType activityPoliciesBean, ActivityPath activityPath, List<ActivityPolicyRule> rules,
            ObjectResolver objectResolver, Task task, OperationResult result) throws ConfigurationException {

        for (ObjectReferenceType policyRef : activityPoliciesBean.getPolicyRef()) {
            AbstractRoleType role = null;
            try {
                role = objectResolver.resolve(policyRef, AbstractRoleType.class, null, "resolving policyRef", task, result);
            } catch (ObjectNotFoundException ex) {
                LOGGER.warn(
                        "Referenced object for policyRef {} not found, skipping. Activity path: {}",
                        policyRef, activityPath);
            } catch (CommonException ex) {
                LOGGER.warn(
                        "Error resolving object for policyRef {}, skipping. Activity path: {}. Error: {}",
                        policyRef, activityPath, ex.getMessage());
            }

            if (role == null) {
                continue;
            }

            for (AssignmentType inducement : role.getInducement()) {
                PolicyRuleType rule = inducement.getPolicyRule();
                if (rule == null || BooleanUtils.isFalse(rule.isEnabled())) {
                    continue;
                }

                if (inducement.getOrder() != null) {
                    throw new ConfigurationException(
                            "Inducement-based policy rules do not support order, but rule %s in role %s has order defined"
                                    .formatted(rule.getName(), role.getName())
                    );
                }

                if (inducement.getCondition() != null) {
                    throw new ConfigurationException(
                            "Inducement-based policy rules do not support condition, but rule %s in role %s has condition defined"
                                    .formatted(rule.getName(), role.getName())
                    );
                }

                ConfigurationItemOrigin origin = ConfigurationItemOrigin.inObject(
                        role, ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_POLICY_RULE, rule.getId()));

                PolicyRuleIdentifier identifier = PlainPolicyRuleIdentifier.of(role.getOid(), inducement.getId());

                addActivityPolicyRule(rule, activityPath, origin, identifier, rules);
            }
        }
    }

    private static void addActivityPolicyRule(
            PolicyRuleType rule, ActivityPath activityPath, ConfigurationItemOrigin origin,
            PolicyRuleIdentifier customPolicyRuleIdentifier, List<ActivityPolicyRule> rules) {

        rules.add(new ActivityPolicyRuleBuilder(rule, activityPath, origin)
                .customPolicyRuleIdentifier(customPolicyRuleIdentifier)
                .build());
    }
}
