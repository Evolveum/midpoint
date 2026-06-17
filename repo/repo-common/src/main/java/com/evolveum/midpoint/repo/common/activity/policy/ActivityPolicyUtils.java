/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ActivityPolicyUtils {

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
        String fallBackMessage = localPart;

        return new SingleLocalizableMessage(key, new Object[0], fallBackMessage);
    }

    public static String createIdentifier(ActivityPath path, PolicyRuleType policy) {
        return path.toString() + ":" + policy.getId();
    }

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

    public static String buildPolicyIdentifier(PrismObject<TaskType> task, ActivityPath path, String policyIdentifier) {
        return buildPolicyIdentifier(task, path, policyIdentifier, false);
    }

    public static String buildPolicyIdentifier(PrismObject<TaskType> task, ActivityPath path, String policyIdentifier, boolean exact) {
        TaskType taskType = task.asObjectable();

        ActivityDefinitionType def = ActivityDefinitionUtil.findActivityDefinition(taskType.getActivity(), path);
        if (def == null) {
            throw new IllegalStateException("No activity definition for path " + path + " in task " + taskType);
        }

        ActivityPoliciesType policies = def.getPolicies();
        if (policies == null) {
            throw new IllegalStateException("No activity policies for path " + path + " in task " + taskType);
        }

        PolicyRuleType policy = policies.getPolicy().stream()
                .filter(p -> exact ?
                        Objects.equals(policyIdentifier, p.getName())
                        : p.getName() != null && p.getName().contains(policyIdentifier))
                .findFirst()
                .orElse(null);
        if (policy == null) {
            throw new IllegalStateException("No activity policy matching '" + policyIdentifier + "' for path " + path + " in task " + taskType);
        }

        return new ActivityPolicyRuleIdentifier(path, policy.getId()).asString();
    }
}
