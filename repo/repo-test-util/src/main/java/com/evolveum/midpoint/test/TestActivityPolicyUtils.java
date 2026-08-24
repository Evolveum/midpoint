/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.ActivityPolicyRuleIdentifier;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPoliciesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import java.util.Objects;

public class TestActivityPolicyUtils {

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

        return ActivityPolicyRuleIdentifier.of(policy, path).asString();
    }
}
