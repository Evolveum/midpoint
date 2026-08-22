/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;

import com.evolveum.midpoint.schema.util.task.ActivityPolicyRuleIdentifier;

import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Utility methods for working with activity policies.
 */
public class ActivityPolicyUtils {

    /**
     * Returns true if processing of activity policies (inline "policy" + "policyRef") declared at this activity
     * is suppressed — either by the activity itself or by any of its ancestors (suppression is inherited downward).
     */
    public static boolean isActivityPolicyProcessingDisabled(@Nullable Activity<?, ?> activity) {
        return isProcessingDisabled(activity, ActivityPoliciesProcessingType::getActivityPolicies);
    }

    /**
     * Returns true if application of virtual assignments declared at this activity is suppressed — either by
     * the activity itself or by any of its ancestors (suppression is inherited downward).
     */
    public static boolean isVirtualAssignmentPolicyProcessingDisabled(@Nullable Activity<?, ?> activity) {
        return isProcessingDisabled(activity, ActivityPoliciesProcessingType::getVirtualAssignmentPolicies);
    }

    private static boolean isProcessingDisabled(
            @Nullable Activity<?, ?> activity,
            @NotNull Function<ActivityPoliciesProcessingType, PolicyProcessingModeType> scope) {
        for (Activity<?, ?> a = activity; a != null; a = a.getParent()) {
            ActivityPoliciesProcessingType processing =
                    a.getDefinition().getPoliciesDefinition().getPolicies().getProcessing();
            if (processing != null && scope.apply(processing) == PolicyProcessingModeType.NONE) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static <C extends AbstractPolicyConstraintType> String getDefaultConstraintName(@NotNull JAXBElement<C> constraintJaxb) {
        if (constraintJaxb == null || constraintJaxb.getValue() == null) {
            throw new IllegalArgumentException("Null constraint");
        }

        C constraint = constraintJaxb.getValue();
        String localPart = constraintJaxb.getName().getLocalPart();

        return StringUtils.isNotEmpty(constraint.getName()) ? constraint.getName() : localPart;
    }

    @NotNull
    public static <PC extends AbstractPolicyConstraintType> LocalizableMessage getConstraintName(@NotNull JAXBElement<PC> constraintJaxb) {
        if (constraintJaxb == null || constraintJaxb.getValue() == null) {
            throw new IllegalArgumentException("Null constraint");
        }

        PC constraint = constraintJaxb.getValue();
        String localPart = constraintJaxb.getName().getLocalPart();

        String key = StringUtils.isNotEmpty(constraint.getName()) ? constraint.getName() : "Constraint." + localPart + ".defaultName";
        @SuppressWarnings("UnnecessaryLocalVariable") String fallBackMessage = localPart;

        return new SingleLocalizableMessage(key, new Object[0], fallBackMessage);
    }

    /**
     * Returns all policy identifiers for _directly attached_ policy rules in the given activity definition.
     *
     * @param definition Definition to inspect
     * @param path The path at which is this definition (we may or may not start at root)
     */
    public static Collection<ActivityPolicyRuleIdentifier> listPolicyRuleIdentifiers(
            ActivityDefinitionType definition, ActivityPath path) {

        Collection<ActivityPolicyRuleIdentifier> identifiers = new HashSet<>();

        ActivityDefinitionUtil.visitActivityDefinitions(definition, path, (def, activityPath) -> {
            ActivityPoliciesType policies = def.getPolicies();
            if (policies == null) {
                return true;
            }

            policies.getPolicy()
                    .forEach(policy -> identifiers.add(ActivityPolicyRuleIdentifier.of(policy, activityPath)));

            return true;
        });

        return identifiers;
    }
}
