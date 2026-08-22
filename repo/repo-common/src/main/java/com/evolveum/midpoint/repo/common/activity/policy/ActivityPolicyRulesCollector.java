/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerUtils;
import com.evolveum.midpoint.schema.util.task.ActivityPolicyRuleIdentifier;
import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.util.exception.*;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.policy.PlainPolicyRuleIdentifier;
import com.evolveum.midpoint.schema.policy.PolicyRuleIdentifier;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.Task;
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

        // We know that embedded rules are defined in the activity definition, which resides in the root task.
        // In the future, we may consired putting the origin directly into ActivityDefinition, just as it is
        // now in the WorkDefinition.
        ConfigurationItemOrigin originForEmbeddedRules =
                ConfigurationItemOrigin.inObjectApproximate(
                        activityRun.getRunningTask().getRootTask().getRawTaskObjectClonedIfNecessary().asObjectable(),
                        TaskType.F_ACTIVITY);

        List<ActivityPolicyRule> rules = collectRules(
                activityRun.getActivity(), originForEmbeddedRules, objectResolver, activityRun.getRunningTask(), result);
        getPolicyRulesContext().setPolicyRules(rules);

        LOGGER.trace("Found {} activity policy rules for activity hierarchy, activity: '{}', rules: {}",
                rules.size(),
                activityRun.getActivityPath(),
                DebugUtil.lazy(() -> rules.stream()
                        .map(ActivityPolicyRule::getName)
                        .collect(Collectors.joining(", "))));

        PreexistingValues preexistingValues = PreexistingValues.determine(activityRun, rules, result);
        getPolicyRulesContext().setPreexistingValues(preexistingValues);

        LOGGER.trace("Determined preexisting values for activity policy rules:\n{}", preexistingValues.debugDumpLazily(1));
    }

    /**
     * Collects all policy rules from the given activity and its parent activities recursively.
     *
     * Rules from parent activities are included because otherwise they would only be evaluated/enforced
     * in-between child activities, which might be too infrequent (e.g., for execution time policies).
     * By collecting rules from the entire activity hierarchy, we ensure that parent rules are
     * enforced as often as necessary.
     *
     * Note that each rule is returned exactly once, under the path of the activity that declares it. Embedded child
     * activities (e.g. of reconciliation) must not inherit the policies of their parent into their own definition,
     * otherwise the parent rules would be returned twice; see {@link ActivityHandlerUtils#cloneWithoutIdForChildActivity}.
     *
     * Only the activity and the task are needed here; the activity run is not, which is what makes this callable
     * (and testable) without running the task.
     *
     * @param activity The activity from which to start collecting policy rules (null to stop).
     * @param originForEmbeddedRules The origin to use for rules embedded in the activity definition (not referenced from a role).
     *                               We assume they are defined in the root task object.
     * @return List of evaluated activity policy rules, ordered by their defined order. They all are enabled.
     */
    public static List<ActivityPolicyRule> collectRules(
            @Nullable Activity<?, ?> activity,
            @NotNull ConfigurationItemOrigin originForEmbeddedRules,
            @NotNull ObjectResolver objectResolver,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ConfigurationException, SchemaException, ObjectNotFoundException {

        if (activity == null) {
            return List.of();
        }

        var rules = new ArrayList<>(collectRules(activity.getParent(), originForEmbeddedRules, objectResolver, task, result));

        ActivityPath activityPath = activity.getPath();
        ActivityPoliciesType activityPoliciesBean = activity.getDefinition().getPoliciesDefinition().getPolicies();

        List<ActivityPolicyRule> activityRules = new ArrayList<>();

        if (ActivityPolicyUtils.isActivityPolicyProcessingDisabled(activity)) {
            LOGGER.trace("Activity policy processing is disabled for '{}', skipping rules declared there", activityPath);
        } else {
            collectRulesFromActivityPolicies(activityPoliciesBean, activityPath, activityRules, originForEmbeddedRules);

            collectRulesFromActivityPolicyRefs(
                    activityPoliciesBean, activityPath, activityRules, objectResolver, task, result);
        }

        activityRules.sort(
                Comparator.comparing(
                        ActivityPolicyRule::getOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        rules.addAll(activityRules);

        LOGGER.trace("Found {} activity policy rules for activity '{}' (including ancestors)", rules.size(), activityPath);

        return rules;
    }

    private static void collectRulesFromActivityPolicies(
            @NotNull ActivityPoliciesType activityPoliciesBean,
            @NotNull ActivityPath activityPath,
            @NotNull List<ActivityPolicyRule> rules,
            @NotNull ConfigurationItemOrigin originForEmbeddedRules) {

        for (PolicyRuleType rule : activityPoliciesBean.getPolicy()) {
            if (BooleanUtils.isFalse(rule.isEnabled())) {
                continue;
            }

            var ruleId = ActivityPolicyRuleIdentifier.of(rule, activityPath);
            addActivityPolicyRule(rule, activityPath, originForEmbeddedRules, ruleId, rules);
        }
    }

    private static void collectRulesFromActivityPolicyRefs(
            ActivityPoliciesType activityPoliciesBean, ActivityPath activityPath, List<ActivityPolicyRule> rules,
            ObjectResolver objectResolver, Task task, OperationResult result)
            throws ConfigurationException, ObjectNotFoundException, SchemaException {

        for (ObjectReferenceType policyRef : activityPoliciesBean.getPolicyRef()) {
            AbstractRoleType role;
            try {
                role = objectResolver.resolve(
                        policyRef, AbstractRoleType.class, null, "resolving policyRef", task, result);
            } catch (CommunicationException | SecurityViolationException | ExpressionEvaluationException |
                    SubscriptionComplianceException e) {
                throw SystemException.unexpected(e, "while resolving policyRef");
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
                        role,
                        ItemPath.create(
                                AbstractRoleType.F_INDUCEMENT,
                                inducement.getId(),
                                AssignmentType.F_POLICY_RULE,
                                rule.getId()));

                PolicyRuleIdentifier ruleId = PlainPolicyRuleIdentifier.of(role.getOid(), inducement.getId());
                addActivityPolicyRule(rule, activityPath, origin, ruleId, rules);
            }
        }
    }

    private static void addActivityPolicyRule(
            @NotNull PolicyRuleType rule,
            @NotNull ActivityPath activityPath,
            @NotNull ConfigurationItemOrigin origin,
            @NotNull PolicyRuleIdentifier policyRuleIdentifier,
            @NotNull List<ActivityPolicyRule> rules) {

        rules.add(new ActivityPolicyRuleBuilder(rule, activityPath, policyRuleIdentifier, origin)
                .build());
    }
}
