/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.Collection;
import java.util.HashSet;

import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.work.ActivityDefinitionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPoliciesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyType;

public class ActivityPolicyUtils {

    public static String createIdentifier(ActivityPath path, ActivityPolicyType policy) {
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
}
